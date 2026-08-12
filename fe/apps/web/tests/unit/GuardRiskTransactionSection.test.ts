import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GuardRiskTransactionSection from '@/pages/guard/-components/GuardRiskTransactionSection.vue'

const pendingApproval = {
  transactionId: 41,
  amount: 35_000,
  holderName: '박수취',
  accountNo: '110234567890',
  riskScore: 87,
  riskReason: '메모에 위험 키워드 포함',
  createdAt: '2026-08-05T09:10:00',
}

describe('GuardRiskTransactionSection', () => {
  it('대기 거래가 없어도 이상 거래 내역 진입 경로를 표시한다', async () => {
    const wrapper = mount(GuardRiskTransactionSection, {
      props: { pendingApproval: null },
    })

    expect(wrapper.get('h2').text()).toBe('이상 거래 내역')
    expect(wrapper.text()).toContain('대기중인 이상거래가 없어요.')
    expect(wrapper.get('section').classes()).toContain('pb-[48px]')

    await wrapper.get('[aria-label="이상 거래 내역 더보기"]').trigger('click')
    expect(wrapper.emitted('more')).toHaveLength(1)
  })

  it('대기 거래가 있으면 날짜와 거래 요약을 위험 상태로 표시한다', async () => {
    const wrapper = mount(GuardRiskTransactionSection, {
      props: { pendingApproval },
    })

    expect(wrapper.text()).toContain('8월 5일')
    expect(wrapper.text()).toContain('-35,000원')
    expect(wrapper.text()).toContain('박수취')
    expect(wrapper.text()).toContain('위험')
    expect(wrapper.text()).not.toContain('대기중인 이상거래가 없어요.')
    expect(wrapper.get('section').classes()).toContain('pb-xl')

    await wrapper
      .get('[aria-label="8월 5일 -35,000원 박수취 이상 거래 목록 보기"]')
      .trigger('click')
    expect(wrapper.emitted('more')).toHaveLength(1)
  })
})
