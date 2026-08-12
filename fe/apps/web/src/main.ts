import { VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from '@/App.vue'
import { configureSessionExpiredHandler } from '@/api/auth-session'
import { clearLocalPushSubscription } from '@/api/push-session'
import { startAccessTokenRefreshScheduler } from '@/api/token-refresh'
import { startPushNotifications } from '@/composables/usePushNotification'
import router from '@/router'
import { initializeSentry } from '@/lib/sentry'
import '@/style.css'

const app = createApp(App)
initializeSentry(app, router)

configureSessionExpiredHandler(async () => {
  const currentPath = router.currentRoute.value.fullPath
  const redirect = currentPath.startsWith('/auth') ? undefined : currentPath
  const signInLocation = router.resolve({
    name: 'auth-sign-in',
    query: {
      reason: 'session-expired',
      ...(redirect ? { redirect } : {}),
    },
  })

  // 공용 기기에서 만료된 사용자의 알림이 계속 노출되지 않도록 로컬 구독부터 폐기한다.
  await clearLocalPushSubscription()

  // 새 로그인 세션에 이전 사용자의 Pinia 메모리가 남지 않도록 앱을 다시 시작한다.
  globalThis.location.replace(signInLocation.href)
})
startAccessTokenRefreshScheduler()

app.use(createPinia())
app.use(router)
app.use(VueQueryPlugin, {
  queryClientConfig: {
    defaultOptions: {
      queries: {
        refetchOnWindowFocus: false,
        retry: 1,
      },
    },
  },
})

app.mount('#app')
void startPushNotifications(router)
