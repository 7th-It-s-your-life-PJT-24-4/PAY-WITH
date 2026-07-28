import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/history')
})

test('결제 내역을 검색하고 차단된 거래 상세를 확인한다', async ({ page }) => {
  await expect(page.getByRole('heading', { name: '거래 내역' })).toBeVisible()

  await page
    .getByRole('group', { name: '거래 종류' })
    .getByRole('button', { name: '결제', exact: true })
    .click()
  await expect(page.getByText('김철수', { exact: true })).toBeHidden()

  await page.getByRole('searchbox', { name: '거래 내역 검색' }).fill('제주')
  await page.getByRole('link', { name: /CU 제주공항점/ }).click()

  await expect(page).toHaveURL(/\/ward\/history\/104$/)
  await expect(page.getByText('거래 차단됨', { exact: true })).toBeVisible()
  await expect(page.getByText('거래 후 잔액', { exact: true })).toBeVisible()
  await expect(page.getByText('1,383,000원', { exact: true })).toBeVisible()
  await expect(
    page.getByText('위험한 거래로 판단되어 보호자가 거래를 차단했습니다.'),
  ).toBeVisible()
  await expect(page.getByText('거절된 거래입니다.')).toBeVisible()

  const riskSection = page.getByRole('region', { name: '거래 안전 확인' })
  await expect(riskSection).toHaveClass(/border-error/)

  await page.getByRole('button', { name: '뒤로 가기' }).click()
  await expect(page).toHaveURL(/\/ward\/history$/)
})

test('보호자가 승인한 위험 거래는 위험으로 표시한다', async ({ page }) => {
  await page.goto('/ward/history/106')

  await expect(page.getByText('위험', { exact: true })).toBeVisible()
  await expect(
    page.getByText('위험한 거래로 판단되었지만 보호자가 승인했습니다.'),
  ).toBeVisible()
  await expect(page.getByText('거절된 거래입니다.')).toBeHidden()
})

test('받은 송금 내역에서 은행과 계좌번호를 확인한다', async ({ page }) => {
  await page.getByRole('button', { name: '받은 돈' }).click()
  await page.getByRole('link', { name: /김철수/ }).click()

  await expect(page.getByText('받은 돈', { exact: true })).toBeVisible()
  await expect(page.getByText('우리은행 1002123456789')).toBeVisible()
  await expect(
    page.getByRole('region', { name: '거래 안전 확인' }),
  ).toBeHidden()

  await page.getByRole('button', { name: '목록으로 돌아가기' }).click()
  await expect(page).toHaveURL(/\/ward\/history$/)
})

test('잘못된 거래 번호는 거래 내역 목록으로 이동한다', async ({ page }) => {
  await page.goto('/ward/history/9999')
  await expect(page).toHaveURL(/\/ward\/history$/)
})
