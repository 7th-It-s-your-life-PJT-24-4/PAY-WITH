import { onMessage } from 'firebase/messaging'
import { computed, ref, shallowRef } from 'vue'
import type { Router } from 'vue-router'

import { registerFcmToken } from '@/api/fcm-token'
import { getUserIdFromAccessToken, tokenStorage } from '@/api/token-storage'
import {
  getCurrentFcmToken,
  getFirebaseMessaging,
  hasFirebaseConfig,
} from '@/lib/firebase'
import {
  resolvePushNotificationDestination,
  type PushNotificationDestination,
} from '@/lib/push-notification'
import { pushTokenStorage } from '@/lib/push-token-storage'
import {
  isPushTokenSessionSynced,
  markPushTokenSessionSynced,
} from '@/lib/push-token-sync-state'
import { ensureServiceWorkerRegistration } from '@/lib/service-worker'

type PushAvailability = 'checking' | 'available' | 'unsupported' | 'disabled'

export type ForegroundPushNotification = PushNotificationDestination & {
  body: string
  title: string
}

const availability = ref<PushAvailability>('checking')
const permission = ref<NotificationPermission>('default')
const isSyncing = ref(false)
const errorMessage = ref('')
const foregroundNotification = shallowRef<ForegroundPushNotification | null>(
  null,
)

let started = false

function updatePermission(): void {
  if (typeof Notification !== 'undefined')
    permission.value = Notification.permission
}

function publishNotification(
  data: unknown,
  title?: string,
  body?: string,
): void {
  const destination = resolvePushNotificationDestination(data)
  if (!destination) return

  foregroundNotification.value = {
    ...destination,
    title: title || 'PayWith 알림',
    body: body || '새로운 알림을 확인해 주세요.',
  }
}

export async function syncPushNotificationToken(): Promise<void> {
  updatePermission()
  const accessToken = tokenStorage.getAccessToken()
  const userId = accessToken ? getUserIdFromAccessToken(accessToken) : null
  if (
    availability.value !== 'available' ||
    permission.value !== 'granted' ||
    !userId ||
    isSyncing.value
  ) {
    return
  }

  isSyncing.value = true
  errorMessage.value = ''
  try {
    const registration = await ensureServiceWorkerRegistration()
    if (!registration) return
    const fcmToken = await getCurrentFcmToken(registration)
    if (!fcmToken) return

    const sessionKey = `${userId}:${fcmToken}`
    if (isPushTokenSessionSynced(sessionKey)) return

    await registerFcmToken(fcmToken)
    pushTokenStorage.set({ token: fcmToken, userId })
    markPushTokenSessionSynced(sessionKey)
  } catch (error) {
    console.warn('FCM 토큰을 등록하지 못했습니다.', error)
    errorMessage.value = '알림을 연결하지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSyncing.value = false
  }
}

export async function startPushNotifications(router: Router): Promise<void> {
  if (started) return
  started = true
  updatePermission()

  try {
    await ensureServiceWorkerRegistration()
  } catch (error) {
    console.warn('서비스 워커를 등록하지 못했습니다.', error)
    availability.value = hasFirebaseConfig() ? 'unsupported' : 'disabled'
    return
  }

  if (!hasFirebaseConfig()) {
    availability.value = 'disabled'
    return
  }

  let messaging
  try {
    messaging = await getFirebaseMessaging()
  } catch (error) {
    console.warn('Firebase Messaging을 초기화하지 못했습니다.', error)
    availability.value = 'unsupported'
    return
  }
  if (!messaging || typeof Notification === 'undefined') {
    availability.value = 'unsupported'
    return
  }

  availability.value = 'available'
  onMessage(messaging, (payload) => {
    publishNotification(
      payload.data,
      payload.notification?.title,
      payload.notification?.body,
    )
  })

  navigator.serviceWorker.addEventListener('message', (event) => {
    if (event.data?.type !== 'PAY_WITH_NOTIFICATION_CLICK') return
    const destination = resolvePushNotificationDestination(event.data.data)
    if (destination) void router.push(destination.path)
  })

  router.afterEach(() => {
    void syncPushNotificationToken()
  })
  window.addEventListener('focus', () => {
    void syncPushNotificationToken()
  })
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') void syncPushNotificationToken()
  })
  await syncPushNotificationToken()
}

export function usePushNotification() {
  const shouldShowPermissionCard = computed(
    () =>
      availability.value !== 'disabled' &&
      (availability.value === 'unsupported' ||
        permission.value !== 'granted' ||
        Boolean(errorMessage.value)),
  )

  async function requestPermission(): Promise<void> {
    if (availability.value !== 'available' || permission.value === 'denied') {
      return
    }

    try {
      permission.value = await Notification.requestPermission()
      if (permission.value === 'granted') await syncPushNotificationToken()
    } catch (error) {
      console.warn('알림 권한을 요청하지 못했습니다.', error)
      errorMessage.value = '알림 권한을 확인하지 못했어요.'
    }
  }

  function dismissForegroundNotification(): void {
    foregroundNotification.value = null
  }

  return {
    availability,
    dismissForegroundNotification,
    errorMessage,
    foregroundNotification,
    isSyncing,
    permission,
    requestPermission,
    shouldShowPermissionCard,
  }
}
