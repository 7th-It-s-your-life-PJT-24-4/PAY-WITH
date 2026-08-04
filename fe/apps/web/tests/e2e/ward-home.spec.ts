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
  await expect(header).toBeVisible()
  await expect(navigation).toBeVisible()

  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
  const headerBox = await header.boundingBox()
  const navigationBox = await navigation.boundingBox()
  expect(Math.abs(headerBox?.y ?? 0)).toBeLessThanOrEqual(1)
  expect(
    Math.abs((navigationBox?.y ?? 0) + (navigationBox?.height ?? 0) - 844),
  ).toBeLessThanOrEqual(1)
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
})
