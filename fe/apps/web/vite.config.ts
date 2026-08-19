import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig(({ mode }) => {
  const appRoot = fileURLToPath(new URL('.', import.meta.url))
  const env = loadEnv(mode, appRoot, '')

  return {
    plugins: [
      vue(),
      VitePWA({
        strategies: 'injectManifest',
        srcDir: 'src',
        filename: 'sw.ts',
        registerType: 'autoUpdate',
        injectRegister: false,
        includeAssets: ['pwa-icon.svg'],
        manifest: {
          name: 'PayWith',
          short_name: 'PayWith',
          description: '보호자와 시니어를 위한 안전한 금융 서비스',
          theme_color: '#00b1d2',
          background_color: '#f4f9fa',
          display: 'standalone',
          start_url: '/',
          lang: 'ko-KR',
          icons: [
            {
              src: 'pwa-icon.svg',
              sizes: 'any',
              type: 'image/svg+xml',
              purpose: 'any maskable',
            },
          ],
        },
        injectManifest: {
          globPatterns: ['**/*.{js,css,html,svg,png,webp}'],
          maximumFileSizeToCacheInBytes: 3_000_000,
          rollupFormat: 'iife',
        },
        devOptions: {
          enabled: env.VITE_ENABLE_PWA_DEV === 'true',
          type: 'module',
        },
      }),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    test: {
      environment: 'jsdom',
      env: {
        VITE_API_BASE_URL: 'http://127.0.0.1:5173/api',
      },
      globals: true,
      include: ['tests/unit/**/*.test.ts'],
    },
  }
})
