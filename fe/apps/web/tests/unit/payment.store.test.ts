import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { usePaymentStore } from '@/stores/payment.store'

describe('payment store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-29T06:30:00.000Z'))
  })

  afterEach(() => vi.useRealTimers())

  it('restores a QR session without storing a pin', () => {
    const store = usePaymentStore()
    store.saveQrSession({
      paymentId: 42,
      qrToken: 'pay_qr_test',
      availableBalance: 130_000,
      expiresAt: '2026-07-29T15:31:00+09:00',
      expiresInSeconds: 60,
    })

    const persisted = sessionStorage.getItem('pay-with:ward-payment') ?? ''
    expect(persisted).not.toContain('"pin"')
    expect(store.qrExpiresAt).toBe(Date.now() + 60_000)

    vi.advanceTimersByTime(10_000)
    setActivePinia(createPinia())
    const restored = usePaymentStore()
    restored.restore()
    expect(restored.qrSession?.paymentId).toBe(42)
    expect(restored.qrExpiresAt).toBe(Date.now() + 50_000)
  })
})
