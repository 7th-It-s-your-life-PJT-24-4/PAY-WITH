import { expect, test, type Page } from './fixtures'

const baseStoredTransferDetail = {
  holderName: '박지연',
  bankCode: '088',
  bankName: '신한은행',
  accountNo: '110-234-567890',
  amount: 30_000,
  memo: null,
  requestedAt: '2026-07-24T15:00:00+09:00',
  expiredAt: null,
  respondedAt: null,
  completedAt: null,
  balanceAfter: null,
  riskAnalysis: null,
  failureCode: null,
  failureMessage: null,
}

const storedTransferDetails = [
  {
    ...baseStoredTransferDetail,
    transactionId: 73,
    status: 'COMPLETED',
    holderName: '김민수',
    bankCode: '004',
    bankName: '국민은행',
    accountNo: '432102-01-234567',
    amount: 50_000,
    requestedAt: '2026-07-24T14:30:00+09:00',
    expiredAt: '2026-07-24T14:40:00+09:00',
    respondedAt: '2026-07-24T14:32:00+09:00',
    completedAt: '2026-07-24T14:32:01+09:00',
    balanceAfter: 1_200_000,
    riskAnalysis: { riskScore: 0, reasons: [] },
  },
  {
    ...baseStoredTransferDetail,
    transactionId: 74,
    status: 'HELD',
    holderName: '김민수',
    bankCode: '004',
    bankName: '국민은행',
    accountNo: '432102-01-234567',
    amount: 50_000,
    requestedAt: '2026-07-24T14:30:00+09:00',
    expiredAt: '2026-07-24T14:40:00+09:00',
  },
  {
    ...baseStoredTransferDetail,
    transactionId: 75,
    status: 'REJECTED',
    holderName: '이지혜',
    bankCode: '004',
    bankName: 'KB국민은행',
    accountNo: '123123890123',
    amount: 500_000,
    requestedAt: '2026-07-24T14:30:00+09:00',
    expiredAt: '2026-07-24T14:40:00+09:00',
    respondedAt: '2026-07-24T14:36:00+09:00',
  },
  {
    ...baseStoredTransferDetail,
    transactionId: 76,
    status: 'HELD',
    expiredAt: '2026-07-24T15:10:00+09:00',
  },
  {
    ...baseStoredTransferDetail,
    transactionId: 77,
    status: 'EXPIRED',
    expiredAt: '2026-07-24T15:10:00+09:00',
  },
  {
    ...baseStoredTransferDetail,
    transactionId: 78,
    status: 'FAILED',
    respondedAt: '2026-07-24T15:01:00+09:00',
    failureCode: 'INSUFFICIENT_BALANCE',
    failureMessage: '송금 가능한 잔액이 부족합니다.',
  },
  {
    ...baseStoredTransferDetail,
    transactionId: 79,
    status: 'CANCELED',
    respondedAt: '2026-07-24T15:01:00+09:00',
  },
]

