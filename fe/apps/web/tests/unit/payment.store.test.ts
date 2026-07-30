import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { usePaymentStore } from '@/stores/payment.store'

describe('payment store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
  })

  it('restores a QR session without storing a pin', () => {
    const store = usePaymentStore()
    store.saveQrSession({
      paymentId: 42,
      qrToken: 'pay_qr_test',
      availableBalance: 130_000,
      expiresAt: '2026-07-29T15:31:00+09:00',
    })

    const persisted = sessionStorage.getItem('pay-with:ward-payment') ?? ''
    expect(persisted).not.toContain('"pin"')

    setActivePinia(createPinia())
    const restored = usePaymentStore()
    restored.restore()
    expect(restored.qrSession?.paymentId).toBe(42)
  })
})
