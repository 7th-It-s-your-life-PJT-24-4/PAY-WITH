import { expect, test } from '@playwright/test'

test('completes the mock ward QR payment flow', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/home')

  await page.getByRole('button', { name: '결제하기' }).click()
  await expect(page.getByRole('heading', { name: 'QR 결제' })).toBeVisible()
  await expect(page.getByText('결제 가능 잔액')).toBeVisible()

  await page.getByRole('button', { name: 'QR 코드 스캔 완료' }).click()
  await expect(
    page.getByRole('heading', { name: '비밀번호 입력' }),
  ).toBeVisible()
  await expect(
    page.getByRole('navigation', { name: '시니어 주요 기능' }),
  ).toBeHidden()

  for (const digit of ['1', '2', '3', '4', '5', '6']) {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }

  await expect(page.getByText('결제가 완료되었습니다')).toBeVisible()
  await expect(page.getByText('스타벅스 강남점')).toBeVisible()

  await page.getByRole('button', { name: '홈으로' }).click()
  await expect(page).toHaveURL(/\/ward\/home$/)
})
