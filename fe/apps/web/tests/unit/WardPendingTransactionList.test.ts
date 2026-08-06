import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import WardPendingTransactionList from '@/pages/ward/-components/WardPendingTransactionList.vue'
import type { PendingApprovalItem } from '@/schemas/home.schema'

const transaction: PendingApprovalItem = {
  approvalId: 7,
  transactionId: 74,
  type: 'TRANSFER_OUT',
  amount: 50_000,
  holderName: '김민수',
  bankName: '국민은행',
  accountNo: '43210201234567',
  riskLevel: 'CAUTION',
  requestedAt: '2026-08-04T10:00:00',
  expiredAt: '2026-08-04T10:10:00',
}

describe('WardPendingTransactionList', () => {
  it('대기 거래가 없으면 표시하지 않는다', () => {
    const wrapper = mount(WardPendingTransactionList, {
      props: { transactions: [] },
    })

    expect(wrapper.find('ul').exists()).toBe(false)
  })

  it('승인 대기 송금을 독립 카드 버튼으로 표시하고 선택한 거래를 전달한다', async () => {
    const transactions = [transaction]
    const wrapper = mount(WardPendingTransactionList, {
      props: { transactions },
    })

    expect(wrapper.find('section').exists()).toBe(false)
    expect(wrapper.get('button').classes()).toContain('rounded-large')
    expect(wrapper.text()).toContain('송금')
    expect(wrapper.text()).toContain('김민수')
    expect(wrapper.text()).toContain('50,000원')
    expect(wrapper.text()).toContain('주의')
    expect(wrapper.text()).not.toContain('결제')

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('select')?.[0]).toEqual([transaction])
  })
})
