import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import WardPendingTransactionSummaryCard from '@/pages/ward/-components/WardPendingTransactionSummaryCard.vue'

describe('WardPendingTransactionSummaryCard', () => {
  it('대기 거래가 없으면 표시하지 않는다', () => {
    const wrapper = mount(WardPendingTransactionSummaryCard, {
      props: { count: 0 },
    })

    expect(wrapper.find('section').exists()).toBe(false)
  })

  it('대기 거래 건수를 표시하고 카드 선택을 전달한다', async () => {
    const wrapper = mount(WardPendingTransactionSummaryCard, {
      props: { count: 2 },
    })

    expect(wrapper.text()).toContain('승인 대기 거래가 2건 있습니다')

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('open')).toHaveLength(1)
  })
})