async function submitTransferWithPin(page: Page, pin: string) {
  await page.goto('/ward/transfer')
  await page
    .getByRole('button', { name: /김민수/ })
    .first()
    .click()
  await page.getByRole('button', { name: '+5만원', exact: true }).click()
  await page.getByRole('button', { name: '다음으로' }).click()
  await page.getByRole('button', { name: '송금하기' }).click()
  for (const digit of pin)
    await page.getByRole('button', { name: digit, exact: true }).click()
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript((details) => {
    for (const detail of details) {
      sessionStorage.setItem(
        `pay-with:ward-transfer:${detail.transactionId}`,
        JSON.stringify(detail),
      )
    }
  }, storedTransferDetails)

  await page.route('**/api/ward/home', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          userName: '김시니어',
          wallet: {
            walletId: 9207,
            balance: 1_250_000,
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

  await page.route('**/api/ward/wallet', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          walletId: 9207,
          balance: 1_250_000,
          updatedAt: '2026-08-04T10:00:00',
        },
        message: null,
      },
    })
  })

  await page.route('**/api/ward/safe-accounts', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          safeAccounts: [
            {
              safeAccountId: 1,
              recipientId: 1,
              holderName: '김민수',
              bankCode: '004',
              bankName: '국민은행',
              accountNo: '43210201234567',
              accountAlias: '민수 형',
              isVerified: true,
              createdAt: '2026-08-01T12:00:00',
            },
          ],
        },
        message: null,
      },
    })
  })

  await page.route('**/api/banks', async (route) => {
    expect(route.request().method()).toBe('GET')
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: [
          { bankCode: '004', bankName: 'KB국민은행' },
          { bankCode: '020', bankName: '우리은행' },
          { bankCode: '081', bankName: '하나은행' },
          { bankCode: '088', bankName: '신한은행' },
        ],
        message: null,
      },
    })
  })

  await page.route('**/api/ward/filter-bank', async (route) => {
    expect(route.request().postDataJSON()).toEqual({
      accountNo: '12345678',
    })
    await new Promise((resolve) => setTimeout(resolve, 200))
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          banks: [
            { bankCode: '020', bankName: '우리은행' },
            { bankCode: '081', bankName: '하나은행' },
          ],
        },
        message: null,
      },
    })
  })

  await page.route('**/api/ward/transfers/recipient*', async (route) => {
    if (route.request().method() === 'GET') {
      const keyword = new URL(route.request().url()).searchParams.get('keyword')
      const recipients = [
        {
          recipientId: 1,
          holderName: '김민수',
          bankCode: '004',
          bankName: '국민은행',
          accountNo: '43210201234567',
          lastSentAt: '2026-07-31T12:00:00',
          sendCount: 3,
          isRegisteredSafe: true,
          safeAccountId: 1,
          accountAlias: null,
        },
        {
          recipientId: 2,
          holderName: '박지연',
          bankCode: '088',
          bankName: '신한은행',
          accountNo: '110234567890',
          lastSentAt: '2026-07-30T12:00:00',
          sendCount: 1,
          isRegisteredSafe: false,
          safeAccountId: null,
          accountAlias: null,
        },
      ].filter((recipient) =>
        keyword
          ? `${recipient.holderName}${recipient.accountNo}`.includes(keyword)
          : true,
      )
      await route.fulfill({
        contentType: 'application/json',
        json: { success: true, data: { recipients }, message: null },
      })
      return
    }

    const request = route.request().postDataJSON() as {
      bankCode: string
      accountNo: string
    }
    await new Promise((resolve) => setTimeout(resolve, 200))
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          bankCode: request.bankCode,
          bankName: request.bankCode === '081' ? '하나은행' : '우리은행',
          accountNo: request.accountNo,
          recipientName: '김준호',
        },
        message: null,
      },
    })
  })

  await page.route('**/api/ward/transfers', async (route) => {
    const request = route.request().postDataJSON() as {
      bankCode: string
      accountNo: string
      amount: number
      memo: string | null
      transferPin: string
    }
    await new Promise((resolve) => setTimeout(resolve, 200))
    if (request.transferPin === '222222') {
      await route.fulfill({
        status: 202,
        contentType: 'application/json',
        json: {
          success: true,
          data: {
            transactionId: 74,
            status: 'HELD',
            holderName: null,
            bankCode: null,
            bankName: null,
            accountNo: null,
            amount: null,
            memo: null,
            completedAt: null,
            balanceAfter: null,
          },
          message: null,
        },
      })
      return
    }

    if (request.transferPin === '000000') {
      await route.fulfill({
        status: 400,
        contentType: 'application/json',
        json: {
          success: false,
          data: null,
          code: null,
          message: '송금 비밀번호가 올바르지 않습니다.',
        },
      })
      return
    }

    if (request.transferPin === '333333') {
      await route.fulfill({
        status: 422,
        contentType: 'application/json',
        json: {
          success: false,
          data: null,
          code: null,
          message: '송금 가능한 잔액이 부족합니다.',
        },
      })
      return
    }

    if (request.transferPin === '999999') {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        json: {
          success: false,
          data: null,
          code: null,
          message:
            '송금 처리 중 오류가 발생했습니다. 잔액을 확인 후 고객센터로 문의해주세요.',
        },
      })
      return
    }

    expect(route.request().headers()['idempotency-key']).toBeTruthy()
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          transactionId: 73,
          status: 'COMPLETED',
          holderName: '김민수',
          bankCode: request.bankCode,
          bankName: 'KB국민은행',
          accountNo: request.accountNo,
          amount: request.amount,
          memo: request.memo,
          completedAt: '2026-07-31T12:00:00',
          balanceAfter: 1_200_000,
        },
        message: null,
      },
    })
  })
})

