import { expect, test as base, type Page } from '@playwright/test'

const testAccessToken = `test.${btoa(
  JSON.stringify({
    sub: '1',
    exp: Math.floor(Date.now() / 1_000) + 60 * 60,
  }),
)}.signature`

const test = base.extend<{ page: Page }>({
  page: async ({ page }, use) => {
    await page.addInitScript((accessToken) => {
      localStorage.setItem('accessToken', accessToken)
    }, testAccessToken)

    await use(page)
  },
})

export { expect, test, type Page }
