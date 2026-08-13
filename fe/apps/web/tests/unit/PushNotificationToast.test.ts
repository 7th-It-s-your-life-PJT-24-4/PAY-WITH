import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import type { ForegroundPushNotification } from '@/composables/usePushNotification'

const mocks = vi.hoisted(() => ({
  dismissForegroundNotification: vi.fn(),
  routerPush: vi.fn(),
}))

const foregroundNotification = ref<ForegroundPushNotification | null>(null)

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mocks.routerPush }),
}))

vi.mock('@/composables/usePushNotification', () => ({
  usePushNotification: () => ({
    dismissForegroundNotification: mocks.dismissForegroundNotification,
    foregroundNotification,
  }),
}))

import PushNotificationToast from '@/components/PushNotificationToast.vue'

describe('PushNotificationToast', () => {
  it('승인 요청을 포그라운드에서 표시하고 상세 화면으로 이동한다', async () => {
    foregroundNotification.value = {
      title: '송금 승인 요청',
      body: '확인이 필요한 송금이 있어요.',
      data: {
        type: 'APPROVAL_REQUEST',
        refType: 'APPROVAL',
        refId: '7',
      },
      expectedRole: 'GUARD',
      path: '/guard/approval-requests/7?source=push',
    }
    const wrapper = mount(PushNotificationToast, {
      global: { stubs: { teleport: true, transition: false } },
    })

    expect(wrapper.text()).toContain('송금 승인 요청')
    expect(wrapper.text()).toContain('확인이 필요한 송금이 있어요.')

    await wrapper.get('button:not([aria-label])').trigger('click')

    expect(mocks.dismissForegroundNotification).toHaveBeenCalledOnce()
    expect(mocks.routerPush).toHaveBeenCalledWith(
      '/guard/approval-requests/7?source=push',
    )
  })

  it('이상거래 알림을 닫을 수 있다', async () => {
    mocks.dismissForegroundNotification.mockClear()
    foregroundNotification.value = {
      title: '결제 차단 알림',
      body: '위험 결제가 차단됐어요.',
      data: {
        type: 'ANOMALY',
        refType: 'TRANSACTION',
        refId: '8',
        wardId: '12',
      },
      expectedRole: 'GUARD',
      path: '/guard/history/8?wardId=12&source=push',
    }
    const wrapper = mount(PushNotificationToast, {
      global: { stubs: { teleport: true, transition: false } },
    })

    await wrapper.get('[aria-label="알림 닫기"]').trigger('click')

    expect(mocks.dismissForegroundNotification).toHaveBeenCalledOnce()
  })

  it('승인 결과 알림에는 일반 확인 문구를 표시한다', () => {
    foregroundNotification.value = {
      title: '승인 결과 알림',
      body: '보호자가 송금을 승인했어요.',
      data: {
        type: 'APPROVAL_RESULT',
        refType: 'APPROVAL',
        refId: '9',
      },
      expectedRole: 'WARD',
      path: '/ward?source=push',
    }

    const wrapper = mount(PushNotificationToast, {
      global: { stubs: { teleport: true, transition: false } },
    })

    expect(wrapper.get('button:not([aria-label])').text()).toBe('확인하기')
  })
})
