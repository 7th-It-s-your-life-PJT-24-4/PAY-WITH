import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import { getMockPendingTransactions } from '@/mocks/pending-transaction.mock'
import WardPendingTransactionList from '@/pages/ward/-components/WardPendingTransactionList.vue'

describe('WardPendingTransactionList', () => {
  it('대기 거래가 없으면 표시하지 않는다', () => {
    const wrapper = mount(WardPendingTransactionList, {
      props: { transactions: [] },
    })

    expect(wrapper.find('section').exists()).toBe(false)
  })

  it('송금과 결제 거래를 구분해 표시하고 선택한 거래를 전달한다', async () => {
    const transactions = getMockPendingTransactions()
    const wrapper = mount(WardPendingTransactionList, {
      props: { transactions },
    })

    expect(wrapper.text()).toContain('송금')
    expect(wrapper.text()).toContain('결제')
    expect(wrapper.text()).toContain('우리동네마트')

    await wrapper.findAll('button')[1]?.trigger('click')

    expect(wrapper.emitted('select')?.[0]).toEqual([transactions[1]])
  })
})
