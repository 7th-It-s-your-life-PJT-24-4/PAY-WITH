import { expect, test } from './fixtures'

const guardUser = {
  id: 1,
  name: '김보호',
  phone: '01012345678',
  role: 'GUARD',
  avatarId: 2,
  createdAt: '2026-08-05T10:00:00',
  updatedAt: '2026-08-05T10:00:00',
}

const approvalDetail = {
  approvalId: 7,
  transactionId: 8,
  wardId: 12,
  wardName: '김시니어',
  amount: 35000,
  holderName: '박수취',
  bankName: '신한은행',
  accountNo: '110234567890',
  memo: '생활비',
  totalScore: 87,
  riskLevel: 'DANGER',
  ruleHits: [
    { ruleCode: 'SUSPICIOUS_MEMO', description: '메모에 위험 키워드 포함' },
  ],
  requestedAt: '2026-08-05T09:10:00',
  expiredAt: '2026-08-05T12:10:00',
  status: 'PENDING',
  respondedAt: null,
}

const transactionDetail = {
  transactionId: 8,
  type: 'PAYMENT',
  direction: 'OUT',
  status: 'BLOCKED',
  riskLevel: 'DANGER',
  counterpartyName: '위험상점',
  bankName: null,
  accountNo: null,
  amount: 600000,
  memo: null,
  balanceAfter: null,
  riskAnalysis: {
    riskScore: 90,
    summary: '위험 결제가 감지됐어요.',
    reasons: ['PAY_RISKY_CATEGORY'],
  },
  occurredAt: '2026-08-05T09:20:01',
}

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.route('**/api/users/1', (route) =>
    route.fulfill({
      json: { success: true, data: guardUser, message: null },
    }),
  )
  await page.route('**/api/approval-requests/7', (route) =>
    route.fulfill({
      json: { success: true, data: approvalDetail, message: null },
    }),
  )
  await page.route('**/api/guard/wards/12/transactions/8', (route) =>
    route.fulfill({
      json: { success: true, data: transactionDetail, message: null },
    }),
  )
})

test('승인 요청 푸시 클릭 목적지에서 승인 상세를 직접 연다', async ({
  page,
}) => {
  await page.goto('/guard/approval-requests/7?source=push')

  await expect(page).toHaveURL(/\/guard\/approval-requests\/7\?source=push$/)
  await expect(
    page.getByRole('heading', { name: '이상 거래 상세' }),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: '승인하기' })).toBeVisible()
})

test('이상거래 푸시 클릭 목적지에서 해당 피보호자의 거래 상세를 연다', async ({
  page,
}) => {
  const detailRequest = page.waitForRequest((request) =>
    request.url().endsWith('/api/guard/wards/12/transactions/8'),
  )

  await page.goto('/guard/history/8?wardId=12&source=push')

  await detailRequest
  await expect(page).toHaveURL(/\/guard\/history\/8\?wardId=12&source=push$/)
  await expect(
    page.getByRole('heading', { name: '차단된 이상 거래에요' }),
  ).toBeVisible()
  await expect(page.getByText('위험상점')).toBeVisible()
})
