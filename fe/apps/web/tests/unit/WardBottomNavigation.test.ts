import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import WardBottomNavigation from '@/pages/ward/-components/WardBottomNavigation.vue'

describe('WardBottomNavigation', () => {
  it('isPaired가 true인 경우 송금 및 결제 버튼 클릭 시 navigate 이벤트를 발송한다', async () => {
    const wrapper = mount(WardBottomNavigation, {
      props: {
        active: 'home',
        isPaired: true,
      },
    })

    const buttons = wrapper.findAll('button')
    // left: 송금, center: 홈, right: 결제
    const transferButton = buttons.find((btn) => btn.text().includes('송금'))
    const paymentButton = buttons.find((btn) => btn.text().includes('결제'))

    expect(transferButton).toBeDefined()
    expect(paymentButton).toBeDefined()

    await transferButton?.trigger('click')
    expect(wrapper.emitted('navigate')).toBeTruthy()
    expect(wrapper.emitted('navigate')?.[0]).toEqual(['transfer'])

    await paymentButton?.trigger('click')
    expect(wrapper.emitted('navigate')?.[1]).toEqual(['payment'])
  })

  it('isPaired가 false인 경우 송금 및 결제 버튼이 비활성화되어 navigate 이벤트를 발송하지 않는다', async () => {
    const wrapper = mount(WardBottomNavigation, {
      props: {
        active: 'home',
        isPaired: false,
      },
    })

    const buttons = wrapper.findAll('button')
    const transferButton = buttons.find((btn) => btn.text().includes('송금'))
    const paymentButton = buttons.find((btn) => btn.text().includes('결제'))

    expect(transferButton?.classes()).toContain('pointer-events-none')
    expect(paymentButton?.classes()).toContain('pointer-events-none')

    await transferButton?.trigger('click')
    expect(wrapper.emitted('navigate')).toBeFalsy()

    await paymentButton?.trigger('click')
    expect(wrapper.emitted('navigate')).toBeFalsy()
  })
})
