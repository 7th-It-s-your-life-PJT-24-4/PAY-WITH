import { expect, test } from '@playwright/test'

test('최근 사용 계좌를 선택해 지갑 충전을 완료한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/home')

  await page.getByRole('button', { name: '충전하기' }).click()
  await expect(page.getByRole('heading', { name: '충전하기' })).toBeVisible()
  await expect(page.getByText('KB국민은행')).toBeVisible()

  await page.getByRole('button', { name: '+5만', exact: true }).click()
  await page.getByRole('button', { name: '충전하기' }).click()

  await expect(page.getByRole('heading', { name: '충전 완료' })).toBeVisible()
  await expect(page.getByText('50,000원')).toBeVisible()
  await expect(page.getByText('150,000원')).toBeVisible()

  await page.getByRole('button', { name: '홈으로' }).click()
  await expect(page).toHaveURL(/\/ward\/home$/)
})

test('새 계좌를 등록하고 충전 계좌로 사용한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/charge')

  await page.getByRole('button', { name: /KB국민은행/ }).click()
  await page.getByRole('button', { name: '새 계좌 추가' }).click()
  await page.getByLabel('은행 선택').selectOption('WOORI')
  await page.getByLabel('계좌번호').fill('1002123456789')

  for (const digit of ['1', '0', '0', '4']) {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }
  await page.getByRole('button', { name: '계좌 등록하기' }).click()

  await expect(
    page.getByRole('heading', { name: '계좌 추가 완료' }),
  ).toBeVisible()
  await expect(page.getByText('우리은행')).toBeVisible()
  await page.getByRole('button', { name: '확인' }).click()

  await expect(page).toHaveURL(/\/ward\/charge$/)
  await expect(page.getByText('우리은행')).toBeVisible()
})
