import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  dismissForegroundNotification: vi.fn(),
}))

const foregroundNotification = ref({
  title: '송금이 완료되었습니다',
  body: '김철수님께 10,000원을 보냈습니다.',
  path: '/ward/history/9?source=push',
  data: {
    type: 'APPROVAL_RESULT' as const,
    refType: 'TRANSACTION' as const,
    refId: '9',
  },
  expectedRole: 'WARD' as const,
})

vi.mock('@/composables/usePushNotification', () => ({
  usePushNotification: () => ({
    dismissForegroundNotification: mocks.dismissForegroundNotification,
    foregroundNotification,
  }),
}))

const routerPush = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

import ForegroundPushNotification from '@/components/notifications/ForegroundPushNotification.vue'

describe('ForegroundPushNotification', () => {
  beforeEach(() => {
    mocks.dismissForegroundNotification.mockClear()
    routerPush.mockClear()
  })

  it('알림을 표시한다', () => {
    const wrapper = mount(ForegroundPushNotification)

    expect(wrapper.text()).toContain('송금이 완료되었습니다')
    expect(wrapper.text()).toContain('김철수님께 10,000원을 보냈습니다.')
  })

  it('닫기 버튼을 누르면 알림을 닫는다', async () => {
    const wrapper = mount(ForegroundPushNotification)

    await wrapper.get('button:not(.text-left)').trigger('click')

    expect(mocks.dismissForegroundNotification).toHaveBeenCalled()
  })

  it('알림을 클릭하면 거래 상세로 이동하고 알림을 닫는다', async () => {
    const wrapper = mount(ForegroundPushNotification)

    await wrapper.find('button.text-left').trigger('click')

    expect(mocks.dismissForegroundNotification).toHaveBeenCalledOnce()
    expect(routerPush).toHaveBeenCalledWith('/ward/history/9?source=push')
  })
})