test('모달이 열려도 고정 헤더와 하단 내비게이션 위치를 유지한다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 1024, height: 844 })
  await page.goto('/ward/transfer')

  const header = page.locator('header')
  const main = page.locator('main')
  const navigation = page.locator('nav[aria-label="시니어 주요 기능"]')
  const headerBefore = await header.boundingBox()
  const mainBefore = await main.boundingBox()
  const navigationBefore = await navigation.boundingBox()

  await page.getByRole('tab', { name: '최근 보낸 사람' }).click()
  await page.getByRole('button', { name: '박지연 안심계좌 추가' }).click()
  await expect(page.getByRole('dialog', { name: '연락처 추가' })).toBeVisible()

  const headerAfter = await header.boundingBox()
  const mainAfter = await main.boundingBox()
  const navigationAfter = await navigation.boundingBox()
  expect(headerAfter?.x).toBeCloseTo(headerBefore?.x ?? 0, 1)
  expect(mainAfter?.x).toBeCloseTo(mainBefore?.x ?? 0, 1)
  expect(navigationAfter?.x).toBeCloseTo(navigationBefore?.x ?? 0, 1)
})

test('계좌번호로 은행을 찾고 계좌를 확인한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/transfer/account')

  await page.getByLabel('계좌 번호').fill('12345678')

  const bankButtons = page
    .getByRole('region', { name: '은행 목록' })
    .getByRole('button')
  await expect(bankButtons).toHaveCount(4)
  await expect(bankButtons.nth(0)).toContainText('우리은행')
  await expect(bankButtons.nth(1)).toContainText('하나은행')
  await expect(bankButtons.nth(2)).toContainText('KB국민은행')
  await expect(bankButtons.nth(3)).toContainText('신한은행')

  const hanaBankButton = page.getByRole('button', { name: /하나은행/ })
  await expect(hanaBankButton.locator('img')).toBeVisible()
  await hanaBankButton.click()
  await page.getByRole('button', { name: '다음으로' }).click()

  await expect(
    page.getByRole('dialog', { name: '계좌를 확인하고 있습니다' }),
  ).toBeVisible()
  await expect(page).toHaveURL(/\/ward\/transfer\/amount$/)
  await expect(page.getByText('김준호')).toBeVisible()
})

test('전체 은행 목록을 한 페이지에 최대 6개씩 표시한다', async ({ page }) => {
  await page.unroute('**/api/banks')
  await page.route('**/api/banks', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: [
          { bankCode: '004', bankName: 'KB국민은행' },
          { bankCode: '020', bankName: '우리은행' },
          { bankCode: '081', bankName: '하나은행' },
          { bankCode: '088', bankName: '신한은행' },
          { bankCode: '003', bankName: '기업은행' },
          { bankCode: '011', bankName: '농협은행' },
          { bankCode: '023', bankName: 'SC제일은행' },
          { bankCode: '027', bankName: '한국씨티은행' },
        ],
        message: null,
      },
    })
  })

  await page.goto('/ward/transfer/account')
  await page.getByLabel('계좌 번호').fill('12345678')

  const bankList = page.getByRole('region', { name: '은행 목록' })
  await expect(bankList.getByRole('button')).toHaveCount(6)
  await expect(page.getByText('1/2', { exact: true })).toBeVisible()
  await expect(bankList.getByRole('button').nth(0)).toContainText('우리은행')
  await expect(bankList.getByRole('button').nth(1)).toContainText('하나은행')
  const pagination = page.getByRole('navigation', {
    name: '은행 목록 페이지',
  })
  const paginationBefore = await pagination.boundingBox()

  await page.getByRole('button', { name: '다음 은행 목록' }).click()

  await expect(bankList.getByRole('button')).toHaveCount(2)
  await expect(page.getByText('2/2', { exact: true })).toBeVisible()
  await expect(bankList.getByRole('button').nth(0)).toContainText('SC제일은행')
  await expect(bankList.getByRole('button').nth(1)).toContainText(
    '한국씨티은행',
  )
  const paginationAfter = await pagination.boundingBox()
  expect(paginationAfter?.y).toBeCloseTo(paginationBefore?.y ?? 0, 1)

  await page.getByRole('button', { name: '이전 은행 목록' }).click()
  await expect(bankList.getByRole('button')).toHaveCount(6)
  await expect(page.getByText('1/2', { exact: true })).toBeVisible()
})

