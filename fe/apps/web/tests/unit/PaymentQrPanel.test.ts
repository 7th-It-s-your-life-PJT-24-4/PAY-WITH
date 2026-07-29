import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PaymentQrPanel from '@/pages/ward/payment/-components/PaymentQrPanel.vue'

describe('PaymentQrPanel', () => {
  it('exposes the payment token to the QR renderer boundary', () => {
    const wrapper = mount(PaymentQrPanel, {
      props: { expired: false, paymentToken: 'pay_qr_test' },
    })

    expect(
      wrapper
        .get('[aria-label="결제 QR 코드"]')
        .attributes('data-payment-token'),
    ).toBe('pay_qr_test')
  })

  it('requests reissue when the QR code has expired', async () => {
    const wrapper = mount(PaymentQrPanel, {
      props: { expired: true, paymentToken: 'pay_qr_expired' },
    })

    const reissueButton = wrapper.get('button')
    expect(reissueButton.text()).toContain('QR 코드 재발급')

    await reissueButton.trigger('click')

    expect(wrapper.emitted('reissue')).toHaveLength(1)
  })
})
