import { expect, test } from './fixtures'

const guardHome = {
  wards: [
    { wardId: 12, name: '수이', avatarId: 1, hasPending: false },
    { wardId: 13, name: '원이', avatarId: 2, hasPending: false },
  ],
  selectedWard: {
    wardId: 12,
    name: '수이',
    balance: 120000,
    pendingApproval: null,
    pendingApprovalCount: 0,
    recentTransactions: [],
  },
}

const transactions = [
  {
    transactionId: 40,
    type: 'CHARGE',
    status: 'COMPLETED',
    counterpartyName: 'KB국민은행',
    amount: 50000,
    riskLevel: null,
    riskReason: null,
    createdAt: '2026-07-28T10:00:00',
  },
  {
    transactionId: 41,
    type: 'TRANSFER',
    status: 'COMPLETED',
    counterpartyName: '박수취',
    amount: 35000,
    riskLevel: 'DANGER',
    riskReason: '메모에 위험 키워드 포함',
    createdAt: '2026-07-25T09:20:01',
  },
  {
    transactionId: 42,
    type: 'TRANSFER',
    status: 'FAILED',
    counterpartyName: '이실패',
    amount: 20000,
    riskLevel: 'DANGER',
    riskReason: '처음 송금하는 수취인',
    createdAt: '2026-07-24T09:20:01',
  },
]

const transactionDetail = {
  transactionId: 41,
  type: 'TRANSFER',
  direction: 'OUT',
  status: 'COMPLETED',
  riskLevel: 'DANGER',
  counterpartyName: '박수취',
  bankName: '신한은행',
  accountNo: '110234567890',
  amount: 35000,
  memo: '생활비',
  balanceAfter: 120000,
  riskAnalysis: {
    riskScore: 87,
    summary: '평소와 다른 고액 송금이에요.',
    reasons: ['SUSPICIOUS_MEMO'],
  },
  occurredAt: '2026-07-25T09:20:01',
}

const chargeTransactionDetail = {
  transactionId: 40,
  type: 'CHARGE',
  direction: 'IN',
  status: 'COMPLETED',
  riskLevel: null,
  counterpartyName: '194 테스트 피보호자',
  bankName: 'KB국민은행',
  accountNo: '12345678901234',
  amount: 50000,
  memo: null,
  balanceAfter: 120000,
  riskAnalysis: null,
  occurredAt: '2026-07-28T10:00:00',
}

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.route('**/api/guard?**', (route) =>
    route.fulfill({
      json: { success: true, data: guardHome, message: null },
    }),
  )
  await page.route('**/api/guard/wards/12/transactions**', (route) => {
    const url = new URL(route.request().url())

    if (url.pathname.endsWith('/transactions/41')) {
      return route.fulfill({
        json: { success: true, data: transactionDetail, message: null },
      })
    }

    if (url.pathname.endsWith('/transactions/40')) {
      return route.fulfill({
        json: { success: true, data: chargeTransactionDetail, message: null },
      })
    }

    const riskLevel = url.searchParams.get('riskLevel')
    const filtered = riskLevel
      ? transactions.filter(
          (transaction) => transaction.riskLevel === riskLevel,
        )
      : transactions

    return route.fulfill({
      json: {
        success: true,
        data: {
          transactions: filtered,
          page: 0,
          size: 100,
          totalElements: filtered.length,
          totalPages: filtered.length ? 1 : 0,
          hasNext: false,
        },
        message: null,
      },
    })
  })
})

test('보호자가 위험도별 거래 목록을 조회하고 상세를 확인한다', async ({
  page,
}) => {
  await page.goto('/guard/history?wardId=12')

  await expect(page.getByText('+50,000원')).toBeVisible()
  await expect(page.getByText('KB국민은행 충전')).toBeVisible()

  await page.getByRole('button', { name: '위험', exact: true }).click()
  await expect(page.getByText('+50,000원')).toBeHidden()
  await expect(page.getByText('-35,000원')).toBeVisible()

  await page
    .getByRole('button', { name: /수이 계좌 → 박수취 거래 상세 보기/ })
    .click()

  await expect(page).toHaveURL(/\/guard\/history\/41\?wardId=12$/)
  await expect(
    page.getByRole('heading', {
      name: '승인되어 송금이 완료된 거래에요',
    }),
  ).toBeVisible()
  await expect(
    page.getByRole('heading', { name: '거래 안전 확인' }),
  ).toBeVisible()
  await expect(page.getByText('87', { exact: true })).toBeVisible()
  await expect(page.getByText('/ 100', { exact: true })).toBeVisible()
  await expect(page.getByText('확인할 내용')).toBeVisible()
  await expect(page.getByText('평소와 다른 고액 송금이에요.')).toBeVisible()
  await expect(page.getByRole('button', { name: '연락하기' })).toBeVisible()
})

test('보호자가 피보호자의 직접 충전 상세에서 충전 계좌 정보만 확인한다', async ({
  page,
}) => {
  await page.goto('/guard/history?wardId=12')

  await page
    .getByRole('button', { name: /KB국민은행 충전 거래 상세 보기/ })
    .click()

  await expect(page).toHaveURL(/\/guard\/history\/40\?wardId=12$/)
  await expect(page.getByText('거래금액')).toBeVisible()
  await expect(page.getByText('충전', { exact: true })).toBeVisible()
  await expect(page.getByText('194 테스트 피보호자님')).toBeVisible()
  await expect(page.getByText('KB국민은행')).toBeVisible()
  await expect(page.getByText('12345678901234')).toBeVisible()
  await expect(page.getByText('***')).toBeHidden()
  await expect
    .poll(() =>
      page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    )
    .toBe(true)
  await expect
    .poll(() =>
      page
        .getByTestId('charge-account-number')
        .evaluate((element) => element.scrollWidth > element.clientWidth),
    )
    .toBe(true)
  await expect(page.getByText('사용처')).toBeHidden()
  await expect(page.getByText('출금처')).toBeHidden()
  await expect(
    page.getByRole('heading', { name: '안심할 수 있는 거래에요' }),
  ).toBeHidden()
  await expect(page.getByText('이상거래 의심도')).toBeHidden()
  await expect(page.getByText('0점')).toBeHidden()
})

test('거래 내역에는 COMPLETED 상태의 거래만 표시한다', async ({ page }) => {
  await page.goto('/guard/history?wardId=12')

  await expect(page.getByText('KB국민은행 충전')).toBeVisible()
  await expect(page.getByText('박수취')).toBeVisible()
  await expect(page.getByText('이실패')).toBeHidden()
  await expect(page.getByText('송금 실패')).toBeHidden()
})