test('은행 후보 조회에 실패하면 계좌번호 화면에서 다시 시도할 수 있다', async ({
  page,
}) => {
  await page.unroute('**/api/ward/filter-bank')
  await page.route('**/api/ward/filter-bank', async (route) => {
    await route.fulfill({
      status: 500,
      contentType: 'application/json',
      json: {
        success: false,
        data: null,
        code: 'BANK_001',
        message: '은행 정보를 조회하지 못했습니다.',
      },
    })
  })
  await page.goto('/ward/transfer/account')

  await page.getByLabel('계좌 번호').fill('12345678')

  await expect(page.getByRole('alert')).toContainText(
    '은행 정보를 조회하지 못했습니다.',
  )
  await expect(page.getByRole('button', { name: '다음으로' })).toBeDisabled()
})

test('추천 은행이 없어도 전체 은행 목록을 표시한다', async ({ page }) => {
  await page.unroute('**/api/ward/filter-bank')
  await page.route('**/api/ward/filter-bank', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: { success: true, data: { banks: [] }, message: null },
    })
  })
  await page.goto('/ward/transfer/account')

  await page.getByLabel('계좌 번호').fill('12345678')

  const bankButtons = page
    .getByRole('region', { name: '은행 목록' })
    .getByRole('button')
  await expect(bankButtons).toHaveCount(4)
  await expect(bankButtons.nth(0)).toContainText('KB국민은행')
  await expect(bankButtons.nth(1)).toContainText('신한은행')
  await expect(bankButtons.nth(2)).toContainText('우리은행')
  await expect(bankButtons.nth(3)).toContainText('하나은행')
  await expect(page.getByRole('button', { name: '다음으로' })).toBeDisabled()
})

test('최근 수취인을 별칭과 함께 연락처에 추가한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/transfer')
  await page.getByRole('tab', { name: '최근 보낸 사람' }).click()

  const addContactButton = page.getByRole('button', {
    name: '박지연 안심계좌 추가',
  })
  await expect(addContactButton).toContainText('안심계좌 추가')
  const buttonBox = await addContactButton.boundingBox()
  expect(buttonBox?.height).toBeGreaterThanOrEqual(48)
  await addContactButton.click()

  const dialog = page.getByRole('dialog', { name: '연락처 추가' })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByText('신한은행 110234567890')).toBeVisible()

  await dialog.getByLabel('연락처 별칭').fill('지연 이모')
  await dialog.getByRole('button', { name: '추가하기' }).click()

  await expect(dialog).toBeHidden()
  await expect(
    page.getByRole('button', { name: '박지연 안심계좌 추가' }),
  ).toBeHidden()
})

test('안심계좌 탭을 기본으로 표시하고 최근 보낸 사람 탭으로 전환한다', async ({
  page,
}) => {
  await page.goto('/ward/transfer')

  await expect(page.getByRole('tab', { name: '안심계좌' })).toHaveAttribute(
    'aria-selected',
    'true',
  )
  await expect(page.getByLabel('안심계좌 검색')).toHaveCount(0)
  await expect(page.getByText('민수 형')).toBeVisible()

  await page.getByRole('tab', { name: '최근 보낸 사람' }).click()
  await expect(
    page.getByRole('tab', { name: '최근 보낸 사람' }),
  ).toHaveAttribute('aria-selected', 'true')
  await expect(page.getByText('박지연')).toBeVisible()
})

