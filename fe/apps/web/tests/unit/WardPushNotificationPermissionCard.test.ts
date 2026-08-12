import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  requestPermission: vi.fn(),
}))

const availability = ref<'available' | 'unsupported'>('available')
const permission = ref<NotificationPermission>('default')

vi.mock('@/composables/usePushNotification', () => ({
  usePushNotification: () => ({
    availability,
    errorMessage: ref(''),
    isSyncing: ref(false),
    permission,
    requestPermission: mocks.requestPermission,
    shouldShowPermissionCard: ref(true),
  }),
}))

import WardPushNotificationPermissionCard from '@/pages/ward/-components/WardPushNotificationPermissionCard.vue'

describe('WardPushNotificationPermissionCard', () => {
  it('기본 권한 상태에서 시니어 친화적 문구와 함께 권한 요청 버튼을 렌더링한다', async () => {
    permission.value = 'default'
    availability.value = 'available'
    const wrapper = mount(WardPushNotificationPermissionCard)

    expect(wrapper.text()).toContain('중요한 거래 알림을 받아보세요')
    expect(wrapper.text()).toContain(
      '송금 결과와 보호자 처리 결과를 놓치지 않도록 알려드려요.',
    )

    const button = wrapper.find('button')
    expect(button.exists()).toBe(true)
    expect(button.text()).toContain('알림 켜기')

    await button.trigger('click')
    expect(mocks.requestPermission).toHaveBeenCalledOnce()
  })

  it('거부 상태에서는 설정 안내 문구를 표시하고 버튼을 노출하지 않는다', () => {
    permission.value = 'denied'
    availability.value = 'available'
    const wrapper = mount(WardPushNotificationPermissionCard)

    expect(wrapper.text()).toContain('브라우저에서 알림을 켜주세요')
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('미지원 환경에서는 지원되지 않는다는 안내를 표시한다', () => {
    permission.value = 'default'
    availability.value = 'unsupported'
    const wrapper = mount(WardPushNotificationPermissionCard)

    expect(wrapper.text()).toContain('이 기기에서는 알림을 사용할 수 없어요')
    expect(wrapper.find('button').exists()).toBe(false)
  })
})
