import { expect, test as base, type Page } from '@playwright/test'

const testAccessToken = `test.${btoa(
  JSON.stringify({
    sub: '1',
    exp: Math.floor(Date.now() / 1_000) + 60 * 60,
  }),
)}.signature`
const testAuthSeedKey = 'pay-with:e2e:auth-seeded'

const test = base.extend<{ page: Page }>({
  page: async ({ page }, use) => {
    await page.addInitScript(
      ({ accessToken, seedKey }) => {
        if (localStorage.getItem(seedKey)) return

        localStorage.setItem(seedKey, 'true')
        if (!localStorage.getItem('accessToken')) {
          localStorage.setItem('accessToken', accessToken)
        }
      },
      { accessToken: testAccessToken, seedKey: testAuthSeedKey },
    )

    await use(page)
  },
})

export { expect, test, type Page }
