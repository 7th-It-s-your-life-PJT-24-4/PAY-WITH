import { expect, test } from './fixtures'

const apiResponse = (data: unknown) => ({
  success: true,
  data,
  message: null,
})

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })

  await page.route('**/api/users/1', async (route) => {
    await route.fulfill({
      json: apiResponse({
        id: 1,
        name: '보호자',
        phone: '01087654321',
        role: 'GUARD',
        avatarId: 1,
        createdAt: '2026-08-01T00:00:00',
        updatedAt: '2026-08-01T00:00:00',
      }),
    })
  })

  await page.route(/\/api\/guard(?:\/.*)?(?:\?.*)?$/, async (route) => {
    const url = new URL(route.request().url())

    if (url.pathname === '/api/guard') {
      const requestedWardId = Number(url.searchParams.get('wardId'))
      const selectedWardId = requestedWardId === 13 ? 13 : 12
      await route.fulfill({
        json: apiResponse({
          wards: [
            { wardId: 12, name: '수이', avatarId: 1, hasPending: false },
            { wardId: 13, name: '원이', avatarId: 2, hasPending: false },
          ],
          selectedWard: {
            wardId: selectedWardId,
            name: selectedWardId === 13 ? '원이' : '수이',
            balance: 500000,
            pendingApproval: null,
            recentTransactions: [],
          },
        }),
      })
      return
    }

    if (url.pathname === '/api/guard/charges') {
      await route.fulfill({ json: apiResponse({ charges: [] }) })
      return
    }

    if (/\/api\/guard\/wards\/\d+\/transactions$/.test(url.pathname)) {
      await route.fulfill({
        json: apiResponse({
          transactions: [],
          page: 0,
          size: 100,
          totalElements: 0,
          totalPages: 0,
          hasNext: false,
        }),
      })
      return
    }

    await route.abort()
  })
})

test('선택한 시니어를 새로고침과 보호자 탭 이동에서도 유지한다', async ({
  page,
}) => {
  await page.goto('/guard?wardId=12')

  await page.getByRole('button', { name: /원이/ }).click()
  await expect(page).toHaveURL('/guard?wardId=13')
  await page.reload()
  await expect(
    page.getByRole('button', { name: /원이/ }).locator('span').first(),
  ).toHaveClass(/border-2/)

  await page.getByRole('button', { name: '충전', exact: true }).click()
  await expect(page).toHaveURL('/guard/charge?wardId=13')
  await page.reload()
  await expect(
    page.getByRole('button', { name: /원이/ }).locator('span').first(),
  ).toHaveClass(/border-2/)

  await page.getByRole('button', { name: '내역', exact: true }).click()
  await expect(page).toHaveURL('/guard/history?wardId=13')
  await page.reload()
  await expect(
    page.getByRole('button', { name: /원이/ }).locator('span').first(),
  ).toHaveClass(/border-2/)
})
