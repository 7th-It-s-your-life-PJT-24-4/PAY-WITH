import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import NotificationList from '@/components/notifications/NotificationList.vue'

describe('NotificationList', () => {
  it('알림이 없으면 빈 상태를 표시한다', () => {
    const wrapper = mount(NotificationList)

    expect(wrapper.text()).toContain('새로운 알림이 없어요.')
  })

  it('알림을 표시하고 선택 이벤트를 전달한다', async () => {
    const notification = {
      id: 'notification-1',
      title: '송금이 완료되었습니다',
      body: '김철수님께 10,000원을 보냈습니다.',
    }
    const wrapper = mount(NotificationList, {
      props: { notifications: [notification] },
    })

    expect(wrapper.text()).toContain(notification.title)
    expect(wrapper.text()).toContain(notification.body)

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('select')?.[0]).toEqual([notification])
  })
})
