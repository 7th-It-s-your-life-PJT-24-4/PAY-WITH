import { expect, test } from '@playwright/test'

test('renders store interactions and queried users', async ({ page }) => {
  await page.route('**/users', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: [
        {
          id: 1,
          name: 'Grace Hopper',
          email: 'grace@example.com',
        },
      ],
    })
  })

  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Count: 0' })).toBeVisible()
  await page.getByRole('button', { name: 'Increment' }).click()
  await expect(page.getByRole('heading', { name: 'Count: 1' })).toBeVisible()

  await expect(page.getByText('Grace Hopper')).toBeVisible()
  await expect(page.getByText('grace@example.com')).toBeVisible()
})
