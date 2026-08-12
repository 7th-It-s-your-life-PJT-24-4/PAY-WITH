import {
  getApp,
  getApps,
  initializeApp,
  type FirebaseOptions,
} from 'firebase/app'
import {
  deleteToken,
  getMessaging,
  getToken,
  isSupported,
  type Messaging,
} from 'firebase/messaging'

import { ensureServiceWorkerRegistration } from '@/lib/service-worker'

const firebaseConfig: FirebaseOptions = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

const requiredFirebaseConfig = [
  firebaseConfig.apiKey,
  firebaseConfig.authDomain,
  firebaseConfig.projectId,
  firebaseConfig.messagingSenderId,
  firebaseConfig.appId,
  import.meta.env.VITE_FIREBASE_VAPID_KEY,
]

let messagingPromise: Promise<Messaging | null> | null = null

export function hasFirebaseConfig(): boolean {
  return requiredFirebaseConfig.every(
    (value) => typeof value === 'string' && value.length > 0,
  )
}

export function getFirebaseMessaging(): Promise<Messaging | null> {
  if (messagingPromise) return messagingPromise

  messagingPromise = (async () => {
    if (
      !hasFirebaseConfig() ||
      typeof window === 'undefined' ||
      !('serviceWorker' in navigator) ||
      !(await isSupported())
    ) {
      return null
    }

    const app = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig)
    return getMessaging(app)
  })().catch((error: unknown) => {
    messagingPromise = null
    throw error
  })

  return messagingPromise
}

export async function getCurrentFcmToken(
  serviceWorkerRegistration: ServiceWorkerRegistration,
): Promise<string | null> {
  const messaging = await getFirebaseMessaging()
  const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY
  if (!messaging || !vapidKey) return null

  const token = await getToken(messaging, {
    serviceWorkerRegistration,
    vapidKey,
  })
  return token || null
}

export async function deleteCurrentFcmToken(): Promise<void> {
  const serviceWorkerRegistration = await ensureServiceWorkerRegistration()
  if (!serviceWorkerRegistration) return

  const messaging = await getFirebaseMessaging()
  const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY
  if (!messaging || !vapidKey) return

  // deleteToken()이 Firebase 기본 SW를 찾지 않도록 현재 앱의 /sw.js 등록을 먼저 연결한다.
  await getToken(messaging, { serviceWorkerRegistration, vapidKey })
  await deleteToken(messaging)
}
