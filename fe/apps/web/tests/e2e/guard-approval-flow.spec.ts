import { expect, test, type Page } from './fixtures'

const approval = {
  approvalId: 3,
  transactionId: 41,
  wardId: 12,
  wardName: '김시니어',
  amount: 35000,
  holderName: '박수취',
  bankName: '신한은행',
  riskLevel: 'DANGER',
  requestedAt: '2026-08-05T09:10:00',
  expiredAt: '2026-08-05T12:10:00',
  status: 'PENDING',
  respondedAt: null,
}

const approvalDetail = {
  ...approval,
  memo: '생활비',
  accountNo: '110234567890',
  totalScore: 87,
  ruleHits: [
    { ruleCode: 'SUSPICIOUS_MEMO', description: '메모에 위험 키워드 포함' },
  ],
}

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
  occurredAt: '2026-08-05T09:20:01',
}

let transactionDetailResponse = transactionDetail

async function mockApprovalApi(page: Page) {
  await page.route('**/api/approval-requests?wardId=12', (route) =>
    route.fulfill({
      json: { success: true, data: [approval], message: null },
    }),
  )
  await page.route('**/api/approval-requests/3', (route) =>
    route.fulfill({
      json: { success: true, data: approvalDetail, message: null },
    }),
  )
  await page.route('**/api/approval-requests/history?**', (route) => {
    const status = new URL(route.request().url()).searchParams.get('status')
    const item = {
      ...approval,
      status,
      respondedAt: '2026-08-05T09:20:00',
    }
    return route.fulfill({
      json: { success: true, data: [item], message: null },
    })
  })
  await page.route('**/api/guard/wards/12/transactions/41', (route) =>
    route.fulfill({
      json: { success: true, data: transactionDetailResponse, message: null },
    }),
  )
}

test.beforeEach(async ({ page }) => {
  transactionDetailResponse = transactionDetail
  await page.setViewportSize({ width: 390, height: 844 })
  await mockApprovalApi(page)
})

test('대기 거래가 없어도 홈의 이상 거래 내역에서 목록으로 이동한다', async ({
  page,
}) => {
  await page.route(/\/api\/guard(?:\?.*)?$/, (route) =>
    route.fulfill({
      json: {
        success: true,
        data: {
          wards: [
            {
              wardId: 12,
              name: '김시니어',
              avatarId: 1,
              hasPending: false,
            },
          ],
          selectedWard: {
            wardId: 12,
            name: '김시니어',
            balance: 120000,
            pendingApprovals: [],
            pendingApprovalCount: 0,
            recentTransactions: [],
          },
        },
        message: null,
      },
    }),
  )

  await page.goto('/guard')
  await expect(
    page.getByRole('heading', { name: '이상 거래 내역' }),
  ).toBeVisible()
  await expect(page.getByText('대기중인 이상거래가 없어요.')).toBeVisible()
  await page.getByRole('button', { name: '이상 거래 내역 더보기' }).click()

  await expect(page).toHaveURL(/\/guard\/approval-requests\?wardId=12$/)
  await expect(
    page.getByRole('heading', { name: '이상 거래 목록' }),
  ).toBeVisible()
})

test('대기 거래가 있으면 홈에 거래 요약을 표시하고 목록으로 이동한다', async ({
  page,
}) => {
  await page.route(/\/api\/guard(?:\?.*)?$/, (route) =>
    route.fulfill({
      json: {
        success: true,
        data: {
          wards: [
            {
              wardId: 12,
              name: '김시니어',
              avatarId: 1,
              hasPending: true,
            },
            {
              wardId: 13,
              name: '이시니어',
              avatarId: 2,
              hasPending: true,
            },
          ],
          selectedWard: {
            wardId: 12,
            name: '김시니어',
            balance: 120000,
            pendingApprovals: [
              {
                transactionId: 41,
                amount: 35000,
                holderName: '박수취',
                accountNo: '110234567890',
                riskScore: 87,
                riskReason: '메모에 위험 키워드 포함',
                createdAt: '2026-08-05T09:10:00',
              },
            ],
            pendingApprovalCount: 3,
            recentTransactions: [],
          },
        },
        message: null,
      },
    }),
  )

  await page.goto('/guard')
  await expect(
    page.getByRole('heading', { name: '이상 거래 내역' }),
  ).toBeVisible()
  await expect(page.getByText('8월 5일')).toBeVisible()
  await expect(page.getByText('-35,000원')).toBeVisible()
  await expect(page.getByText('박수취')).toBeVisible()
  await expect(page.getByText('위험', { exact: true })).toBeVisible()
  await expect(
    page
      .getByRole('button', { name: '김시니어 이상 거래 있음' })
      .locator('span')
      .first(),
  ).toHaveClass(/border-primary-500/)
  await expect(
    page
      .getByRole('button', { name: '이시니어 이상 거래 있음' })
      .locator('span')
      .first(),
  ).toHaveClass(/border-error/)
  await page.getByRole('button', { name: '이상 거래 내역 더보기' }).click()

  await expect(page).toHaveURL(/\/guard\/approval-requests\?wardId=12$/)
  await expect(
    page.getByRole('heading', { name: '이상 거래 목록' }),
  ).toBeVisible()
})

