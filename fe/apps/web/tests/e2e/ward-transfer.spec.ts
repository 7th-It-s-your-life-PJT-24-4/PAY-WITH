import { expect, test } from '@playwright/test'

test('모달이 열려도 고정 헤더와 하단 내비게이션 위치를 유지한다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 1024, height: 844 })
  await page.goto('/ward/transfer')

  const header = page.locator('header')
  const main = page.locator('main')
  const navigation = page.locator('nav[aria-label="시니어 주요 기능"]')
  const headerBefore = await header.boundingBox()
  const mainBefore = await main.boundingBox()
  const navigationBefore = await navigation.boundingBox()

  await page.getByRole('button', { name: '박지연 연락처 추가' }).click()
  await expect(page.getByRole('dialog', { name: '연락처 추가' })).toBeVisible()

  const headerAfter = await header.boundingBox()
  const mainAfter = await main.boundingBox()
  const navigationAfter = await navigation.boundingBox()
  expect(headerAfter?.x).toBeCloseTo(headerBefore?.x ?? 0, 1)
  expect(mainAfter?.x).toBeCloseTo(mainBefore?.x ?? 0, 1)
  expect(navigationAfter?.x).toBeCloseTo(navigationBefore?.x ?? 0, 1)
})

test('계좌번호로 은행을 찾고 계좌를 확인한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/transfer/account')

  for (const digit of '12345678') {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }
  await page.getByRole('button', { name: '다음으로' }).click()

  await expect(
    page.getByRole('dialog', { name: '은행을 찾고 있습니다' }),
  ).toBeVisible()
  await expect(page).toHaveURL(/\/ward\/transfer\/bank$/)

  await page.getByRole('button', { name: /하나은행/ }).click()
  await page.getByRole('button', { name: '다음으로' }).click()

  await expect(
    page.getByRole('dialog', { name: '계좌를 확인하고 있습니다' }),
  ).toBeVisible()
  await expect(page).toHaveURL(/\/ward\/transfer\/amount$/)
  await expect(page.getByText('김준호')).toBeVisible()
})

test('최근 수취인을 별칭과 함께 연락처에 추가한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/transfer')

  const addContactButton = page.getByRole('button', {
    name: '박지연 연락처 추가',
  })
  await expect(addContactButton).toContainText('연락처 추가')
  const buttonBox = await addContactButton.boundingBox()
  expect(buttonBox?.height).toBeGreaterThanOrEqual(48)
  await addContactButton.click()

  const dialog = page.getByRole('dialog', { name: '연락처 추가' })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByText('신한은행 110-234-567890')).toBeVisible()

  await dialog.getByLabel('연락처 별칭').fill('지연 이모')
  await dialog.getByRole('button', { name: '추가하기' }).click()

  await expect(dialog).toBeHidden()
  await expect(page.getByText('지연 이모')).toBeVisible()
  await expect(
    page.getByRole('button', { name: '박지연 연락처 추가' }),
  ).toBeHidden()
})

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

  await expect(page.getByText('안전하게 송금하고 있습니다')).toBeVisible()
  await expect(page.getByRole('heading', { name: '송금 완료' })).toBeVisible()
  await expect(page).toHaveURL(/\/ward\/transfer\/73\/complete$/)
  await expect(page.getByText('김민수')).toBeVisible()

  await page.goBack()
  await expect(page).toHaveURL(/\/ward\/home$/)
  await expect(page.getByRole('heading', { name: 'PayWith' })).toBeVisible()
  await expect(page.getByText('안전하게 송금하고 있습니다')).toBeHidden()

  await page.goBack()
  await expect(page).toHaveURL(/\/ward\/transfer$/)
})

test('이상 거래 승인 대기와 재송금 제한 화면을 표시한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/transfer')

  await page
    .getByRole('button', { name: /김민수/ })
    .first()
    .click()
  await page.getByRole('button', { name: '+5만원', exact: true }).click()
  await page.getByRole('button', { name: '다음으로' }).click()
  await page.getByRole('button', { name: '송금하기' }).click()

  for (const digit of ['2', '2', '2', '2', '2', '2']) {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }

  await expect(
    page.getByRole('heading', { name: '이상 거래 알림' }),
  ).toBeVisible()
  await expect(page).toHaveURL(/\/ward\/transfer\/74\/held$/)
  await expect(page.getByText('잠깐 확인해 보세요!')).toBeVisible()
  await expect(page.getByText('50,000원')).toBeVisible()
  await expect(
    page.getByRole('navigation', { name: '시니어 주요 기능' }),
  ).toBeHidden()

  await page.getByRole('button', { name: '뒤로 가기' }).click()
  await expect(page).toHaveURL(/\/ward\/home$/)

  await page.getByRole('button', { name: '송금하기' }).click()
  await expect(page).toHaveURL(/\/ward\/transfer\/74\/restricted$/)
  await expect(
    page.getByRole('heading', { name: '거래 제한 안내' }),
  ).toBeVisible()
  await expect(page.getByText('거래를 진행할 수 없습니다')).toBeVisible()

  await page.getByRole('button', { name: '홈으로', exact: true }).click()
  await expect(page).toHaveURL(/\/ward\/home$/)
})

test('거래 번호로 최종 상태 화면을 새로고침해도 복구한다', async ({ page }) => {
  await page.goto('/ward/transfer/73/complete')
  await expect(page.getByRole('heading', { name: '송금 완료' })).toBeVisible()
  await page.reload()
  await expect(page).toHaveURL(/\/ward\/transfer\/73\/complete$/)
  await expect(page.getByText('김민수')).toBeVisible()

  await page.goto('/ward/transfer/75/rejected')
  await expect(
    page.getByRole('heading', { name: '거래 거절 안내' }),
  ).toBeVisible()
  await page.reload()
  await expect(page).toHaveURL(/\/ward\/transfer\/75\/rejected$/)
  await expect(page.getByText('위험한 거래로 추정됩니다')).toBeVisible()
})

test('거래 번호로 승인 대기 화면을 새로고침해도 복구한다', async ({ page }) => {
  await page.goto('/ward/transfer/74/held')
  await expect(
    page.getByRole('heading', { name: '이상 거래 알림' }),
  ).toBeVisible()
  await page.reload()
  await expect(page).toHaveURL(/\/ward\/transfer\/74\/held$/)
  await expect(page.getByText('잠깐 확인해 보세요!')).toBeVisible()

  await page.goto('/ward/transfer/74/restricted')
  await expect(
    page.getByRole('heading', { name: '거래 제한 안내' }),
  ).toBeVisible()
  await expect(page.getByText('대기 중인 거래')).toBeVisible()
})

test('유효한 처리 상태 없이 송금 라우트에 직접 접근할 수 없다', async ({
  page,
}) => {
  await page.goto('/ward/transfer/bank')
  await expect(page).toHaveURL(/\/ward\/transfer\/account$/)

  await page.goto('/ward/transfer/processing')
  await expect(page).toHaveURL(/\/ward\/transfer$/)

  await page.goto('/ward/transfer/999/complete')
  await expect(page).toHaveURL(/\/ward\/transfer$/)

  await page.goto('/ward/transfer/not-a-number/held')
  await expect(page).toHaveURL(/\/ward\/transfer$/)
})
