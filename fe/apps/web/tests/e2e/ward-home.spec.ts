import { expect, test } from './fixtures'

function createAccessToken(expiresAt: number) {
  return `header.${btoa(JSON.stringify({ sub: '1', exp: expiresAt }))}.signature`
}

test.beforeEach(async ({ page }) => {
  await page.route('**/api/ward/guardian', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          name: '김보호',
          phone: '010-1234-5678',
          avatarId: 1,
        },
        message: null,
      },
    })
  })

  await page.route('**/api/ward/pairing/status', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          paired: true,
          status: 'CONNECTED',
          guardianName: '김보호',
          guardianPhone: '010-1234-5678',
        },
        message: null,
      },
    })
  })

  await page.route('**/api/ward/transactions/74', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          transactionId: 74,
          type: 'TRANSFER',
          direction: 'OUT',
          status: 'HELD',
          riskLevel: 'CAUTION',
          counterpartyName: '김민수',
          bankName: '국민은행',
          accountNo: '43210201234567',
          amount: 50_000,
          memo: '생활비',
          occurredAt: '2026-08-04T10:00:00',
          balanceAfter: 450_000,
          riskAnalysis: null,
        },
        message: null,
      },
    })
  })

  await page.route('**/api/ward/home', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          userName: '김시니어',
          wallet: {
            walletId: 9207,
            balance: 500_000,
            updatedAt: '2026-08-04T10:00:00',
          },
          pendingApprovalCount: 1,
          pendingApprovals: [
            {
              approvalId: 7,
              transactionId: 74,
              type: 'TRANSFER_OUT',
              amount: 50_000,
              holderName: '김민수',
              bankName: '국민은행',
              accountNo: '43210201234567',
              riskLevel: 'CAUTION',
              requestedAt: '2026-08-04T10:00:00',
              expiredAt: '2026-08-04T10:10:00',
            },
          ],
        },
        message: null,
      },
    })
  })

  await page.route('**/api/ward/approval-requests/7', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          approvalId: 7,
          transactionId: 74,
          type: 'TRANSFER_OUT',
          amount: 50_000,
          memo: '생활비',
          holderName: '김민수',
          bankName: '국민은행',
          accountNo: '43210201234567',
          riskLevel: 'CAUTION',
          requestedAt: '2026-08-04T10:00:00',
          expiredAt: '2026-08-04T10:10:00',
        },
        message: null,
      },
    })
  })
})

test('홈 API의 이름과 잔액을 표시하고 고정 내비게이션을 유지한다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward')

  await expect(page.getByText('김시니어')).toBeVisible()
  await expect(page.getByText('500,000')).toBeVisible()

  const navigation = page.getByRole('navigation', {
    name: '시니어 주요 기능',
  })
  const header = page.getByRole('banner')
  const paymentAction = navigation.getByRole('button', {
    name: '결제',
    exact: true,
  })
  const paymentLabel = paymentAction.getByText('결제', { exact: true })
  const paymentBackground = paymentAction.locator('svg:not(.lucide) path')
  const homeAction = navigation.getByRole('button', {
    name: '홈',
    exact: true,
  })
  await expect(header).toBeVisible()
  await expect(page.getByRole('button', { name: '뒤로 가기' })).toBeHidden()
  await expect(navigation).toBeVisible()
  await expect(navigation.locator('svg.lucide')).toHaveCount(3)
  await expect(homeAction).toBeVisible()
  await expect(paymentLabel).toHaveCSS('color', 'rgb(0, 0, 0)')
  await expect(paymentBackground).toHaveCSS('fill', 'rgb(255, 255, 255)')
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

  const headerBox = await header.boundingBox()
  const navigationBox = await navigation.boundingBox()
  const homeActionBox = await homeAction.boundingBox()

  expect(headerBox).not.toBeNull()
  expect(Math.abs(headerBox?.y ?? 0)).toBeLessThanOrEqual(1)
  expect(navigationBox).not.toBeNull()
  expect(homeActionBox).not.toBeNull()
  expect(homeActionBox?.y ?? 0).toBeLessThan(navigationBox?.y ?? 0)
  expect(homeActionBox?.x ?? -1).toBeGreaterThanOrEqual(0)
  expect(
    (homeActionBox?.x ?? 0) + (homeActionBox?.width ?? 0),
  ).toBeLessThanOrEqual(390)
  expect(
    Math.abs((navigationBox?.y ?? 0) + (navigationBox?.height ?? 0) - 844),
  ).toBeLessThanOrEqual(1)

  await navigation.getByRole('button', { name: '송금', exact: true }).click()
  await expect(page).toHaveURL(/\/ward\/transfer$/)
  await expect(page.getByRole('button', { name: '뒤로 가기' })).toBeVisible()
  await expect(paymentLabel).toHaveCSS('color', 'rgb(0, 0, 0)')
})

