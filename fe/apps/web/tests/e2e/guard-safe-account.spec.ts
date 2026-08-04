import { expect, test } from '@playwright/test'

test('보호자가 안전계좌 정보를 확인하고 추가를 완료한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.goto('/guard/safe-account')

  await expect(
    page.getByRole('heading', { name: '안전계좌 추가' }),
  ).toBeVisible()
  await expect(page.getByLabel('계좌번호')).toHaveValue('933')

  await page.getByRole('button', { name: '안전계좌 추가하기' }).click()
  await expect(page).toHaveURL(/\/guard\/safe-account\/confirm$/)
  await expect(
    page.getByRole('heading', { name: '안유진님을 안전계좌에 추가할까요?' }),
  ).toBeVisible()

  await page.getByRole('button', { name: '안전계좌 추가하기' }).click()
  await expect(page).toHaveURL(/\/guard$/)
})
