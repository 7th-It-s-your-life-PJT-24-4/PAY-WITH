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
  // 개발 서버는 커스텀 PWA 서비스 워커를 등록하지 않으므로 Firebase의 기본 SW 탐색도 막는다.
  if (!import.meta.env.PROD) return

  const messaging = await getFirebaseMessaging()
  if (messaging) await deleteToken(messaging)
}
