import { onMessage } from 'firebase/messaging'
import { computed, ref, shallowRef } from 'vue'
import type { Router } from 'vue-router'

import { registerFcmToken } from '@/api/fcm-token'
import { unregisterPushNotifications } from '@/api/push-session'
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
import { pushNotificationPreference } from '@/lib/push-notification-preference'
import { pushTokenStorage } from '@/lib/push-token-storage'
import {
  isPushTokenSessionSynced,
  markPushTokenSessionSynced,
  resetPushTokenSyncState,
} from '@/lib/push-token-sync-state'
import { ensureServiceWorkerRegistration } from '@/lib/service-worker'

type PushAvailability = 'checking' | 'available' | 'unsupported' | 'disabled'

export type ForegroundPushNotification = PushNotificationDestination & {
  body: string
  title: string
}

const availability = ref<PushAvailability>('checking')
const permission = ref<NotificationPermission>('default')
const isEnabled = ref(false)
const isSyncing = ref(false)
const errorMessage = ref('')
const foregroundNotification = shallowRef<ForegroundPushNotification | null>(
  null,
)

let started = false

function getCurrentUserId(): number | null {
  const accessToken = tokenStorage.getAccessToken()
  return accessToken ? getUserIdFromAccessToken(accessToken) : null
}

function refreshEnabledState(userId = getCurrentUserId()): void {
  isEnabled.value = Boolean(
    userId &&
    pushNotificationPreference.isEnabled(userId) &&
    availability.value === 'available' &&
    permission.value === 'granted',
  )
}

function updatePermission(): void {
  if (typeof Notification !== 'undefined')
    permission.value = Notification.permission
  refreshEnabledState()
}

function publishNotification(
  data: unknown,
  title?: string,
  body?: string,
): void {
  const destination = resolvePushNotificationDestination(data)
  if (!destination) return

  const notificationTitle = title || 'PayWith 알림'
  const notificationBody = body || '새로운 알림을 확인해 주세요.'

  foregroundNotification.value = {
    ...destination,
    title: notificationTitle,
    body: notificationBody,
  }

  if (permission.value === 'granted') {
    new Notification(notificationTitle, {
      body: notificationBody,
      icon: '/pwa-192x192.png',
      tag: `pay-with-${destination.data.type}-${destination.data.refId}`,
    })
    console.info('[Push] 브라우저 알림을 표시했습니다.')
  }
}

export async function syncPushNotificationToken(): Promise<void> {
  updatePermission()
  const userId = getCurrentUserId()
  if (
    availability.value !== 'available' ||
    permission.value !== 'granted' ||
    !userId ||
    !pushNotificationPreference.isEnabled(userId) ||
    isSyncing.value
  ) {
    refreshEnabledState(userId)
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
    if (isPushTokenSessionSynced(sessionKey)) {
      refreshEnabledState(userId)
      return
    }

    await registerFcmToken(fcmToken)
    pushTokenStorage.set({ token: fcmToken, userId })
    markPushTokenSessionSynced(sessionKey)
    isEnabled.value = true
  } catch (error) {
    console.warn('FCM 토큰을 등록하지 못했습니다.', error)
    errorMessage.value = '알림을 연결하지 못했어요. 잠시 후 다시 시도해 주세요.'
    refreshEnabledState(userId)
  } finally {
    isSyncing.value = false
  }
}

export async function startPushNotifications(router: Router): Promise<void> {
  if (started) return
  started = true
  updatePermission()

  let registration: ServiceWorkerRegistration | null
  try {
    registration = await ensureServiceWorkerRegistration()
  } catch (error) {
    console.warn('서비스 워커를 등록하지 못했습니다.', error)
    availability.value = hasFirebaseConfig() ? 'unsupported' : 'disabled'
    started = false
    return
  }

  if (!registration) {
    availability.value = 'disabled'
    started = false
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
    started = false
    return
  }
  if (!messaging || typeof Notification === 'undefined') {
    availability.value = 'unsupported'
    return
  }

  availability.value = 'available'
  refreshEnabledState()
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

  async function enablePushNotifications(): Promise<boolean> {
    const userId = getCurrentUserId()
    errorMessage.value = ''

    if (!userId) {
      errorMessage.value = '로그인 정보를 확인하지 못했어요.'
      return false
    }
    if (availability.value !== 'available') {
      errorMessage.value =
        availability.value === 'unsupported'
          ? '이 브라우저에서는 푸시 알림을 사용할 수 없어요.'
          : '푸시 알림을 사용할 수 있는 환경인지 확인해 주세요.'
      return false
    }
    if (permission.value === 'denied') {
      errorMessage.value =
        '브라우저 또는 기기 설정에서 PayWith 알림을 허용해 주세요.'
      return false
    }

    try {
      if (permission.value !== 'granted') {
        permission.value = await Notification.requestPermission()
      }
      if (permission.value !== 'granted') {
        pushNotificationPreference.setEnabled(userId, false)
        refreshEnabledState(userId)
        return false
      }

      pushNotificationPreference.setEnabled(userId, true)
      resetPushTokenSyncState()
      await syncPushNotificationToken()
      return isEnabled.value
    } catch (error) {
      console.warn('알림 권한을 요청하지 못했습니다.', error)
      errorMessage.value = '알림 권한을 확인하지 못했어요.'
      refreshEnabledState(userId)
      return false
    }
  }

  async function disablePushNotifications(): Promise<void> {
    const userId = getCurrentUserId()
    errorMessage.value = ''
    if (userId) pushNotificationPreference.setEnabled(userId, false)
    isEnabled.value = false
    await unregisterPushNotifications()
  }

  async function requestPermission(): Promise<void> {
    await enablePushNotifications()
  }

  function dismissForegroundNotification(): void {
    foregroundNotification.value = null
  }

  return {
    availability,
    dismissForegroundNotification,
    errorMessage,
    foregroundNotification,
    disablePushNotifications,
    enablePushNotifications,
    isEnabled,
    isSyncing,
    permission,
    requestPermission,
    shouldShowPermissionCard,
  }
}
