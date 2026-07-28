import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import { getMockHeldTransfers } from '@/mocks/transfer.mock'
import WardPendingTransferList from '@/pages/ward/-components/WardPendingTransferList.vue'

describe('WardPendingTransferList', () => {
  it('대기 거래가 없으면 표시하지 않는다', () => {
    const wrapper = mount(WardPendingTransferList, {
      props: { transfers: [] },
    })

    expect(wrapper.find('section').exists()).toBe(false)
  })

  it('선택한 대기 거래 번호를 전달한다', async () => {
    const transfers = getMockHeldTransfers()
    const wrapper = mount(WardPendingTransferList, {
      props: { transfers },
    })

    await wrapper.findAll('button')[0]?.trigger('click')

    expect(wrapper.emitted('select')?.[0]).toEqual([
      transfers[0]?.transactionId,
    ])
  })
})
