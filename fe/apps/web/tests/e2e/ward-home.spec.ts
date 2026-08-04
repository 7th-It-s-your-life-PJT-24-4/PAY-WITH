import { expect, test } from './fixtures'

test.beforeEach(async ({ page }) => {
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

test('홈의 승인 대기 송금을 approvalId로 상세 조회한다', async ({ page }) => {
  await page.goto('/ward')

  const pendingTransfer = page.getByRole('button', {
    name: '송금 김민수 님에게 50,000원 상세 확인',
  })
  await expect(pendingTransfer).toBeVisible()
  await expect(
    page
      .getByRole('region', { name: '보호자 승인을 기다리고 있어요' })
      .getByText('결제', { exact: true }),
  ).toBeHidden()
  await pendingTransfer.click()

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
