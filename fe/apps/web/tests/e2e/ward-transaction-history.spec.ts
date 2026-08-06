import { expect, test } from './fixtures'

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.route('**/ward/transactions**', async (route) => {
    const url = new URL(route.request().url())
    const category = url.searchParams.get('category')
    const keyword = url.searchParams.get('keyword')
    const transactions = [
      {
        transactionId: 101,
        type: 'CHARGE',
        direction: 'IN',
        title: 'KB국민은행',
        amount: 50_000,
        status: 'COMPLETED',
        riskLevel: null,
        occurredAt: '2026-07-28T10:00:00',
      },
      {
        transactionId: 104,
        type: 'PAYMENT',
        direction: 'OUT',
        title: 'CU 제주공항점',
        amount: 12_000,
        status: 'BLOCKED',
        riskLevel: 'DANGER',
        occurredAt: '2026-07-27T12:00:00',
      },
    ].filter(
      (transaction) =>
        (category === 'ALL' || transaction.type === category) &&
        (!keyword || transaction.title.includes(keyword)),
    )

    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          transactions,
          page: 0,
          size: 20,
          totalElements: transactions.length,
          totalPages: transactions.length ? 1 : 0,
          hasNext: false,
        },
        message: null,
      },
    })
  })
  await page.goto('/ward/history')
})

test('결제 내역을 서버 조건으로 검색한다', async ({ page }) => {
  await expect(page.getByRole('heading', { name: '거래 내역' })).toBeVisible()

  await page
    .getByRole('group', { name: '거래 종류' })
    .getByRole('button', { name: '결제', exact: true })
    .click()
  await expect(page.getByText('김철수', { exact: true })).toBeHidden()

  await page.getByRole('searchbox', { name: '거래 내역 검색' }).fill('제주')
  await expect(page.getByText('CU 제주공항점', { exact: true })).toBeVisible()
  await expect(page.getByText('거래 차단됨', { exact: true })).toBeVisible()
})

test('보호자가 승인한 위험 거래는 위험으로 표시한다', async ({ page }) => {
  await page.goto('/ward/history/106')

  await expect(page.getByText('위험', { exact: true })).toBeVisible()
  await expect(
    page.getByText('위험한 거래로 판단되었지만 보호자가 승인했습니다.'),
  ).toBeVisible()
  await expect(page.getByText('거절된 거래입니다.')).toBeHidden()
})

test('위험 평가가 없는 충전 내역은 위험도 배지를 표시하지 않는다', async ({
  page,
}) => {
  await page.getByRole('button', { name: '충전' }).click()
  await expect(page.getByText('KB국민은행', { exact: true })).toBeVisible()
  await expect(page.getByText('안전', { exact: true })).toBeHidden()
  await expect(page.getByText('주의', { exact: true })).toBeHidden()
  await expect(page.getByText('위험', { exact: true })).toBeHidden()
})

test('잘못된 거래 번호는 거래 내역 목록으로 이동한다', async ({ page }) => {
  await page.goto('/ward/history/9999')
  await expect(page).toHaveURL(/\/ward\/history$/)
})
