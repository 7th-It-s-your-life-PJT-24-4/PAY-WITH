import { expect, test } from './fixtures'

test.beforeEach(async ({ page }) => {
  let user = {
    id: 1,
    name: '김보호',
    phone: '01012345678',
    role: 'GUARD' as const,
    avatarId: 2,
    createdAt: '2026-08-05T10:00:00',
    updatedAt: '2026-08-05T10:00:00',
  }
  let isWardConnected = true

  await page.route('**/api/users/1', async (route) => {
    if (route.request().method() === 'DELETE') {
      await route.fulfill({
        contentType: 'application/json',
        json: { success: true, data: null, message: null },
      })
      return
    }

    if (route.request().method() === 'PUT') {
      user = { ...user, ...(route.request().postDataJSON() as object) }
    }

    await route.fulfill({
      contentType: 'application/json',
      json: { success: true, data: user, message: null },
    })
  })

  await page.route('**/api/guard/pairing/12', async (route) => {
    isWardConnected = false
    await route.fulfill({
      contentType: 'application/json',
      json: { success: true, data: null, message: null },
    })
  })

  await page.route('**/api/guard', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      json: {
        success: true,
        data: {
          wards: isWardConnected
            ? [
                {
                  wardId: 12,
                  name: '김시니어',
                  avatarId: 3,
                  hasPending: false,
                },
              ]
            : [],
          selectedWard: null,
        },
        message: null,
      },
    })
  })
})

test('마이페이지에서 로그아웃과 탈퇴 확인 팝업을 표시한다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.goto('/guard/my')

  await expect(page.getByRole('heading', { name: '마이페이지' })).toBeVisible()
  await page.getByRole('button', { name: '로그아웃' }).click()
  await expect(
    page.getByRole('heading', { name: '로그아웃 할까요?' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '취소' }).click()

  await page.getByRole('button', { name: '탈퇴하기' }).click()
  await expect(page.getByRole('heading', { name: '탈퇴할까요?' })).toBeVisible()

  const deleteRequest = page.waitForRequest(
    (request) =>
      request.url().endsWith('/api/users/1') && request.method() === 'DELETE',
  )
  await page.getByRole('button', { name: '확인' }).click()
  await deleteRequest
  await expect(page).toHaveURL(/\/auth\/sign-in$/)
  await expect
    .poll(() => page.evaluate(() => localStorage.getItem('accessToken')))
    .toBeNull()
})

test('마이페이지에서 기존 이용약관과 개인정보 페이지로 이동한다', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.goto('/guard/my')

  await page.getByRole('button', { name: '이용약관' }).click()
  await expect(page).toHaveURL(/\/guard\/my\/terms\/serviceTerms$/)
  await expect(
    page.getByRole('heading', { name: '서비스 이용약관' }),
  ).toBeVisible()

  await page.getByRole('button', { name: '뒤로 가기' }).click()
  await page.getByRole('button', { name: '개인정보처리방침' }).click()
  await expect(page).toHaveURL(/\/guard\/my\/terms\/privacyTerms$/)
  await expect(
    page.getByRole('heading', { name: '개인정보 수집 및 이용 동의' }),
  ).toBeVisible()
})

test('마이페이지에서 푸시알림설정 화면으로 이동한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.goto('/guard/my')

  await page.getByRole('button', { name: '푸시알림설정' }).click()

  await expect(page).toHaveURL(/\/guard\/my\/push-notifications$/)
  await expect(
    page.getByRole('heading', { name: '푸시알림설정' }),
  ).toBeVisible()
  await expect(page.getByRole('heading', { name: '알림 받기' })).toBeVisible()
  const notificationSwitch = page.getByRole('switch', { name: '알림 받기' })
  await expect(notificationSwitch).toBeVisible()
  await expect(page.getByText('다양한 알림을 실시간으로 받아요.')).toBeVisible()

  expect(await notificationSwitch.boundingBox()).toMatchObject({
    width: 44,
    height: 24,
  })
  expect(await notificationSwitch.locator('img').boundingBox()).toMatchObject({
    width: 20,
    height: 20,
  })
})

test('내 정보에서 이름과 프로필을 수정한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.goto('/guard/my/profile')

  await expect(page.getByText('김보호')).toBeVisible()
  await expect(page.getByText('010-1234-5678')).toBeVisible()
  await expect(page.getByText('1명')).toBeVisible()

  await page.getByRole('button', { name: '내 정보 수정' }).click()
  await page.getByRole('button', { name: '프로필 이미지 변경' }).click()
  await page.getByRole('button', { name: '프로필 4' }).click()
  await page.getByRole('button', { name: '선택하기' }).click()
  await page.getByLabel('성함 (실명)').fill('김새이름')

  const updateRequest = page.waitForRequest(
    (request) =>
      request.url().endsWith('/api/users/1') && request.method() === 'PUT',
  )
  await page.getByRole('button', { name: '내 정보 저장' }).click()

  expect((await updateRequest).postDataJSON()).toEqual({
    name: '김새이름',
    avatarId: 4,
  })
  await expect(page).toHaveURL(/\/guard\/my\/profile$/)
  await expect(page.getByText('김새이름')).toBeVisible()
})

test('시니어 관리에서 연결을 해제한다', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 820 })
  await page.goto('/guard/my/seniors')

  await expect(page.getByText('김시니어')).toBeVisible()
  await page.getByRole('button', { name: '연결 해제' }).click()
  await expect(
    page.getByRole('heading', {
      name: '김시니어님과 연결을 해제할까요?',
    }),
  ).toBeVisible()

  const deleteRequest = page.waitForRequest(
    (request) =>
      request.url().endsWith('/api/guard/pairing/12') &&
      request.method() === 'DELETE',
  )
  await page.getByRole('button', { name: '확인' }).click()
  await deleteRequest

  await expect(page.getByText('연결된 시니어가 없어요.')).toBeVisible()
})