test('송금 대상이 없으면 최근 섹션을 숨기고 안심계좌 빈 상태를 표시한다', async ({
  page,
}) => {
  await page.unroute('**/api/ward/transfers/recipient*')
  await page.route('**/api/ward/transfers/recipient*', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: { success: true, data: { recipients: [] }, message: null },
    })
  })
  await page.unroute('**/api/ward/safe-accounts')
  await page.route('**/api/ward/safe-accounts', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: { success: true, data: { safeAccounts: [] }, message: null },
    })
  })

  await page.goto('/ward/transfer')

  await expect(page.getByText('등록된 안심계좌가 없습니다.')).toBeVisible()
  await page.getByRole('tab', { name: '최근 보낸 사람' }).click()
  await expect(page.getByText('최근 보낸 사람이 없습니다.')).toBeVisible()
})

test('최근 수취인을 선택해 시니어 송금 플로우를 완료한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward')

  await page.getByRole('button', { name: '송금하기' }).click()
  await expect(
    page.getByRole('heading', { name: '송금 대상 선택' }),
  ).toBeVisible()

  await page
    .getByRole('button', { name: /김민수/ })
    .first()
    .click()
  await expect(
    page.getByRole('heading', { name: '송금 금액 입력' }),
  ).toBeVisible()

  await page.getByRole('button', { name: '+5만원', exact: true }).click()
  await page.getByRole('button', { name: '다음으로' }).click()

  await expect(page.getByRole('heading', { name: '송금 확인' })).toBeVisible()
  await expect(page.getByText('50,000원').first()).toBeVisible()
  await page.getByRole('button', { name: '송금하기' }).click()

  await expect(
    page.getByRole('heading', { name: '비밀번호 입력' }),
  ).toBeVisible()
  await expect(
    page.getByRole('navigation', { name: '시니어 주요 기능' }),
  ).toBeHidden()

  for (const digit of ['1', '2', '3', '4', '5', '6']) {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }

  await expect(page.getByText('안전하게 송금하고 있습니다')).toBeVisible()
  await expect(page.getByRole('heading', { name: '송금 완료' })).toBeVisible()
  await expect(page.getByText('송금 후 잔액')).toBeVisible()
  await expect(page.getByText('1,200,000원')).toBeVisible()
  await expect(page).toHaveURL(/\/ward\/transfer\/73\/complete$/)
  await expect(page.getByText('김민수')).toBeVisible()
  await page.reload()
  await expect(page).toHaveURL(/\/ward\/transfer\/73\/complete$/)
  await expect(page.getByText('김민수')).toBeVisible()

  await page.goBack()
  await expect(page).toHaveURL(/\/ward$/)
  await expect(page.getByRole('heading', { name: 'PayWith' })).toBeVisible()
  await expect(page.getByText('안전하게 송금하고 있습니다')).toBeHidden()

  await page.goBack()
  await expect(page).toHaveURL(/\/ward\/transfer$/)
})

test('송금 실패 원인에 맞는 안전한 후속 행동을 제공한다', async ({ page }) => {
  await submitTransferWithPin(page, '000000')
  await expect(
    page.getByText('송금 비밀번호가 올바르지 않습니다.'),
  ).toBeVisible()
  await page.getByRole('button', { name: '비밀번호 다시 입력' }).click()
  await expect(page).toHaveURL(/\/ward\/transfer\/password$/)

  await submitTransferWithPin(page, '333333')
  await expect(page.getByText('송금 가능한 잔액이 부족합니다.')).toBeVisible()
  await expect(page.getByRole('button', { name: '홈으로' })).toBeVisible()
  await page.getByRole('button', { name: '충전하기' }).click()
  await expect(page).toHaveURL(/\/ward\/charge$/)

  await submitTransferWithPin(page, '999999')
  await expect(page.getByText(/잔액을 확인 후 고객센터/)).toBeVisible()
  await expect(
    page.getByRole('button', { name: '비밀번호 다시 입력' }),
  ).toBeHidden()
  await page.getByRole('button', { name: '홈으로' }).click()
  await expect(page).toHaveURL(/\/ward$/)
})

