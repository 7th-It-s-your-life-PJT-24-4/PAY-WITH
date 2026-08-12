/// <reference lib="webworker" />

import type { FirebaseOptions } from 'firebase/app'
import { clientsClaim } from 'workbox-core'
import { cleanupOutdatedCaches, precacheAndRoute } from 'workbox-precaching'

import {
  extractPushNotificationData,
  resolvePushNotificationDestination,
} from '@/lib/push-notification'

declare const self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: Array<{
    revision?: string | null
    url: string
  }>
}

const firebaseConfig: FirebaseOptions = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

const hasFirebaseConfig = [
  firebaseConfig.apiKey,
  firebaseConfig.projectId,
  firebaseConfig.messagingSenderId,
  firebaseConfig.appId,
].every((value) => typeof value === 'string' && value.length > 0)

self.skipWaiting()
clientsClaim()
cleanupOutdatedCaches()
precacheAndRoute(self.__WB_MANIFEST)

self.addEventListener('notificationclick', (event) => {
  const destination = resolvePushNotificationDestination(
    extractPushNotificationData(event.notification.data),
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

async function initializeFirebaseMessaging(): Promise<void> {
  // Firebase 공식 권장사항대로 커스텀 notificationclick 리스너를 먼저 등록한 뒤
  // Messaging 모듈을 불러온다. SDK 업데이트로 내부 리스너 등록 시점이 바뀌어도 안전하다.
  const [{ initializeApp }, { getMessaging, isSupported }] = await Promise.all([
    import('firebase/app'),
    import('firebase/messaging/sw'),
  ])
  if (!(await isSupported())) return

  getMessaging(initializeApp(firebaseConfig))
}

if (hasFirebaseConfig) {
  void initializeFirebaseMessaging().catch((error: unknown) => {
    console.warn(
      '서비스 워커에서 Firebase Messaging을 초기화하지 못했습니다.',
      error,
    )
  })
}
