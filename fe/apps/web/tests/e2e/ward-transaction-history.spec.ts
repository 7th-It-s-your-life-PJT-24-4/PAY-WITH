import { expect, test } from './fixtures'

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.route('**/ward/transactions**', async (route) => {
    const url = new URL(route.request().url())
    const detailMatch = url.pathname.match(/\/ward\/transactions\/(\d+)$/)

    if (detailMatch) {
      const transactionId = Number(detailMatch[1])

      if (transactionId !== 104 && transactionId !== 105) {
        await route.fulfill({
          status: 404,
          contentType: 'application/json',
          json: {
            success: false,
            data: null,
            message: '거래 내역을 찾을 수 없습니다.',
          },
        })
        return
      }

      await route.fulfill({
        contentType: 'application/json',
        json: {
          success: true,
          data:
            transactionId === 104
              ? {
                  transactionId: 104,
                  type: 'PAYMENT',
                  direction: 'OUT',
                  status: 'BLOCKED',
                  riskLevel: 'DANGER',
                  counterpartyName: 'CU 제주공항점',
                  bankName: null,
                  accountNo: null,
                  amount: 12_000,
                  memo: null,
                  occurredAt: '2026-07-27T12:00:00',
                  balanceAfter: 488_000,
                  riskScore: null,
                  riskAnalysis: {
                    riskScore: 87,
                    summary: '위험한 결제 패턴이 감지됐어요.',
                    reasons: ['SUSPICIOUS_MEMO'],
                  },
                }
              : {
                  transactionId: 105,
                  type: 'TRANSFER',
                  direction: 'OUT',
                  status: 'REJECTED',
                  riskLevel: 'DANGER',
                  counterpartyName: '김철수',
                  bankName: '신한은행',
                  accountNo: '110123456789',
                  amount: 120_000,
                  memo: null,
                  occurredAt: '2026-07-27T13:00:00',
                  balanceAfter: null,
                  riskScore: null,
                  riskAnalysis: {
                    riskScore: 88,
                    summary: '보호자가 위험 거래로 판단해 거절했습니다.',
                    reasons: ['NEW_RECIPIENT'],
                  },
                },
          message: null,
        },
      })
      return
    }

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

test('차단된 위험 거래는 거절 카드로 표시한다', async ({ page }) => {
  await page.goto('/ward/history/104')

  await expect(page.getByText('위험', { exact: true })).toBeVisible()
  await expect(page.getByText('위험한 결제 패턴이 감지됐어요.')).toBeVisible()
  await expect(page.getByText('거절된 거래입니다.')).toBeVisible()
})

test('보호자가 거절한 거래는 거절 카드로 표시한다', async ({ page }) => {
  await page.goto('/ward/history/105')

  await expect(page.getByText('위험', { exact: true })).toBeVisible()
  await expect(
    page.getByText('보호자가 위험 거래로 판단해 거절했습니다.'),
  ).toBeVisible()
  await expect(page.getByText('거절된 거래입니다.')).toBeVisible()
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

test('없는 거래 번호는 오류 상태를 표시한다', async ({ page }) => {
  await page.goto('/ward/history/9999')
  await expect(page.getByText('거래 상세를 불러오지 못했어요.')).toBeVisible()
  await page.getByRole('button', { name: '목록으로 돌아가기' }).click()
  await expect(page).toHaveURL(/\/ward\/history$/)
})
