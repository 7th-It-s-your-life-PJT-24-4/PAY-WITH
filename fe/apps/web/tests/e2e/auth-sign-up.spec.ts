import { expect, test } from '@playwright/test'

test('가입 유형 없이 상세 주소로 접근하면 첫 단계로 복구한다', async ({
  page,
}) => {
  await page.goto('/auth/sign-up/details')

  await expect(page).toHaveURL(/\/auth\/sign-up$/)
  await expect(
    page.getByRole('heading', { name: '어떤 유형으로 가입하시겠습니까?' }),
  ).toBeVisible()
})

test('가입 유형을 선택하면 상세 단계와 새로고침 상태를 유지한다', async ({
  page,
}) => {
  await page.goto('/auth/sign-up')
  await page.getByRole('radio', { name: /보호자/ }).click()
  await page.getByRole('button', { name: '다음' }).click()

  await expect(page).toHaveURL(/\/auth\/sign-up\/details$/)
  await expect(page.getByRole('heading', { name: '환영합니다' })).toBeVisible()

  await page.reload()

  await expect(page).toHaveURL(/\/auth\/sign-up\/details$/)
  await expect(page.getByText('2 / 2 단계')).toBeVisible()
})

test('상세 단계에서 휴대폰 번호를 인증한다', async ({ page }) => {
  await page.route('**/api/auth/phone/code', async (route) => {
    expect(route.request().postDataJSON()).toEqual({
      phone: '01012345678',
      purpose: 'SIGNUP',
    })
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: { expireIn: 180 },
        message: null,
      },
    })
  })
  await page.route('**/api/auth/phone/verify', async (route) => {
    expect(route.request().postDataJSON()).toEqual({
      phone: '01012345678',
      code: '123456',
    })
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: { verificationToken: 'verified-phone-token' },
        message: null,
      },
    })
  })

  await page.goto('/auth/sign-up')
  await page.getByRole('radio', { name: /보호자/ }).click()
  await page.getByRole('button', { name: '다음' }).click()

  await page.getByLabel('휴대폰 번호').fill('01012345678')
  await page.getByRole('button', { name: '인증하기' }).click()
  await expect(page.getByText('인증번호를 발송했어요.')).toBeVisible()

  await page.getByLabel('휴대폰 인증번호').fill('123456')
  await page.getByRole('button', { name: '확인', exact: true }).click()

  await expect(page.getByText('인증 완료했어요.')).toBeVisible()
  await expect(page.getByRole('button', { name: '인증 완료' })).toBeDisabled()
})