test('보호자가 이상 거래를 승인하고 transaction API 상세를 확인한다', async ({
  page,
}) => {
  let approveRequested = false
  await page.route('**/api/approval-requests/3/approve', async (route) => {
    approveRequested = true
    await route.fulfill({
      json: {
        success: true,
        data: {
          approvalId: 3,
          transactionId: 41,
          status: 'APPROVED',
          respondedAt: '2026-08-05T09:20:00',
          transfer: {
            status: 'COMPLETED',
            failureReason: null,
            completedAt: '2026-08-05T09:20:01',
            balanceAfter: 120000,
          },
        },
        message: null,
      },
    })
  })

  await page.goto('/guard/approval-requests?wardId=12')
  await page.getByRole('button', { name: /이상 거래 상세 보기/ }).click()
  await page.getByRole('button', { name: '승인하기', exact: true }).click()
  await page
    .getByRole('dialog')
    .getByRole('button', { name: '승인', exact: true })
    .click()

  await expect.poll(() => approveRequested).toBe(true)
  await expect(
    page.getByRole('heading', {
      name: '이상 거래를 승인하고 송금을 완료했어요',
    }),
  ).toBeVisible()
  await page.getByRole('button', { name: '확인' }).click()

  await expect(page).toHaveURL(
    /\/guard\/history\/41\?wardId=12&approvalStatus=approved&source=approval$/,
  )
  await expect(
    page.getByRole('heading', {
      name: '승인되어 송금이 완료된 거래에요',
    }),
  ).toBeVisible()
  await expect(page.getByText('평소와 다른 고액 송금이에요.')).toBeVisible()

  await page
    .getByRole('button', { name: '이상 거래 목록으로', exact: true })
    .click()
  await expect(page).toHaveURL(
    /\/guard\/approval-requests\?wardId=12&status=approved$/,
  )
  await expect(page.getByRole('button', { name: '승인' })).toHaveAttribute(
    'aria-pressed',
    'true',
  )
})

test('보호자가 이상 거래를 거절하고 transaction API 상세를 확인한다', async ({
  page,
}) => {
  transactionDetailResponse = {
    ...transactionDetail,
    status: 'REJECTED',
  }
  await page.route('**/api/approval-requests/3/reject', (route) =>
    route.fulfill({
      json: {
        success: true,
        data: {
          approvalId: 3,
          transactionId: 41,
          status: 'REJECTED',
          respondedAt: '2026-08-05T09:20:00',
          transfer: null,
        },
        message: null,
      },
    }),
  )

  await page.goto('/guard/approval-requests/3?wardId=12')
  await page.getByRole('button', { name: '거절하기', exact: true }).click()
  await page
    .getByRole('dialog')
    .getByRole('button', { name: '거절', exact: true })
    .click()

  await expect(
    page.getByRole('heading', { name: '이상 거래를 거절했어요' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '확인' }).click()
  await expect(
    page.getByRole('heading', { name: '거절된 이상 거래에요' }),
  ).toBeVisible()
  await page
    .getByRole('button', { name: '이상 거래 목록으로', exact: true })
    .click()
  await expect(page).toHaveURL(
    /\/guard\/approval-requests\?wardId=12&status=rejected$/,
  )
})

test('보호자가 거래를 승인했지만 송금이 실패하면 사유와 후속 조치를 확인한다', async ({
  page,
}) => {
  transactionDetailResponse = {
    ...transactionDetail,
    status: 'FAILED',
  }
  await page.route('**/api/approval-requests/3/approve', (route) =>
    route.fulfill({
      json: {
        success: true,
        data: {
          approvalId: 3,
          transactionId: 41,
          status: 'APPROVED',
          respondedAt: '2026-08-05T09:20:00',
          transfer: {
            status: 'FAILED',
            failureReason: '송금 가능한 잔액이 부족합니다.',
            completedAt: null,
            balanceAfter: null,
          },
        },
        message: null,
      },
    }),
  )

  await page.goto('/guard/approval-requests/3?wardId=12')
  await page.getByRole('button', { name: '승인하기', exact: true }).click()
  await page
    .getByRole('dialog')
    .getByRole('button', { name: '승인', exact: true })
    .click()

  await expect(
    page.getByRole('heading', {
      name: '거래를 승인했지만 송금에 실패했어요',
    }),
  ).toBeVisible()
  await expect(page.getByText('송금 가능한 잔액이 부족합니다.')).toBeVisible()
  await expect(
    page.getByRole('button', { name: '잔액 충전해주기' }),
  ).toBeVisible()
  await expect(
    page.getByRole('button', { name: '피보호자에게 연락하기' }),
  ).toBeVisible()

  await page.getByRole('button', { name: '거래 상세 확인' }).click()

  await expect(page).toHaveURL(
    /\/guard\/history\/41\?wardId=12&approvalStatus=approved&source=approval&failureReason=/,
  )
  await expect(
    page.getByRole('heading', {
      name: '거래를 승인했지만 송금에 실패했어요',
    }),
  ).toBeVisible()
  await expect(page.getByText('송금 가능한 잔액이 부족합니다.')).toBeVisible()
  await expect(
    page.getByRole('button', { name: '지갑 충전해주기' }),
  ).toBeVisible()
})
