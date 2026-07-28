import { expect, test } from '@playwright/test'

test('keeps the ward header and navigation fixed to the viewport', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/home')

  const navigation = page.getByRole('navigation', {
    name: '시니어 주요 기능',
  })
  const header = page.getByRole('banner')
  const paymentAction = navigation.getByRole('button', {
    name: '결제',
    exact: true,
  })

  await expect(header).toBeVisible()
  await expect(navigation).toBeVisible()
  await expect(navigation.locator('svg')).toHaveCount(3)
  await expect(paymentAction).toHaveCSS('color', 'rgb(255, 255, 255)')
  expect(
    await paymentAction.evaluate(
      (element) => getComputedStyle(element).backgroundImage,
    ),
  ).toContain('linear-gradient')
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

  const headerBox = await header.boundingBox()
  const navigationBox = await navigation.boundingBox()

  expect(headerBox).not.toBeNull()
  expect(Math.abs(headerBox?.y ?? 0)).toBeLessThanOrEqual(1)
  expect(navigationBox).not.toBeNull()
  expect(
    Math.abs((navigationBox?.y ?? 0) + (navigationBox?.height ?? 0) - 844),
  ).toBeLessThanOrEqual(1)

  await navigation.getByRole('button', { name: '송금', exact: true }).click()
  await expect(page).toHaveURL(/\/ward\/transfer$/)
  await expect(paymentAction).toHaveCSS('color', 'rgb(255, 255, 255)')
})

test('보호자 승인 대기 거래를 상세 화면에서 확인한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/home')

  const pendingSection = page.getByRole('region', {
    name: '보호자 승인을 기다리고 있어요',
  })
  await expect(pendingSection).toBeVisible()
  await expect(
    pendingSection.getByText('확인이 필요한 송금 2건이 있습니다.'),
  ).toBeVisible()

  const pendingTransfer = pendingSection.getByRole('button', {
    name: '박지연 님에게 30,000원 송금 상세 확인',
  })
  await expect(pendingTransfer.getByText('30,000원')).toBeVisible()
  await pendingTransfer.click()

  await expect(page).toHaveURL(/\/ward\/transfer\/76\/held$/)
  await expect(
    page.getByRole('heading', { name: '잠깐 확인해 보세요!' }),
  ).toBeVisible()
})
