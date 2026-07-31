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

  it('hides both the QR code and reissue action while checking its status', () => {
    const wrapper = mount(PaymentQrPanel, {
      props: {
        checking: true,
        checkingMessage: '결제 상태를 확인할 수 없습니다',
        expired: false,
        qrToken: 'pay_qr_unknown',
      },
    })

    expect(wrapper.findComponent(QrcodeVue).exists()).toBe(false)
    expect(wrapper.find('button').exists()).toBe(false)
    expect(wrapper.get('[role="status"]').text()).toContain(
      '결제 상태를 확인할 수 없습니다',
    )
  })
})
