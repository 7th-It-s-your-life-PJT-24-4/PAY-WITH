import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GuardAssetCard from '@/pages/guard/-components/GuardAssetCard.vue'

describe('GuardAssetCard', () => {
  it('안전계좌 추가 버튼을 누르면 추가 플로우 시작을 요청한다', async () => {
    const wrapper = mount(GuardAssetCard, {
      props: {
        seniorName: '수이',
        balance: '1,000,000',
      },
    })

    await wrapper.get('button:nth-of-type(2)').trigger('click')

    expect(wrapper.emitted('addSafeAccount')).toHaveLength(1)
  })
})
