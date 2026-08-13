import { flushPromises, mount } from '@vue/test-utils'
import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  enablePushNotifications: vi.fn(),
  routerReplace: vi.fn(),
}))

const availability = ref<'checking' | 'available' | 'unsupported' | 'disabled'>(
  'available',
)
const errorMessage = ref('')
const isEnabled = ref(false)
const isSyncing = ref(false)
const permission = ref<NotificationPermission>('default')

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: mocks.routerReplace }),
}))

vi.mock('@/composables/usePushNotification', () => ({
  usePushNotification: () => ({
    availability,
    enablePushNotifications: mocks.enablePushNotifications,
    errorMessage,
    isEnabled,
    isSyncing,
    permission,
  }),
}))

import GuardPushNotificationOnboardingPage from '@/pages/guard/onboarding/push-notifications/page.vue'

describe('GuardPushNotificationOnboardingPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    availability.value = 'available'
    errorMessage.value = ''
    isEnabled.value = false
    isSyncing.value = false
    permission.value = 'default'
  })

  it('사용자 클릭으로 알림을 활성화한 뒤 보호자 홈으로 이동한다', async () => {
    mocks.enablePushNotifications.mockResolvedValue(true)
    const wrapper = mount(GuardPushNotificationOnboardingPage)

    await wrapper
      .findAll('button')
      .find((button) => button.text() === '알림 받고 시작하기')!
      .trigger('click')
    await flushPromises()

    expect(mocks.enablePushNotifications).toHaveBeenCalledOnce()
    expect(mocks.routerReplace).toHaveBeenCalledWith({ name: 'guard-home' })
  })

  it('권한을 허용하지 않으면 온보딩에 남아 다시 선택할 수 있다', async () => {
    mocks.enablePushNotifications.mockResolvedValue(false)
    const wrapper = mount(GuardPushNotificationOnboardingPage)

    await wrapper
      .findAll('button')
      .find((button) => button.text() === '알림 받고 시작하기')!
      .trigger('click')
    await flushPromises()

    expect(mocks.routerReplace).not.toHaveBeenCalled()
  })

  it('나중에 하기를 선택하면 권한 요청 없이 홈으로 이동한다', async () => {
    const wrapper = mount(GuardPushNotificationOnboardingPage)

    await wrapper
      .findAll('button')
      .find((button) => button.text() === '나중에 하기')!
      .trigger('click')

    expect(mocks.enablePushNotifications).not.toHaveBeenCalled()
    expect(mocks.routerReplace).toHaveBeenCalledWith({ name: 'guard-home' })
  })

  it('권한이 차단된 환경에서는 설정 안내와 홈 이동을 제공한다', async () => {
    permission.value = 'denied'
    const wrapper = mount(GuardPushNotificationOnboardingPage)

    expect(wrapper.text()).toContain('알림이 차단되어 있어요')
    expect(wrapper.text()).not.toContain('나중에 하기')

    await wrapper
      .findAll('button')
      .find((button) => button.text() === '홈으로')!
      .trigger('click')

    expect(mocks.enablePushNotifications).not.toHaveBeenCalled()
    expect(mocks.routerReplace).toHaveBeenCalledWith({ name: 'guard-home' })
  })
})
