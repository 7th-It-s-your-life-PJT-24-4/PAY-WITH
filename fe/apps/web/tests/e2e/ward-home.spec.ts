import { expect, test } from '@playwright/test'

test('keeps the ward header and navigation fixed to the viewport', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward')

  const navigation = page.getByRole('navigation', {
    name: '시니어 주요 기능',
  })
  const header = page.getByRole('banner')
  const paymentAction = navigation.getByRole('button', {
    name: '결제',
    exact: true,
  })
  const paymentLabel = paymentAction.getByText('결제', { exact: true })
  const paymentBackground = paymentAction.locator('svg:not(.lucide) path')
  const homeAction = navigation.getByRole('button', {
    name: '홈',
    exact: true,
  })

  await expect(header).toBeVisible()
  await expect(navigation).toBeVisible()
  await expect(navigation.locator('svg.lucide')).toHaveCount(3)
  await expect(homeAction).toBeVisible()
  await expect(paymentLabel).toHaveCSS('color', 'rgb(0, 0, 0)')
  await expect(paymentBackground).toHaveCSS('fill', 'rgb(255, 255, 255)')
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

  const headerBox = await header.boundingBox()
  const navigationBox = await navigation.boundingBox()
  const homeActionBox = await homeAction.boundingBox()

  expect(headerBox).not.toBeNull()
  expect(Math.abs(headerBox?.y ?? 0)).toBeLessThanOrEqual(1)
  expect(navigationBox).not.toBeNull()
  expect(homeActionBox).not.toBeNull()
  expect(homeActionBox?.y ?? 0).toBeLessThan(navigationBox?.y ?? 0)
  expect(homeActionBox?.x ?? -1).toBeGreaterThanOrEqual(0)
  expect(
    (homeActionBox?.x ?? 0) + (homeActionBox?.width ?? 0),
  ).toBeLessThanOrEqual(390)
  expect(
    Math.abs((navigationBox?.y ?? 0) + (navigationBox?.height ?? 0) - 844),
  ).toBeLessThanOrEqual(1)

  await navigation.getByRole('button', { name: '송금', exact: true }).click()
  await expect(page).toHaveURL(/\/ward\/transfer$/)
  await expect(paymentLabel).toHaveCSS('color', 'rgb(0, 0, 0)')
})

test('보호자 승인 대기 거래를 상세 화면에서 확인한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward')

  const pendingSection = page.getByRole('region', {
    name: '보호자 승인을 기다리고 있어요',
  })
  await expect(pendingSection).toBeVisible()
  await expect(
    pendingSection.getByText('확인이 필요한 거래 2건이 있습니다.'),
  ).toBeVisible()

  const pendingPayment = pendingSection.getByRole('button', {
    name: '결제 우리동네마트 32,000원 상세 확인',
  })
  await expect(pendingPayment.getByText('32,000원')).toBeVisible()
  await pendingPayment.click()

  await expect(page).toHaveURL(/\/ward\/payment\/held\/81$/)
  await expect(
    page.getByRole('heading', { name: '결제 승인을 기다리고 있어요' }),
  ).toBeVisible()

  await page.getByRole('button', { name: '보호자에게 연락하기' }).click()
  await expect(
    page.getByRole('dialog', { name: '보호자에게 전화할까요?' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '취소', exact: true }).click()

  await page.getByRole('button', { name: '결제 취소하기' }).click()
  const cancelDialog = page.getByRole('dialog', {
    name: '대기 중인 결제를 취소할까요?',
  })
  await expect(cancelDialog).toBeVisible()
  await cancelDialog.getByRole('button', { name: '결제 취소하기' }).click()

  await expect(page).toHaveURL(/\/ward$/)
  await expect(
    page.getByRole('button', {
      name: '결제 우리동네마트 32,000원 상세 확인',
    }),
  ).toBeHidden()
})
