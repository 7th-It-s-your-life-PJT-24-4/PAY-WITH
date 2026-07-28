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
  await expect(page.getByText('50,000원', { exact: true })).toBeVisible()
  await expect(page.getByText('150,000원', { exact: true })).toBeVisible()

  await page.getByRole('button', { name: '홈으로' }).click()
  await expect(page).toHaveURL(/\/ward\/home$/)
})

test('새 계좌를 등록하고 충전 계좌로 사용한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/charge')

  await page.getByRole('button', { name: /KB국민은행/ }).click()
  await page.getByRole('button', { name: '새 계좌 추가' }).click()
  await page.getByRole('combobox', { name: '은행 선택' }).click()
  await expect(
    page.getByText('목록을 위로 밀어 더 많은 은행 보기'),
  ).toBeVisible()
  await page.getByRole('option', { name: '하나은행' }).scrollIntoViewIfNeeded()
  await expect(
    page.getByText('목록을 위로 밀어 더 많은 은행 보기'),
  ).toBeHidden()
  await page.getByRole('option', { name: '우리은행' }).click()

  await page.getByLabel('계좌번호').click()
  const accountSheet = page.getByRole('dialog', { name: '계좌번호 입력' })
  await expect(accountSheet).toBeVisible()
  for (const digit of '1002123456789') {
    await accountSheet.getByRole('button', { name: digit, exact: true }).click()
  }
  await accountSheet.getByRole('button', { name: '입력 완료' }).click()

  await page.getByRole('textbox', { name: '계좌 비밀번호' }).click()
  const passwordSheet = page.getByRole('dialog', {
    name: '계좌 비밀번호 입력',
  })
  await expect(passwordSheet).toBeVisible()

  for (const digit of ['1', '0', '0', '4']) {
    await passwordSheet
      .getByRole('button', { name: digit, exact: true })
      .click()
  }
  await expect(passwordSheet).toBeHidden()
  await page.getByRole('button', { name: '계좌 등록하기' }).click()

  await expect(
    page.getByRole('heading', { name: '계좌 추가 완료' }),
  ).toBeVisible()
  await expect(page.getByText('우리은행')).toBeVisible()
  await page.getByRole('button', { name: '확인' }).click()

  await expect(page).toHaveURL(/\/ward\/charge$/)
  await expect(page.getByText('우리은행')).toBeVisible()
})