test('만료된 access token을 자동 갱신하고 홈을 유지한다', async ({ page }) => {
  const refreshedAccessToken = createAccessToken(
    Math.floor(Date.now() / 1_000) + 900,
  )
  let refreshCount = 0

  await page.addInitScript(() => {
    localStorage.setItem(
      'accessToken',
      'header.eyJzdWIiOiIxIiwiZXhwIjoxfQ.signature',
    )
    localStorage.setItem('refreshToken', 'valid-refresh-token')
  })
  await page.route('**/api/auth/refresh', async (route) => {
    refreshCount += 1
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          accessToken: refreshedAccessToken,
          refreshToken: 'rotated-refresh-token',
          tokenType: 'Bearer',
        },
        message: null,
      },
    })
  })

  await page.goto('/ward')

  await expect(page).toHaveURL(/\/ward$/)
  expect(refreshCount).toBe(1)
  await expect
    .poll(() => page.evaluate(() => localStorage.getItem('refreshToken')))
    .toBe('rotated-refresh-token')
})

test('보호자 승인 대기 거래 목록을 거쳐 승인 상세 화면을 확인한다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward')

  const pendingSummary = page.getByRole('button', {
    name: '보호자 승인 대기 거래 1건 확인하기',
  })
  await expect(pendingSummary).toBeVisible()
  await expect(
    pendingSummary.getByText(/승인 대기 거래가\s*1건\s*있습니다/),
  ).toBeVisible()
  await pendingSummary.click()

  await expect(page).toHaveURL(/\/ward\/pending-transactions$/)
  await expect(
    page.getByRole('banner').getByText('승인 대기 거래'),
  ).toBeVisible()

  const pendingTransfer = page.getByRole('button', {
    name: '송금 김민수 님에게 50,000원 상세 확인',
  })
  await expect(pendingTransfer.getByText('50,000원')).toBeVisible()
  await pendingTransfer.click()

  await expect(page).toHaveURL(/\/ward\/approval-requests\/7$/)
  await expect(
    page.getByRole('heading', { name: '잠깐 확인해 보세요!' }),
  ).toBeVisible()
  await expect(page.getByText('생활비')).toBeVisible()
})

test('refresh token도 만료되면 로그인 화면으로 이동한다', async ({ page }) => {
  await page.addInitScript(() => {
    if (localStorage.getItem('auth-expiry-test-seeded')) return

    localStorage.setItem('auth-expiry-test-seeded', 'true')
    localStorage.setItem(
      'accessToken',
      'header.eyJzdWIiOiIxIiwiZXhwIjoxfQ.signature',
    )
    localStorage.setItem('refreshToken', 'expired-refresh-token')
    sessionStorage.setItem('pay-with:ward-payment', '{"paymentId":1}')
  })
  await page.route('**/api/auth/refresh', async (route) => {
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      json: {
        success: false,
        data: null,
        code: null,
        message: '리프레시 토큰이 유효하지 않습니다.',
      },
    })
  })

  await page.goto('/ward')

  await expect(page).toHaveURL(/\/auth\/sign-in\?reason=session-expired/)
  await expect(
    page.getByRole('status', { name: '로그인 시간이 만료되었어요' }),
  ).toBeVisible()
  await expect
    .poll(() => page.evaluate(() => localStorage.getItem('accessToken')))
    .toBeNull()
  await expect
    .poll(() =>
      page.evaluate(() => sessionStorage.getItem('pay-with:ward-payment')),
    )
    .toBeNull()
})

