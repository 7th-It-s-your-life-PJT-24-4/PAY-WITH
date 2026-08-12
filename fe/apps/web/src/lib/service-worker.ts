import { registerSW } from 'virtual:pwa-register'

let registrationPromise: Promise<ServiceWorkerRegistration | null> | null = null

export function ensureServiceWorkerRegistration(): Promise<ServiceWorkerRegistration | null> {
  if (registrationPromise) return registrationPromise

  registrationPromise = (async () => {
    if (
      !import.meta.env.PROD ||
      typeof navigator === 'undefined' ||
      !('serviceWorker' in navigator)
    ) {
      return null
    }

    registerSW({ immediate: true })
    return navigator.serviceWorker.ready
  })()

  return registrationPromise
}
