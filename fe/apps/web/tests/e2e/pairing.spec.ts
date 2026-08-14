import { expect, test } from './fixtures'

test.use({ pairingStatus: 'UNPAIRED' })

test('보호자가 직접 접속해 인증 코드를 생성하고 복사한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.route('**/api/guard/pairing', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          code: '72941',
          inviteUrl: 'http://localhost:5173/ward/pairing?code=72941',
          expiresAt: '2026-08-04T14:30:00',
        },
        message: null,
      }),
    })
  })
  await page.goto('/ward')
  await page.evaluate(() => {
    globalThis.history.pushState({}, '', '/guard/pairing/code')
    globalThis.dispatchEvent(new PopStateEvent('popstate'))
  })
  await expect(page).toHaveURL(/\/guard\/pairing\/code$/)

  await expect(
    page.getByRole('heading', { name: '시니어 연결하기' }),
  ).toBeVisible()
  const issuedCode = await page.getByText(/^\d{5}$/).textContent()
  expect(issuedCode).toMatch(/^\d{5}$/)
  await page.getByRole('button', { name: '코드 복사' }).click()
  await expect(page.getByRole('status')).toContainText('코드가 복사되었습니다')

  await page.getByRole('button', { name: '뒤로 가기' }).click()
  await expect(page).toHaveURL((url) => url.pathname === '/guard')
})

test('미연결 시니어가 보호자 코드를 입력하고 연결을 완료한다', async ({
  page,
}) => {
  let paired = false
  await page.setViewportSize({ width: 390, height: 854 })
  await page.route('**/api/ward/home', async (route) => {
    if (!paired) {
      await route.fulfill({
        status: 403,
        contentType: 'application/json',
        json: {
          success: false,
          data: null,
          code: 'WARD_001',
          message: '페어링 완료 후 이용할 수 있습니다.',
        },
      })
      return
    }

    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          userName: '김시니어',
          wallet: {
            walletId: 9207,
            balance: 500_000,
            updatedAt: '2026-08-05T10:00:00',
          },
          pendingApprovalCount: 0,
          pendingApprovals: [],
        },
        message: null,
      },
    })
  })
  await page.route('**/api/ward/pairing', async (route) => {
    paired = true
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          relationId: 21,
          guardId: 7,
          guardName: '김철수',
          status: 'ACTIVE',
          connectedAt: '2026-08-04T14:26:00',
        },
        message: null,
      }),
    })
  })
  await page.route('**/api/users/1', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          id: 1,
          name: '김시니어',
          phone: '01012345678',
          role: 'WARD',
          avatarId: 1,
          createdAt: '2026-08-04T14:20:00',
          updatedAt: '2026-08-04T14:20:00',
        },
        message: null,
      },
    })
  })
  await page.route('**/api/ward/guardian', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          id: 7,
          name: '김철수',
          phone: '01087654321',
          avatarId: 2,
        },
        message: null,
      },
    })
  })
  await page.goto('/ward?pairing=unpaired')

  await page.getByRole('button', { name: '보호자와 연결하기' }).click()
  await expect(page).toHaveURL(/\/ward\/pairing$/)

  await page.getByLabel('인증 코드 5자리 입력').fill('72941')
  await page.getByRole('button', { name: '연결하기' }).click()
  await expect(page).toHaveURL(/\/ward\/pairing\/complete$/)
  await expect(page.getByRole('heading', { name: '연결 성공!' })).toBeVisible()
  await expect(
    page.getByLabel('연결된 보호자').getByText('김철수', { exact: true }),
  ).toBeVisible()

  await page.getByRole('button', { name: '홈으로' }).click()
  await expect(page).toHaveURL(/\/ward$/)
  await expect(
    page.getByRole('button', { name: '보호자와 연결하기' }),
  ).toBeHidden()
})