test('refresh 서버 오류에서는 현재 화면과 세션을 유지한다', async ({
  page,
}) => {
  await page.addInitScript(() => {
    localStorage.setItem(
      'accessToken',
      'header.eyJzdWIiOiIxIiwiZXhwIjoxfQ.signature',
    )
    localStorage.setItem('refreshToken', 'temporarily-unavailable-token')
  })
  await page.route('**/api/auth/refresh', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      json: {
        success: false,
        data: null,
        code: null,
        message: '서버 오류가 발생했습니다.',
      },
    })
  })

  await page.goto('/ward')

  await expect(page).toHaveURL(/\/ward$/)
  await expect
    .poll(() => page.evaluate(() => localStorage.getItem('refreshToken')))
    .toBe('temporarily-unavailable-token')
})

test('사용자 조회 서버 오류에서는 저장된 세션을 유지한다', async ({ page }) => {
  const accessToken = createAccessToken(Math.floor(Date.now() / 1_000) + 900)
  await page.addInitScript(
    ({ accessToken }) => {
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', 'valid-refresh-token')
    },
    { accessToken },
  )
  await page.route('**/api/users/1', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      json: {
        success: false,
        data: null,
        code: null,
        message: '서버 오류가 발생했습니다.',
      },
    })
  })

  await page.goto('/auth/sign-in')

  await expect(page).toHaveURL(/\/auth\/sign-in$/)
  await expect
    .poll(() => page.evaluate(() => localStorage.getItem('accessToken')))
    .toBe(accessToken)
})

test('승인 대기 송금을 approvalId로 상세 조회한다', async ({ page }) => {
  await page.goto('/ward/approval-requests/7')

  await expect(page).toHaveURL(/\/ward\/approval-requests\/7$/)
  await expect(
    page.getByRole('heading', { name: '잠깐 확인해 보세요!' }),
  ).toBeVisible()
  await expect(page.getByText('생활비')).toBeVisible()
  const safetyGuide = page.locator('section').filter({
    has: page.getByRole('heading', { name: 'PayWith 안전 가이드' }),
  })
  await expect(
    safetyGuide.getByRole('button', { name: '보호자에게 연락하기' }),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: '거래 취소하기' })).toBeHidden()
  await expect(page.getByText('보호자에게 연락해 주세요')).toBeVisible()
})

test('잘못된 승인 요청 번호를 홈으로 안내한다', async ({ page }) => {
  await page.goto('/ward/approval-requests/invalid')

  await expect(
    page.getByRole('heading', { name: '올바르지 않은 승인 요청 번호입니다' }),
  ).toBeVisible()
  await expect(
    page.getByRole('button', { name: '홈으로 돌아가기' }),
  ).toBeVisible()
})

test('승인 상세 일시 오류에는 재시도를 제공한다', async ({ page }) => {
  await page.unroute('**/api/ward/approval-requests/7')
  await page.route('**/api/ward/approval-requests/7', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      json: {
        success: false,
        data: null,
        code: 'INTERNAL_ERROR',
        message: '일시적으로 거래를 조회할 수 없습니다.',
      },
    })
  })

  await page.goto('/ward/approval-requests/7')

  await expect(
    page.getByRole('heading', {
      name: '승인 대기 거래를 불러오지 못했습니다',
    }),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: '다시 시도' })).toBeVisible()
})

