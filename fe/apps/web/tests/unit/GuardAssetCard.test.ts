import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GuardAssetCard from '@/pages/guard/-components/GuardAssetCard.vue'

describe('GuardAssetCard', () => {
  it('안전계좌 목록 버튼을 누르면 목록 열기를 요청한다', async () => {
    const wrapper = mount(GuardAssetCard, {
      props: {
        seniorName: '수이',
        balance: '1,000,000',
      },
    })

    const safeAccountButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('안전계좌 목록'))

    expect(safeAccountButton).toBeDefined()
    await safeAccountButton!.trigger('click')

    expect(wrapper.emitted('openSafeAccounts')).toHaveLength(1)
  })
})
