import { expect, test } from './fixtures'

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
})

test('보호자가 이상 거래를 승인하고 승인 상태 상세를 확인한다', async ({
  page,
}) => {
  await page.goto('/guard/history/tx-6')

  await page.getByRole('button', { name: '승인하기' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: '승인', exact: true }).click()

  await expect(
    page.getByRole('heading', { name: '이상 거래를 승인했어요' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '확인' }).click()

  await expect(page).toHaveURL(/\/guard\/history\/tx-6\?decision=approved$/)
  await expect(
    page.getByRole('heading', { name: '승인된 이상 거래에요' }),
  ).toBeVisible()
})

test('보호자가 이상 거래를 거절하고 거절 상태 상세를 확인한다', async ({
  page,
}) => {
  await page.goto('/guard/history/tx-6')

  await page.getByRole('button', { name: '거절하기' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: '거절', exact: true }).click()

  await expect(
    page.getByRole('heading', { name: '이상 거래를 거절했어요' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '확인' }).click()

  await expect(page).toHaveURL(/\/guard\/history\/tx-6\?decision=rejected$/)
  await expect(
    page.getByRole('heading', { name: '거절된 이상 거래에요' }),
  ).toBeVisible()
})
