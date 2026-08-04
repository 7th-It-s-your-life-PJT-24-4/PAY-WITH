import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
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
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
})

configureSessionExpiredHandler(async () => {
  queryClient.clear()
  const currentPath = router.currentRoute.value.fullPath
  const redirect = currentPath.startsWith('/auth') ? undefined : currentPath

  await router.replace({
    name: 'auth-sign-in',
    query: {
      reason: 'session-expired',
      ...(redirect ? { redirect } : {}),
    },
  })
})
startAccessTokenRefreshScheduler()

app.use(createPinia())
app.use(router)
app.use(VueQueryPlugin, { queryClient })

app.mount('#app')
