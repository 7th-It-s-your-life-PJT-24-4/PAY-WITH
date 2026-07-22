import { expect, test } from '@playwright/test'

test('최근 수취인을 선택해 시니어 송금 플로우를 완료한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/home')

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

  await expect(page.getByRole('heading', { name: '송금 완료' })).toBeVisible()
  await expect(page.getByText('김민수')).toBeVisible()

  await page.getByRole('button', { name: '홈으로' }).click()
  await expect(page).toHaveURL(/\/ward\/home$/)
})
