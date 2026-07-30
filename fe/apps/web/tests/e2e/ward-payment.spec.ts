import { expect, test } from '@playwright/test'

test('비밀번호 확인 후 QR을 만들고 결제 완료 상태로 이동한다', async ({
  page,
}) => {
  let statusRequestCount = 0

  await page.route('**/api/ward/payments', async (route) => {
    if (route.request().method() !== 'POST') return route.fallback()

    expect(route.request().postDataJSON()).toEqual({
      pin: '123456',
    })
    await route.fulfill({
      contentType: 'application/json',
      status: 201,
      json: {
        success: true,
        data: {
          paymentId: 42,
          qrToken: 'pay_qr_test_token',
          availableBalance: 130_000,
          expiresAt: new Date(Date.now() + 60_000).toISOString(),
          expiresInSeconds: 60,
        },
        message: null,
      },
    })
  })

  await page.route('**/api/ward/payments/42', async (route) => {
    statusRequestCount += 1
    const completed = statusRequestCount >= 3
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          paymentId: 42,
          transactionId: completed ? 73 : null,
          status: completed ? 'COMPLETED' : 'PENDING',
          merchantName: completed ? '스타벅스 강남점' : null,
          amount: completed ? 5_500 : null,
          paidAt: completed ? '2026-07-29T14:45:00+09:00' : null,
          remainingBalance: completed ? 124_500 : null,
          failureCode: null,
          failureMessage: null,
          expiresAt: new Date(Date.now() + 60_000).toISOString(),
        },
        message: null,
      },
    })
  })

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/ward/home')
  await page.getByRole('button', { name: '결제하기' }).click()

  await expect(
    page.getByRole('heading', { name: '비밀번호 입력' }),
  ).toBeVisible()
  for (const digit of ['1', '2', '3', '4', '5', '6']) {
    await page.getByRole('button', { name: digit, exact: true }).click()
  }

  await expect(page).toHaveURL(/\/ward\/payment\/42\/qr$/)
  await expect(page.getByLabel('결제 QR 코드').locator('svg')).toBeVisible()
  await expect(page.getByText('결제 가능 잔액')).toBeVisible()

  await expect(page).toHaveURL(/\/ward\/payment\/73\/complete$/, {
    timeout: 6_000,
  })
  await expect(page.getByText('결제가 완료되었습니다')).toBeVisible()
  await expect(page.getByText('스타벅스 강남점')).toBeVisible()

  await page.getByRole('button', { name: '홈으로' }).click()
  await expect(page).toHaveURL(/\/ward\/home$/)
})
