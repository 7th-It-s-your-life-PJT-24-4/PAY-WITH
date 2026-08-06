import { expect, test } from './fixtures'

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
  await page.getByLabel('계좌 비밀번호').fill('1234')

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
