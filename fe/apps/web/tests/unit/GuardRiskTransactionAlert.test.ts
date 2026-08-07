import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GuardRiskTransactionAlert from '@/pages/guard/-components/GuardRiskTransactionAlert.vue'

describe('GuardRiskTransactionAlert', () => {
  it('안내 문구 묶음을 양쪽 아이콘과 세로 가운데 정렬한다', () => {
    const wrapper = mount(GuardRiskTransactionAlert, {
      props: {
        count: 2,
      },
    })

    const heading = wrapper.get('#guard-risk-transaction-title')
    const alert = wrapper.get(
      'section[aria-labelledby="guard-risk-transaction-title"]',
    )

    expect(heading.text()).toBe('위험 거래 2건 발생')
    expect(heading.classes()).not.toContain('text-center')
    expect(alert.classes()).toContain('flex')
    expect(alert.classes()).toContain('items-center')
  })
})
