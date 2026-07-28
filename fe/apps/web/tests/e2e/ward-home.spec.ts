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