test('잔액 초과 금액을 유지하고 잔액 배지를 강조한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/transfer')

  await page
    .getByRole('button', { name: /김민수/ })
    .first()
    .click()

  const amountInput = page.getByLabel('송금 금액 입력')
  await amountInput.fill('2000000')

  const balanceBadge = page.getByText('잔액 1,250,000원', { exact: true })
  const amountCard = balanceBadge.locator('..')

  await expect(page.getByText('2,000,000')).toBeVisible()
  await expect(balanceBadge).toHaveCSS('background-color', 'rgb(255, 97, 97)')
  await expect(balanceBadge).toHaveCSS('color', 'rgb(255, 255, 255)')
  await expect(page.getByRole('button', { name: '다음으로' })).toBeDisabled()
  const overBalanceCardBox = await amountCard.boundingBox()

  await amountInput.fill('200000')

  await expect(page.getByText('200,000')).toBeVisible()
  await expect(balanceBadge).toHaveCSS('background-color', 'rgb(204, 239, 246)')
  await expect(balanceBadge).toHaveCSS('color', 'rgb(0, 106, 126)')
  await expect(page.getByRole('button', { name: '다음으로' })).toBeEnabled()
  const validAmountCardBox = await amountCard.boundingBox()

  expect(overBalanceCardBox?.height).toBe(validAmountCardBox?.height)
})

test('이상 거래 승인 대기 중에도 새 송금을 시작할 수 있다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/transfer')

  await page
    .getByRole('button', { name: /김민수/ })
    .first()
    .click()
  await page.getByRole('button', { name: '+5만원', exact: true }).click()
  await page.getByRole('button', { name: '다음으로' }).click()
  await page.getByRole('button', { name: '송금하기' }).click()

  for (const digit of ['2', '2', '2', '2', '2', '2']) {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }

  await expect(
    page.getByRole('heading', { name: '이상 거래 알림' }),
  ).toBeVisible()
  await expect(page).toHaveURL(/\/ward\/transfer\/74\/held$/)
  await expect(page.getByText('잠깐 확인해 보세요!')).toBeVisible()
  await expect(page.getByText('50,000원')).toBeVisible()
  await expect(
    page.getByRole('navigation', { name: '시니어 주요 기능' }),
  ).toBeHidden()

  await page.getByRole('button', { name: '보호자에게 연락하기' }).click()
  await expect(
    page.getByRole('dialog', { name: '보호자에게 전화할까요?' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '취소', exact: true }).click()

  await expect(
    page.getByRole('button', { name: '홈에서 기다리기' }),
  ).toBeVisible()

  await expect(page.getByRole('button', { name: '거래 취소하기' })).toBeHidden()

  await page.reload()
  await expect(page).toHaveURL(/\/ward\/transfer\/74\/held$/)
  await expect(page.getByText('50,000원')).toBeVisible()

  await page.getByRole('button', { name: '뒤로 가기' }).click()
  await expect(page).toHaveURL(/\/ward$/)

  await page.getByRole('button', { name: '송금하기' }).click()
  await expect(page).toHaveURL(/\/ward\/transfer$/)
  await expect(
    page.getByRole('heading', { name: '누구에게 보낼까요?' }),
  ).toBeVisible()
})

test('승인 대기 거래를 유지하고 홈에서 기다린다', async ({ page }) => {
  await page.goto('/ward/transfer/74/held')

  await page.getByRole('button', { name: '홈에서 기다리기' }).click()

  await expect(page).toHaveURL(/\/ward$/)
  await expect(
    page.getByRole('button', {
      name: '송금 김민수 님에게 50,000원 상세 확인',
    }),
  ).toBeVisible()
})

test('보호자 전화 확인 후 연결된 번호로 전화를 건다', async ({ page }) => {
  await page.addInitScript(() => {
    window.open = (url) => {
      window.sessionStorage.setItem('last-opened-url', String(url))
      return null
    }
  })
  await page.goto('/ward/transfer/75/rejected')

  await page.getByRole('button', { name: '보호자에게 연락하기' }).click()
  const dialog = page.getByRole('dialog', {
    name: '보호자에게 전화할까요?',
  })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '전화 걸기' }).click()

  await expect
    .poll(() =>
      page.evaluate(() => window.sessionStorage.getItem('last-opened-url')),
    )
    .toBe('tel:01012345678')
  await expect(dialog).toBeHidden()
})

