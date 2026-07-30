import { mount } from '@vue/test-utils'
import QrcodeVue from 'qrcode.vue'
import { describe, expect, it } from 'vitest'

import PaymentQrPanel from '@/pages/ward/payment/-components/PaymentQrPanel.vue'

describe('PaymentQrPanel', () => {
  it('renders the QR token as an SVG QR code', () => {
    const wrapper = mount(PaymentQrPanel, {
      props: { expired: false, qrToken: 'pay_qr_test' },
    })

    expect(wrapper.getComponent(QrcodeVue).props('value')).toBe('pay_qr_test')
    expect(
      wrapper.get('[aria-label="결제 QR 코드"]').find('svg').exists(),
    ).toBe(true)
  })

  it('requests reissue when the QR code has expired', async () => {
    const wrapper = mount(PaymentQrPanel, {
      props: { expired: true, qrToken: 'pay_qr_expired' },
    })

    const reissueButton = wrapper.get('button')
    expect(reissueButton.text()).toContain('QR 코드 재발급')

    await reissueButton.trigger('click')

    expect(wrapper.emitted('reissue')).toHaveLength(1)
  })
})
