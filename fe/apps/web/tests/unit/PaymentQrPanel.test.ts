import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PaymentQrPanel from '@/pages/ward/payment/-components/PaymentQrPanel.vue'

describe('PaymentQrPanel', () => {
  it('emits scan from an active QR code', async () => {
    const wrapper = mount(PaymentQrPanel, {
      props: { expired: false },
    })

    await wrapper.get('[aria-label="QR 코드 스캔 완료"]').trigger('click')

    expect(wrapper.emitted('scan')).toHaveLength(1)
  })

  it('requests reissue when the QR code has expired', async () => {
    const wrapper = mount(PaymentQrPanel, {
      props: { expired: true },
    })

    const reissueButton = wrapper.get('button')
    expect(reissueButton.text()).toContain('QR 코드 재발급')

    await reissueButton.trigger('click')

    expect(wrapper.emitted('reissue')).toHaveLength(1)
  })
})