test('승인 대기 거래에서 준비 중인 취소 기능을 노출하지 않는다', async ({
  page,
}) => {
  await page.goto('/ward/transfer/76/held')
  await expect(
    page.getByRole('heading', { name: '이상 거래 알림' }),
  ).toBeVisible()
  await expect(page.getByText('30,000원')).toBeVisible()
  await expect(page.getByRole('button', { name: '거래 취소하기' })).toBeHidden()
  await expect(
    page.getByRole('button', { name: '홈에서 기다리기' }),
  ).toBeVisible()
})

test('거래 번호로 최종 상태 화면을 새로고침해도 복구한다', async ({ page }) => {
  await page.goto('/ward/transfer/73/complete')
  await expect(page.getByRole('heading', { name: '송금 완료' })).toBeVisible()
  await expect(page.getByText('송금 후 잔액')).toBeVisible()
  await expect(page.getByText('1,200,000원')).toBeVisible()
  await page.reload()
  await expect(page).toHaveURL(/\/ward\/transfer\/73\/complete$/)
  await expect(page.getByText('김민수')).toBeVisible()

  await page.goto('/ward/transfer/75/rejected')
  await expect(
    page.getByRole('heading', { name: '거래 거절 안내' }),
  ).toBeVisible()
  await page.reload()
  await expect(page).toHaveURL(/\/ward\/transfer\/75\/rejected$/)
  await expect(page.getByText('위험한 거래로 추정됩니다')).toBeVisible()
})

test('만료·취소된 송금에서 다시 송금할 수 있다', async ({ page }) => {
  await page.goto('/ward/transfer/77/expired')
  await expect(page.getByRole('heading', { name: '송금 만료' })).toBeVisible()
  await expect(page.getByText('승인 시간 만료')).toBeVisible()
  await page.getByRole('button', { name: '송금 다시하기' }).click()
  await expect(page).toHaveURL(/\/ward\/transfer$/)

  await page.goto('/ward/transfer/79/canceled')
  await expect(page.getByRole('heading', { name: '송금 취소' })).toBeVisible()
  await expect(page.getByText('요청한 송금이 취소되었습니다.')).toBeVisible()
  await expect(
    page.getByRole('button', { name: '송금 다시하기' }),
  ).toBeVisible()
})

test('잔액 부족으로 실패한 송금에서 충전 화면으로 이동한다', async ({
  page,
}) => {
  await page.goto('/ward/transfer/78/failed')
  await expect(page.getByRole('heading', { name: '송금 실패' })).toBeVisible()
  await expect(page.getByText('송금 가능한 잔액이 부족합니다.')).toBeVisible()
  await page.getByRole('button', { name: '충전하기' }).click()
  await expect(page).toHaveURL(/\/ward\/charge$/)
})

test('거래 번호로 승인 대기 화면을 새로고침해도 복구한다', async ({ page }) => {
  await page.goto('/ward/transfer/74/held')
  await expect(
    page.getByRole('heading', { name: '이상 거래 알림' }),
  ).toBeVisible()
  await page.reload()
  await expect(page).toHaveURL(/\/ward\/transfer\/74\/held$/)
  await expect(page.getByText('잠깐 확인해 보세요!')).toBeVisible()

  await page.goto('/ward/transfer/74/restricted')
  await expect(
    page.getByRole('heading', { name: '거래 제한 안내' }),
  ).toBeVisible()
  await expect(page.getByText('대기 중인 거래')).toBeVisible()
})

test('유효한 처리 상태 없이 송금 라우트에 직접 접근할 수 없다', async ({
  page,
}) => {
  await page.goto('/ward/transfer/bank')
  await expect(page).toHaveURL(/\/ward\/transfer\/account$/)

  await page.goto('/ward/transfer/processing')
  await expect(page).toHaveURL(/\/ward\/transfer$/)

  await page.goto('/ward/transfer/999/complete')
  await expect(page).toHaveURL(/\/ward\/transfer$/)

  await page.goto('/ward/transfer/not-a-number/held')
  await expect(page).toHaveURL(/\/ward\/transfer$/)
})
