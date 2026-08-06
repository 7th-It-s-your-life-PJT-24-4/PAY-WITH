import { expect, test } from './fixtures'

test('보호자가 안전계좌 정보를 확인하고 추가를 완료한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })
  let safeAccounts: Array<Record<string, unknown>> = []

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
  await page.route('**/api/guard/wards/1/safe-accounts', async (route) => {
    if (route.request().method() === 'POST') {
      const registered = {
        safeAccountId: 10,
        recipientId: null,
        bankCode: '004',
        bankName: 'KB국민은행',
        accountNo: '11012300006781',
        holderName: '안유진',
        accountAlias: null,
        isVerified: true,
        status: 'ACTIVE',
        createdAt: '2026-08-06T10:00:00',
      }
      safeAccounts = [registered]
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        json: { success: true, data: registered, message: null },
      })
      return
    }

    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: { safeAccounts },
        message: null,
      },
    })
  })

  await page.goto('/guard/safe-account?wardId=1')

  await expect(
    page.getByRole('heading', { name: '안전계좌 목록' }),
  ).toBeVisible()
  await expect(page.getByText('등록된 안전계좌가 없어요')).toBeVisible()

  await page.getByRole('button', { name: '안전계좌 추가' }).click()
  await expect(page).toHaveURL(/\/guard\/safe-account\/add\?wardId=1$/)

  await page
    .getByRole('button', { name: '은행/증권사를 선택해 주세요' })
    .click()
  await page.getByRole('button', { name: 'KB국민은행' }).click()
  await page.getByLabel('계좌번호').fill('11012300006781')

  await page.getByRole('button', { name: '안전계좌 추가하기' }).click()
  await expect(page).toHaveURL(/\/guard\/safe-account\/confirm\?wardId=1$/)
  await expect(
    page.getByRole('heading', {
      name: 'KB국민은행 계좌를 안전계좌에 추가할까요?',
    }),
  ).toBeVisible()

  await page.getByRole('button', { name: '안전계좌 추가하기' }).click()
  await expect(page).toHaveURL(/\/guard\/safe-account\?wardId=1$/)
  await expect(page.getByText('안유진')).toBeVisible()
  await expect(page.getByText('KB국민은행 · •••• 6781')).toBeVisible()
})
