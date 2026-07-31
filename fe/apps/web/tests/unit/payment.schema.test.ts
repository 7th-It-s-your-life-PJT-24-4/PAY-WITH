import { describe, expect, it } from 'vitest'

import {
  createWardPaymentRequestSchema,
  paymentQrSessionResponseSchema,
  paymentStatusResponseSchema,
} from '@/schemas/payment.schema'

describe('payment schemas', () => {
  it('requires a six digit pin', () => {
    expect(createWardPaymentRequestSchema.parse({ pin: '123456' })).toEqual({
      pin: '123456',
    })
    expect(() =>
      createWardPaymentRequestSchema.parse({ pin: '12345' }),
    ).toThrow()
  })

  it('accepts nullable fields while a payment is pending', () => {
    expect(
      paymentStatusResponseSchema.parse({
        success: true,
        data: {
          paymentId: 42,
          transactionId: null,
          status: 'PENDING',
          merchantName: null,
          amount: null,
          paidAt: null,
          remainingBalance: null,
          failureCode: null,
          failureMessage: null,
          expiresAt: '2026-07-29T15:31:00+09:00',
        },
        message: null,
      }).data.status,
    ).toBe('PENDING')
  })

  it('keeps the server-provided QR lifetime in the session contract', () => {
    const response = paymentQrSessionResponseSchema.parse({
      success: true,
      data: {
        paymentId: 42,
        qrToken: 'pay_qr_test',
        availableBalance: 130_000,
        expiresAt: '2026-07-29T15:31:00+09:00',
        expiresInSeconds: 60,
      },
      message: null,
    })

    expect(response.data.expiresInSeconds).toBe(60)
  })
})
