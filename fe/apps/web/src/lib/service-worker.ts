import { registerSW } from 'virtual:pwa-register'

let registrationPromise: Promise<ServiceWorkerRegistration | null> | null = null

async function resolveRegistration(
  registration: ServiceWorkerRegistration | undefined,
): Promise<ServiceWorkerRegistration> {
  if (registration) return registration

  const existingRegistration = await navigator.serviceWorker.getRegistration()
  if (existingRegistration) return existingRegistration
  throw new Error('등록된 서비스 워커를 확인할 수 없습니다.')
}

export function ensureServiceWorkerRegistration(): Promise<ServiceWorkerRegistration | null> {
  if (registrationPromise) return registrationPromise

  if (
    !import.meta.env.PROD ||
    typeof navigator === 'undefined' ||
    !('serviceWorker' in navigator)
  ) {
    return Promise.resolve(null)
  }

  registrationPromise = new Promise<ServiceWorkerRegistration>(
    (resolve, reject) => {
      registerSW({
        immediate: true,
        onRegisteredSW: (_swUrl, registration) => {
          void resolveRegistration(registration).then(resolve, reject)
        },
        onRegisterError: reject,
      })
    },
  ).catch((error: unknown) => {
    // 일시적인 네트워크·배포 오류가 복구된 뒤 다시 등록할 수 있어야 한다.
    registrationPromise = null
    throw error
  })

  return registrationPromise
}
