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
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

const requiredFirebaseConfig = [
  firebaseConfig.apiKey,
  firebaseConfig.authDomain,
  firebaseConfig.projectId,
  firebaseConfig.storageBucket,
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
  })()

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
  const messaging = await getFirebaseMessaging()
  if (messaging) await deleteToken(messaging)
}
