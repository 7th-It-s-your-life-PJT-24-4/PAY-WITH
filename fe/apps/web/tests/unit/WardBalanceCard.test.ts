import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import WardBalanceCard from '@/pages/ward/-components/WardBalanceCard.vue'

describe('WardBalanceCard', () => {
  it('shows only the balance when the wallet is unlocked', () => {
    const wrapper = mount(WardBalanceCard, {
      props: {
        balance: '100,000',
      },
    })

    expect(wrapper.text()).toContain('100,000')
    expect(wrapper.text()).not.toContain('지갑 잠김')
    expect(wrapper.find('button').exists()).toBe(false)
  })

  it('shows the locked state and requests guardian contact', async () => {
    const wrapper = mount(WardBalanceCard, {
      props: {
        balance: '100,000',
        locked: true,
      },
    })

    expect(wrapper.get('[role="status"]').text()).toContain('지갑 잠김')
    expect(wrapper.text()).toContain('안전을 위해 지갑이 잠금처리되었습니다.')

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('contactGuardian')).toHaveLength(1)
  })
})
