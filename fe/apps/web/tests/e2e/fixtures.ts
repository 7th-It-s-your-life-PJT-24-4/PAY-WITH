import { expect, test as base, type Page } from '@playwright/test'

type PairingStatus = 'PAIRED' | 'UNPAIRED'

type TestOptions = {
  pairingStatus: PairingStatus
}

const testAccessToken = `test.${btoa(
  JSON.stringify({
    sub: '1',
    exp: Math.floor(Date.now() / 1_000) + 60 * 60,
  }),
)}.signature`
const testAuthSeedKey = 'pay-with:e2e:auth-seeded'
const pairingStorageKey = 'pay-with:pairing-status'

const test = base.extend<TestOptions & { page: Page }>({
  pairingStatus: ['PAIRED', { option: true }],
  page: async ({ page, pairingStatus }, use) => {
    await page.route(/^https?:\/\/[^/]+\/api(?:\/|\?|$)/, async (route) => {
      console.warn(
        `[E2E] mock되지 않은 API 요청: ${route.request().method()} ${route.request().url()}`,
      )
      await route.fulfill({
        status: 503,
        contentType: 'application/json',
        json: {
          success: false,
          data: null,
          code: 'E2E_UNMOCKED_API',
          message: `E2E mock이 없는 요청입니다: ${route.request().method()} ${route.request().url()}`,
        },
      })
    })
    await page.route('**/api/ward/home', async (route) => {
      await route.fulfill({
        contentType: 'application/json',
        json: {
          success: true,
          data: {
            userName: '테스트 시니어',
            wallet: {
              walletId: 1,
              balance: 0,
              updatedAt: '2026-08-14T00:00:00+09:00',
            },
            pendingApprovalCount: 0,
            pendingApprovals: [],
          },
          message: null,
        },
      })
    })
    await page.route(/\/api\/guard(?:\?.*)?$/, async (route) => {
      await route.fulfill({
        contentType: 'application/json',
        json: {
          success: true,
          data: { wards: [], selectedWard: null },
          message: null,
        },
      })
    })

    await page.addInitScript(
      ({ accessToken, pairingStatus, pairingStorageKey, seedKey }) => {
        if (localStorage.getItem(seedKey)) return

        localStorage.setItem(seedKey, 'true')
        if (!localStorage.getItem('accessToken')) {
          localStorage.setItem('accessToken', accessToken)
        }
        if (pairingStatus === 'PAIRED') {
          sessionStorage.setItem(pairingStorageKey, pairingStatus)
        } else {
          sessionStorage.removeItem(pairingStorageKey)
        }
      },
      {
        accessToken: testAccessToken,
        pairingStatus,
        pairingStorageKey,
        seedKey: testAuthSeedKey,
      },
    )

    await use(page)
  },
})

export { expect, test, type Page }
