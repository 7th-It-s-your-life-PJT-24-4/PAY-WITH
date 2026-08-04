import { expect, test as base, type Page } from '@playwright/test'

const testAccessToken = 'test.eyJzdWIiOiIxIn0.signature'

const test = base.extend<{ page: Page }>({
  page: async ({ page }, use) => {
    await page.addInitScript((accessToken) => {
      localStorage.setItem('accessToken', accessToken)
    }, testAccessToken)

    await use(page)
  },
})

export { expect, test, type Page }