test('승인 대기 상세 화면에서 거래가 승인(COMPLETED)되면 완료 화면으로 실시간 자동 이동한다', async ({
  page,
}) => {
  let transactionStatus = 'HELD'
  await page.addInitScript(() => {
    sessionStorage.setItem(
      'pay-with:ward-transfer:74',
      JSON.stringify({
        transactionId: 74,
        status: 'HELD',
        holderName: '김민수',
        bankCode: '004',
        bankName: '국민은행',
        accountNo: '432102-01-234567',
        amount: 50_000,
        requestedAt: '2026-08-04T10:00:00',
        expiredAt: '2026-08-04T10:10:00',
      }),
    )
  })

  await page.unroute('**/api/ward/transactions/74')
  await page.route('**/api/ward/transactions/74', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          transactionId: 74,
          type: 'TRANSFER',
          direction: 'OUT',
          status: transactionStatus,
          riskLevel: 'CAUTION',
          counterpartyName: '김민수',
          bankName: '국민은행',
          accountNo: '43210201234567',
          amount: 50_000,
          memo: '생활비',
          occurredAt: '2026-08-04T10:00:00',
          balanceAfter: 450_000,
          riskAnalysis: null,
        },
        message: null,
      },
    })
  })

  await page.goto('/ward/approval-requests/7')
  await expect(page).toHaveURL(/\/ward\/approval-requests\/7$/)
  await expect(
    page.getByRole('heading', { name: '잠깐 확인해 보세요!' }),
  ).toBeVisible()

  // 보호자가 승인하여 거래 상태가 COMPLETED로 변경됨
  transactionStatus = 'COMPLETED'

  // 실시간 폴링에 의해 완료 결과 화면으로 자동 전환 확인
  await expect(page).toHaveURL(/\/ward\/transfer\/74\/complete$/, {
    timeout: 10_000,
  })
  await expect(page.getByRole('heading', { name: '송금 완료' })).toBeVisible()
  await expect(page.getByRole('heading', { name: /50,000/ })).toBeVisible()
})

test('승인 대기 상세 화면에서 거래가 거절(REJECTED)되면 거절 화면으로 실시간 자동 이동한다', async ({
  page,
}) => {
  let transactionStatus = 'HELD'
  await page.addInitScript(() => {
    sessionStorage.setItem(
      'pay-with:ward-transfer:74',
      JSON.stringify({
        transactionId: 74,
        status: 'HELD',
        holderName: '김민수',
        bankCode: '004',
        bankName: '국민은행',
        accountNo: '432102-01-234567',
        amount: 50_000,
        requestedAt: '2026-08-04T10:00:00',
        expiredAt: '2026-08-04T10:10:00',
      }),
    )
  })

  await page.unroute('**/api/ward/transactions/74')
  await page.route('**/api/ward/transactions/74', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          transactionId: 74,
          type: 'TRANSFER',
          direction: 'OUT',
          status: transactionStatus,
          riskLevel: 'DANGER',
          counterpartyName: '김민수',
          bankName: '국민은행',
          accountNo: '43210201234567',
          amount: 50_000,
          memo: '생활비',
          occurredAt: '2026-08-04T10:00:00',
          balanceAfter: 500_000,
          riskAnalysis: null,
        },
        message: null,
      },
    })
  })

  await page.goto('/ward/approval-requests/7')
  await expect(page).toHaveURL(/\/ward\/approval-requests\/7$/)
  await expect(
    page.getByRole('heading', { name: '잠깐 확인해 보세요!' }),
  ).toBeVisible()

  // 보호자가 거절하여 거래 상태가 REJECTED로 변경됨
  transactionStatus = 'REJECTED'

  // 실시간 폴링에 의해 거절 결과 화면으로 자동 전환 확인
  await expect(page).toHaveURL(/\/ward\/transfer\/74\/rejected$/, {
    timeout: 10_000,
  })
  await expect(
    page.getByRole('heading', { name: /보호자가.*거래를 거절했습니다/ }),
  ).toBeVisible()
})
