import { expect, test } from './fixtures'

const apiResponse = (data: unknown) => ({
  success: true,
  data,
  message: null,
})

const transactionBase = {
  transactionId: 141,
  type: 'TRANSFER',
  status: 'COMPLETED',
  counterpartyName: '안유진',
  amount: 30000,
  riskLevel: 'DANGER',
  riskReason: '최근 평균보다 큰 금액의 거래에요.',
  createdAt: '2026-07-29T10:18:00',
}

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })

  await page.route('**/api/users/*', async (route) => {
    const id = Number(new URL(route.request().url()).pathname.split('/').at(-1))
    await route.fulfill({
      json: apiResponse({
        id,
        name: id === 12 ? '수이' : '보호자',
        phone: id === 12 ? '01012345678' : '01087654321',
        role: id === 12 ? 'WARD' : 'GUARD',
        avatarId: 1,
        createdAt: '2026-07-01T00:00:00',
        updatedAt: '2026-07-01T00:00:00',
      }),
    })
  })

  await page.route(/\/api\/guard(?:\/.*)?(?:\?.*)?$/, async (route) => {
    const url = new URL(route.request().url())

    if (url.pathname === '/api/guard') {
      await route.fulfill({
        json: apiResponse({
          wards: [
            {
              wardId: 12,
              name: '수이',
              avatarId: 1,
              hasPending: false,
            },
          ],
          selectedWard: {
            wardId: 12,
            name: '수이',
            balance: 500000,
            pendingApproval: null,
            recentTransactions: [],
          },
        }),
      })
      return
    }

    if (url.pathname === '/api/guard/wards/12/transactions/141') {
      await route.fulfill({
        json: apiResponse({
          ...transactionBase,
          direction: 'OUT',
          bankName: '국민',
          accountNo: '1234563700',
          memo: null,
          occurredAt: '2026-07-29T10:18:00',
          balanceAfter: 470000,
          riskScore: null,
          analyzedAt: null,
          riskAnalysis: {
            riskScore: 87,
            summary: '평소와 다른 패턴으로 감지되었어요.',
            reasons: [
              {
                ruleCode: 'HIGH_AMOUNT',
                description: '최근 평균보다 큰 금액의 거래에요.',
                score: 30,
              },
            ],
            analyzedAt: '2026-07-29T10:17:59',
          },
        }),
      })
      return
    }

    if (url.pathname === '/api/guard/wards/12/transactions') {
      const riskLevel = url.searchParams.get('riskLevel')
      await route.fulfill({
        json: apiResponse({
          transactions:
            riskLevel === null || riskLevel === 'DANGER'
              ? [transactionBase]
              : [],
          page: 0,
          size: 100,
          totalElements: riskLevel === null || riskLevel === 'DANGER' ? 1 : 0,
          totalPages: 1,
          hasNext: false,
        }),
      })
      return
    }

    await route.abort()
  })
})

test('위험 거래를 필터링하고 읽기 전용 상세를 확인한다', async ({ page }) => {
  await page.goto('/guard/history?wardId=12')

  await expect(page.getByText('수이 계좌 → 안유진')).toBeVisible()
  await page.getByRole('button', { name: '위험', exact: true }).click()
  await expect(
    page.getByRole('button', { name: /안유진 거래 상세 보기/ }),
  ).toContainText('위험')

  await page.getByRole('button', { name: /안유진 거래 상세 보기/ }).click()
  await expect(page).toHaveURL('/guard/history/141?wardId=12')
  await expect(
    page.getByRole('heading', { name: '이상 거래가 발생했어요' }),
  ).toBeVisible()
  await expect(page.getByText('87점')).toBeVisible()
  await expect(page.getByText('국민 3700')).toBeVisible()
  await expect(page.getByRole('button', { name: '연락하기' })).toBeVisible()
  await expect(page.getByRole('button', { name: '승인하기' })).toBeHidden()
  await expect(page.getByRole('button', { name: '거절하기' })).toBeHidden()
})
