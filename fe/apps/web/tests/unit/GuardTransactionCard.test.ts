import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GuardTransactionCard from '@/pages/guard/-components/GuardTransactionCard.vue'

describe('GuardTransactionCard', () => {
  it('설명과 강조된 금액, 위험도 배지를 표시하고 선택 이벤트를 보낸다', async () => {
    const wrapper = mount(GuardTransactionCard, {
      props: {
        amount: '-10,000원',
        accessibleLabel: '김예호 송금 거래 상세 보기',
        category: 'transfer',
        description: '김시니어 계좌 → 김예호',
        status: 'danger',
      },
    })

    expect(wrapper.get('button').attributes('aria-label')).toBe(
      '김예호 송금 거래 상세 보기',
    )
    expect(wrapper.text()).toContain('김시니어 계좌 → 김예호')
    expect(wrapper.text()).toContain('-10,000원')
    expect(wrapper.text()).toContain('위험')

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('select')).toHaveLength(1)
  })

  it('충전 카드는 위험도 배지 없이 가독성 높은 금액 색상을 사용한다', () => {
    const wrapper = mount(GuardTransactionCard, {
      props: {
        amount: '-50,000원',
        accessibleLabel: '김시니어 충전 상세 보기',
        category: 'charge',
        description: '김시니어 충전',
      },
    })

    expect(wrapper.text()).not.toContain('안전')
    const amount = wrapper.get('[data-testid="transaction-amount"]')
    expect(amount.text()).toBe('-50,000원')
    expect(amount.classes()).toContain('text-[#087E96]')
  })
})
