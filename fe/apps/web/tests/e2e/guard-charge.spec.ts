import { expect, test } from './fixtures'

test('연결된 시니어가 없으면 상단 시니어 선택 영역을 숨긴다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.route('**/api/guard', (route) =>
    route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: { wards: [], selectedWard: null },
        message: null,
      },
    }),
  )
  await page.route('**/api/guard/charges', (route) =>
    route.fulfill({
      contentType: 'application/json',
      json: { success: true, data: { charges: [] }, message: null },
    }),
  )

  await page.goto('/guard/charge')

  await expect(
    page.getByRole('heading', { name: '보호자 충전 내역', exact: true }),
  ).toBeVisible()
  await expect(page.getByRole('button', { name: '시니어 추가' })).toHaveCount(0)
})

test('등록 계좌가 없으면 계좌를 추가한 뒤 충전을 이어간다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 820 })
  let accounts: Array<Record<string, unknown>> = []

  await page.route('**/api/accounts', async (route) => {
    if (route.request().method() === 'POST') {
      const registeredAccount = {
        accountId: 7,
        bankCode: '004',
        bankName: 'KB국민은행',
        accountNo: '11012300006781',
      }
      accounts = [registeredAccount]
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        json: {
          success: true,
          data: registeredAccount,
          message: null,
        },
      })
      return
    }

    await route.fulfill({
      contentType: 'application/json',
      json: { success: true, data: accounts, message: null },
    })
  })

  await page.route('**/api/banks', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: [{ bankCode: '004', bankName: 'KB국민은행' }],
        message: null,
      },
    })
  })

  await page.goto('/guard/charge/be')

  await expect(page).toHaveURL(/\/guard\/charge\/account$/)
  await expect(
    page.getByRole('heading', { name: '충전계좌 연결' }),
  ).toBeVisible()

  await page.getByRole('button', { name: '은행/증권사를 선택해주세요' }).click()
  await page.getByRole('button', { name: /KB국민은행/ }).click()
  await page.getByLabel('계좌번호').fill('11012300006781')
  await page.getByLabel('계좌 비밀번호').click()

  const passwordSheet = page.getByRole('dialog', {
    name: '계좌 비밀번호 입력',
  })
  await expect(passwordSheet).toBeVisible()
  for (const digit of '1234') {
    await passwordSheet
      .getByRole('button', { name: digit, exact: true })
      .click()
  }
  await expect(passwordSheet).toBeHidden()

  const registerRequest = page.waitForRequest(
    (request) =>
      request.url().endsWith('/api/accounts') && request.method() === 'POST',
  )
  await page.getByRole('button', { name: '충전계좌 연결하기' }).click()

  expect((await registerRequest).postDataJSON()).toEqual({
    bankCode: '004',
    accountNo: '11012300006781',
    accountPassword: '1234',
  })
  await expect(page).toHaveURL(/\/guard\/charge\/be$/)
  await expect(
    page.getByRole('button', { name: '출금 계좌 선택' }),
  ).toBeVisible()
  await expect(page.getByText('KB국민은행 6781 에서')).toBeVisible()
})

test('보호자 충전 요청에 입력한 6자리 PIN을 포함한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })

  await page.route('**/api/accounts', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: [
          {
            accountId: 7,
            bankCode: '004',
            bankName: 'KB국민은행',
            accountNo: '11012300006781',
          },
        ],
        message: null,
      },
    })
  })

  await page.route('**/api/guard/wards/13/charges', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          transactionId: 44,
          chargeAmount: 50_000,
          balanceAfter: 200_000,
          bankName: 'KB국민은행',
          accountNo: '11012300006781',
          createdAt: '2026-08-07T10:30:00',
          wardId: 13,
          wardName: '김시니어',
        },
        message: null,
      },
    })
  })

  await page.goto('/guard/charge/be?wardId=13')
  await page.getByRole('button', { name: '+ 5만', exact: true }).click()
  await page.getByRole('button', { name: '충전하기' }).click()

  const chargeRequest = page.waitForRequest(
    (request) =>
      request.url().endsWith('/api/guard/wards/13/charges') &&
      request.method() === 'POST',
  )

  for (const digit of '123456') {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }

  expect((await chargeRequest).postDataJSON()).toEqual({
    accountId: 7,
    amount: 50_000,
    pin: '123456',
  })
  await expect(page).toHaveURL(/\/guard\/charge\/complete\?wardId=13$/)
})
