/// <reference lib="webworker" />

import { initializeApp, type FirebaseOptions } from 'firebase/app'
import {
  getMessaging,
  isSupported,
  onBackgroundMessage,
} from 'firebase/messaging/sw'
import { clientsClaim } from 'workbox-core'
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching'

import { resolvePushNotificationDestination } from '@/lib/push-notification'

declare const self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: Array<{
    revision?: string | null
    url: string
  }>
}

const firebaseConfig: FirebaseOptions = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

const hasFirebaseConfig = [
  firebaseConfig.apiKey,
  firebaseConfig.authDomain,
  firebaseConfig.projectId,
  firebaseConfig.storageBucket,
  firebaseConfig.messagingSenderId,
  firebaseConfig.appId,
].every((value) => typeof value === 'string' && value.length > 0)

self.skipWaiting()
clientsClaim()
cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)

function extractPushData(value: unknown): unknown {
  if (!value || typeof value !== 'object') return null
  const record = value as Record<string, unknown>
  if ('type' in record) return record

  const fcmMessage = record.FCM_MSG
  if (!fcmMessage || typeof fcmMessage !== 'object') return null
  return (fcmMessage as Record<string, unknown>).data
}

self.addEventListener('notificationclick', (event) => {
  const destination = resolvePushNotificationDestination(
    extractPushData(event.notification.data),
  )
  if (!destination) return

  event.stopImmediatePropagation()
  event.notification.close()
  event.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({
        includeUncontrolled: true,
        type: 'window',
      })
      const existingWindow = windows[0]
      if (existingWindow) {
        await existingWindow.focus()
        existingWindow.postMessage({
          type: 'PAY_WITH_NOTIFICATION_CLICK',
          data: destination.data,
        })
        return
      }
      await self.clients.openWindow(destination.path)
    })(),
  )
})

if (hasFirebaseConfig) {
  void isSupported()
    .then((supported) => {
      if (!supported) return

      const messaging = getMessaging(initializeApp(firebaseConfig))
      onBackgroundMessage(messaging, async (payload) => {
        // PR #204는 notification payload를 함께 보내므로 Firebase가 OS 알림을 자동 표시한다.
        // data-only 메시지로 바뀐 경우에만 안전한 기본 알림을 한 번 표시한다.
        if (payload.notification) return

        const destination = resolvePushNotificationDestination(payload.data)
        if (!destination) return
        await self.registration.showNotification('PayWith 알림', {
          body: '앱에서 새로운 알림을 확인해 주세요.',
          data: destination.data,
          icon: '/pwa-icon.svg',
        })
      })
    })
    .catch((error: unknown) => {
      console.warn(
        '서비스 워커에서 Firebase Messaging을 초기화하지 못했습니다.',
        error,
      )
    })
}
