import { expect, test, type Page } from './fixtures'

async function mockChargeApi(
  page: Page,
  options: { insufficientBalance?: boolean } = {},
) {
  const accounts = [
    {
      accountId: 7,
      bankCode: '004',
      bankName: 'KB국민은행',
      accountNo: '12345612123456',
    },
    {
      accountId: 9,
      bankCode: '011',
      bankName: 'NH농협은행',
      accountNo: '2345634234567',
    },
  ]

  await page.route('**/api/accounts', async (route) => {
    if (route.request().method() === 'POST') {
      const request = route.request().postDataJSON() as {
        bankCode: string
        accountNo: string
      }
      const account = {
        accountId: 10,
        bankCode: request.bankCode,
        bankName: '우리은행',
        accountNo: request.accountNo,
      }
      accounts.unshift(account)
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        json: { success: true, data: account, message: null },
      })
      return
    }

    await route.fulfill({
      contentType: 'application/json',
      json: { success: true, data: accounts, message: null },
    })
  })

  await page.route('**/api/ward/charges', async (route) => {
    if (options.insufficientBalance) {
      await route.fulfill({
        status: 422,
        contentType: 'application/json',
        json: {
          success: false,
          data: null,
          code: 'ACCOUNT_002',
          message: '출금 계좌의 잔액이 부족합니다.',
        },
      })
      return
    }

    const request = route.request().postDataJSON() as {
      accountId: number
      amount: number
    }
    const account = accounts.find(
      ({ accountId }) => accountId === request.accountId,
    )!
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          transactionId: 43,
          chargeAmount: request.amount,
          balanceAfter: 100_000 + request.amount,
          bankName: account.bankName,
          accountNo: account.accountNo,
          createdAt: '2026-07-23T17:30:00',
        },
        message: null,
      },
    })
  })
}

test('최근 사용 계좌를 선택해 지갑 충전을 완료한다', async ({ page }) => {
  await mockChargeApi(page)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward')

  await page.getByRole('button', { name: '충전하기' }).click()
  await expect(page.getByRole('heading', { name: '충전하기' })).toBeVisible()
  await expect(page.getByText('KB국민은행')).toBeVisible()

  await page.getByRole('button', { name: '+5만', exact: true }).click()
  await page.getByRole('button', { name: '충전하기' }).click()

  await expect(page.getByRole('heading', { name: '충전 완료' })).toBeVisible()
  await expect(page.getByText('50,000원', { exact: true })).toBeVisible()
  await expect(page.getByText('150,000원', { exact: true })).toBeVisible()

  await page.reload()
  await expect(page.getByRole('heading', { name: '충전 완료' })).toBeVisible()
  await expect(page.getByText('50,000원', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: '홈으로' }).click()
  await expect(page).toHaveURL(/\/ward$/)
})

test('새 계좌를 등록하고 충전 계좌로 사용한다', async ({ page }) => {
  await mockChargeApi(page)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/charge')

  await page.getByRole('button', { name: /KB국민은행/ }).click()
  await page.getByRole('button', { name: '새 계좌 추가' }).click()
  await page.getByRole('combobox', { name: '은행 선택' }).click()
  await expect(
    page.getByText('목록을 위로 밀어 더 많은 은행 보기'),
  ).toBeVisible()
  await page.getByRole('option', { name: '하나은행' }).scrollIntoViewIfNeeded()
  await expect(
    page.getByText('목록을 위로 밀어 더 많은 은행 보기'),
  ).toBeHidden()
  await page.getByRole('option', { name: '우리은행' }).click()

  await page.getByLabel('계좌번호').click()
  const accountSheet = page.getByRole('dialog', { name: '계좌번호 입력' })
  await expect(accountSheet).toBeVisible()
  for (const digit of '1002123456789') {
    await accountSheet.getByRole('button', { name: digit, exact: true }).click()
  }
  await accountSheet.getByRole('button', { name: '입력 완료' }).click()

  await page.getByRole('textbox', { name: '계좌 비밀번호' }).click()
  const passwordSheet = page.getByRole('dialog', {
    name: '계좌 비밀번호 입력',
  })
  await expect(passwordSheet).toBeVisible()

  for (const digit of ['1', '0', '0', '4']) {
    await passwordSheet
      .getByRole('button', { name: digit, exact: true })
      .click()
  }
  await expect(passwordSheet).toBeHidden()
  const accountRequestPromise = page.waitForRequest(
    (request) =>
      request.url().endsWith('/api/accounts') && request.method() === 'POST',
  )
  await page.getByRole('button', { name: '계좌 등록하기' }).click()

  const accountRequest = await accountRequestPromise
  expect(accountRequest.postDataJSON()).toMatchObject({ bankCode: '020' })

  await expect(
    page.getByRole('heading', { name: '계좌 추가 완료' }),
  ).toBeVisible()
  await expect(page.getByText('우리은행')).toBeVisible()
  await page.getByRole('button', { name: '확인' }).click()

  await expect(page).toHaveURL(/\/ward\/charge$/)
  await expect(page.getByText('우리은행')).toBeVisible()
})

test('출금 계좌 잔액 부족 오류를 안내한다', async ({ page }) => {
  await mockChargeApi(page, { insufficientBalance: true })
  await page.goto('/ward/charge')

  await page.getByRole('button', { name: '+5만', exact: true }).click()
  await page.getByRole('button', { name: '충전하기' }).click()

  await expect(page.getByRole('alert')).toContainText(
    '출금 계좌의 잔액이 부족합니다.',
  )
  await expect(page).toHaveURL(/\/ward\/charge$/)
  await expect(page.getByRole('button', { name: '충전하기' })).toBeEnabled()
})
