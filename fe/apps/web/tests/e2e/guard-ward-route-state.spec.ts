import { expect, test, type Page } from './fixtures'

const wards = [
  { wardId: 12, name: '수이', avatarId: 1, hasPending: false },
  { wardId: 13, name: '원이', avatarId: 2, hasPending: false },
]

const selectedWards = {
  12: {
    wardId: 12,
    name: '수이',
    balance: 120000,
    pendingApprovals: [],
    pendingApprovalCount: 0,
    recentTransactions: [],
  },
  13: {
    wardId: 13,
    name: '원이',
    balance: 230000,
    pendingApprovals: [],
    pendingApprovalCount: 0,
    recentTransactions: [],
  },
}

const chargeHistory = {
  transactionId: 202,
  wardId: 13,
  wardName: '원이',
  amount: 30000,
  createdAt: '2026-08-06T14:30:00',
}

async function mockGuardApis(page: Page) {
  await page.route(/\/api\/guard(?:\?.*)?$/, (route) => {
    const requestedWardIdParam = new URL(
      route.request().url(),
    ).searchParams.get('wardId')
    const requestedWardId = Number(requestedWardIdParam)

    if (requestedWardIdParam && !(requestedWardId in selectedWards)) {
      return route.fulfill({
        status: 404,
        json: {
          success: false,
          data: null,
          message: '연동된 피보호자를 찾을 수 없습니다.',
        },
      })
    }

    const wardId = requestedWardId === 13 ? 13 : 12

    return route.fulfill({
      json: {
        success: true,
        data: { wards, selectedWard: selectedWards[wardId] },
        message: null,
      },
    })
  })

  await page.route(/\/api\/guard\/charges(?:\/\d+)?$/, (route) => {
    const isDetail = /\/charges\/\d+$/.test(
      new URL(route.request().url()).pathname,
    )

    return route.fulfill({
      json: isDetail
        ? {
            success: true,
            data: {
              ...chargeHistory,
              memo: null,
              accountId: 7,
              account: {
                bankCode: '004',
                bankName: 'KB국민은행',
                accountNo: '11012300006781',
              },
              balanceAfter: 200000,
            },
            message: null,
          }
        : {
            success: true,
            data: { charges: [chargeHistory] },
            message: null,
          },
    })
  })

  await page.route(
    /\/api\/guard\/wards\/\d+\/transactions(?:\?.*)?$/,
    (route) =>
      route.fulfill({
        json: {
          success: true,
          data: {
            transactions: [],
            page: 0,
            size: 100,
            totalElements: 0,
            totalPages: 0,
            hasNext: false,
          },
          message: null,
        },
      }),
  )
}

test('다른 계정의 시니어 ID가 남아 있으면 기본 시니어로 복구한다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await mockGuardApis(page)

  await page.goto('/guard?wardId=9302')

  await expect(page).toHaveURL(/\/guard\?wardId=12$/)
  await expect(page.getByRole('region', { name: '수이 자산' })).toBeVisible()
})

test('선택한 시니어를 홈·충전·거래내역 URL에서 계속 유지한다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await mockGuardApis(page)

  await page.goto('/guard')
  await expect(page).toHaveURL(/\/guard\?wardId=12$/)

  await page.getByRole('button', { name: /원이/ }).click()
  await expect(page).toHaveURL(/\/guard\?wardId=13$/)
  await expect(page.getByRole('region', { name: '원이 자산' })).toBeVisible()

  await page.reload()
  await expect(page).toHaveURL(/\/guard\?wardId=13$/)
  await expect(page.getByRole('region', { name: '원이 자산' })).toBeVisible()

  await page
    .getByRole('navigation', { name: '보호자 주요 기능' })
    .getByRole('button', { name: '충전' })
    .click()
  await expect(page).toHaveURL(/\/guard\/charge\?wardId=13$/)
  await expect(page.getByText('원이 충전')).toBeVisible()

  await page.getByRole('button', { name: /충전 상세 보기/ }).click()
  await expect(page).toHaveURL(/\/guard\/charge\/202\?wardId=13$/)
  await page.getByRole('button', { name: '충전 내역으로 돌아가기' }).click()
  await expect(page).toHaveURL(/\/guard\/charge\?wardId=13$/)

  await page
    .getByRole('navigation', { name: '보호자 주요 기능' })
    .getByRole('button', { name: '내역' })
    .click()
  await expect(page).toHaveURL(/\/guard\/history\?wardId=13$/)

  await page.reload()
  await expect(page).toHaveURL(/\/guard\/history\?wardId=13$/)
  await expect(page.getByText('아직 거래 내역이 없어요.')).toBeVisible()

  await page.goBack()
  await expect(page).toHaveURL(/\/guard\/charge\?wardId=13$/)
})
