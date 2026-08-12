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

import PushNotificationPermissionCard from '@/components/PushNotificationPermissionCard.vue'

describe('PushNotificationPermissionCard', () => {
  it('default 권한에서는 설명 뒤 사용자 동작으로 권한을 요청한다', async () => {
    permission.value = 'default'
    availability.value = 'available'
    const wrapper = mount(PushNotificationPermissionCard)

    await wrapper.get('button').trigger('click')

    expect(wrapper.text()).toContain('중요한 거래 알림을 받아보세요')
    expect(mocks.requestPermission).toHaveBeenCalledOnce()
  })

  it('거부 상태에서는 재요청 버튼 없이 설정 안내를 표시한다', () => {
    permission.value = 'denied'
    availability.value = 'available'
    const wrapper = mount(PushNotificationPermissionCard)

    expect(wrapper.text()).toContain('브라우저에서 알림을 허용해 주세요')
    expect(wrapper.find('button').exists()).toBe(false)
  })
})
