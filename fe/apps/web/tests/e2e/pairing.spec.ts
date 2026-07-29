import { expect, test } from '@playwright/test'

test('보호자가 직접 접속해 인증 코드를 생성하고 복사한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.goto('/ward/home')
  await page.evaluate(() => {
    globalThis.history.pushState({}, '', '/guardian/pairing/code')
    globalThis.dispatchEvent(new PopStateEvent('popstate'))
  })
  await expect(page).toHaveURL(/\/guardian\/pairing\/code$/)

  const dialog = page.getByRole('dialog', {
    name: '인증 코드를 생성할까요?',
  })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '완료' }).click()

  await expect(page.getByText('72941')).toBeVisible()
  await page.getByRole('button', { name: '코드 복사' }).click()
  await expect(page.getByRole('status')).toContainText('코드가 복사되었습니다')

  await page.getByRole('button', { name: '뒤로 가기' }).click()
  await expect(page).toHaveURL((url) => url.pathname === '/ward/home')
  await page.goForward()

  await expect(page).toHaveURL(/\/guardian\/pairing\/code$/)
  await expect(page.getByText('72941')).toBeVisible()
  await expect(page.getByText('0:00')).toBeHidden()
})

test('미연결 시니어가 보호자 코드를 입력하고 연결을 완료한다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 854 })
  await page.goto('/ward/home?pairing=unpaired')

  await page.getByRole('button', { name: '보호자와 연결하기' }).click()
  await expect(page).toHaveURL(/\/ward\/pairing$/)

  await page.getByRole('button', { name: '인증 코드 0자리 입력됨' }).click()
  await expect(
    page.getByRole('dialog', { name: '인증 코드 입력' }),
  ).toBeVisible()

  for (const digit of ['7', '2', '9', '4', '1']) {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }

  await page.getByRole('button', { name: '입력 완료' }).click()
  await page.getByRole('button', { name: '연결하기' }).click()
  await expect(page).toHaveURL(/\/ward\/pairing\/complete$/)
  await expect(page.getByRole('heading', { name: '연결 성공!' })).toBeVisible()
  await expect(page.getByText('김철수 보호자님')).toBeVisible()

  await page.getByRole('button', { name: '홈으로' }).click()
  await expect(page).toHaveURL(/\/ward\/home$/)
  await expect(
    page.getByRole('button', { name: '보호자와 연결하기' }),
  ).toBeHidden()
})
