import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TransferHeldView from '@/pages/ward/transfer/-components/TransferHeldView.vue'

const rows = [{ label: '송금 금액', value: '50,000원' }]

describe('TransferHeldView', () => {
  it('취소할 수 없는 실제 거래에는 보호자 확인을 안내한다', () => {
    const wrapper = mount(TransferHeldView, { props: { rows } })

    expect(wrapper.text()).toContain('보호자에게 연락해 주세요')
    expect(wrapper.text()).not.toContain('거래 취소하기')
  })

  it('취소 가능한 거래에는 취소 안내와 버튼을 표시한다', () => {
    const wrapper = mount(TransferHeldView, {
      props: { rows, canCancel: true },
    })

    expect(wrapper.text()).toContain('취소하는 것이 안전합니다')
    expect(wrapper.text()).toContain('거래 취소하기')
  })
})
