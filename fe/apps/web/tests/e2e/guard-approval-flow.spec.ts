import { expect, test, type Page } from './fixtures'

const approval = {
  approvalId: 3,
  wardId: 12,
  wardName: '김시니어',
  amount: 35000,
  holderName: '박수취',
  bankName: '신한은행',
  riskLevel: 'DANGER',
  requestedAt: '2026-08-05T09:10:00',
  expiredAt: '2026-08-05T09:40:00',
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
}

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await mockApprovalApi(page)
})

test('홈의 거래 확인하기에서 선택한 시니어의 이상 거래 목록으로 이동한다', async ({
  page,
}) => {
  await page.route('**/api/guard', (route) =>
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
          ],
          selectedWard: {
            wardId: 12,
            name: '김시니어',
            balance: 120000,
            pendingApproval: {
              transactionId: 41,
              amount: 35000,
              holderName: '박수취',
              accountNo: '110234567890',
              riskScore: 87,
              riskReason: '메모에 위험 키워드 포함',
              createdAt: '2026-08-05T09:10:00',
            },
            recentTransactions: [],
          },
        },
        message: null,
      },
    }),
  )

  await page.goto('/guard')
  await page.getByRole('button', { name: '거래 확인하기' }).click()

  await expect(page).toHaveURL(/\/guard\/approval-requests\?wardId=12$/)
  await expect(
    page.getByRole('heading', { name: '이상 거래 목록' }),
  ).toBeVisible()
})

test('보호자가 목록에서 이상 거래를 승인하고 결과 상세를 확인한다', async ({
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
  await expect(
    page.getByRole('heading', { name: '이상 거래 목록' }),
  ).toBeVisible()
  await page.getByRole('button', { name: /이상 거래 상세 보기/ }).click()

  await expect(
    page.getByRole('heading', { name: '이상 거래가 발생했어요' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '승인', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '승인', exact: true }).click()

  await expect.poll(() => approveRequested).toBe(true)
  await expect(
    page.getByRole('heading', { name: '이상 거래를 승인했어요' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '확인' }).click()
  await expect(page).toHaveURL(/\/guard\/approval-requests\/3\/result$/)
  await expect(
    page.getByRole('heading', { name: '승인된 이상 거래에요' }),
  ).toBeVisible()
})

test('보호자가 이상 거래를 거절하고 거절 상태 상세를 확인한다', async ({
  page,
}) => {
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

  await page.goto('/guard/approval-requests/3')
  await page.getByRole('button', { name: '거절', exact: true }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '거절', exact: true }).click()

  await expect(
    page.getByRole('heading', { name: '이상 거래를 거절했어요' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '확인' }).click()
  await expect(
    page.getByRole('heading', { name: '거절된 이상 거래에요' }),
  ).toBeVisible()
})
