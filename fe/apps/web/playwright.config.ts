import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  reporter: 'html',
  use: {
    baseURL: 'http://127.0.0.1:5173',
    serviceWorkers: 'block',
    trace: 'on-first-retry',
  },
  webServer: {
    command: 'pnpm dev --host 127.0.0.1',
    env: {
      VITE_API_BASE_URL: 'http://127.0.0.1:5173/api',
      VITE_ENABLE_PWA_DEV: 'false',
    },
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: false,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'webkit-mobile',
      testMatch: /ward-home\.spec\.ts/,
      use: { ...devices['iPhone 13'] },
    },
  ],
})
