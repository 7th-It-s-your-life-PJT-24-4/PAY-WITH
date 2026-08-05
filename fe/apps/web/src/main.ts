import { VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import { registerSW } from 'virtual:pwa-register'

import App from '@/App.vue'
import { configureSessionExpiredHandler } from '@/api/auth-session'
import { startAccessTokenRefreshScheduler } from '@/api/token-refresh'
import router from '@/router'
import '@/style.css'

if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  registerSW({ immediate: true })
}

const app = createApp(App)

configureSessionExpiredHandler(() => {
  const currentPath = router.currentRoute.value.fullPath
  const redirect = currentPath.startsWith('/auth') ? undefined : currentPath
  const signInLocation = router.resolve({
    name: 'auth-sign-in',
    query: {
      reason: 'session-expired',
      ...(redirect ? { redirect } : {}),
    },
  })

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
