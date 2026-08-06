import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const paymentStatusValueSchema = z.enum([
  'PENDING',
  'PROCESSING',
  'COMPLETED',
  'FAILED',
  'EXPIRED',
  'CANCELED',
])

export const createWardPaymentRequestSchema = z.object({
  pin: z.string().regex(/^\d{6}$/),
})

export const paymentQrSessionSchema = z.object({
  paymentId: z.number().int().positive(),
  qrToken: z.string().min(1),
  availableBalance: z.number().int().nonnegative(),
  expiresAt: z.string().datetime({ offset: true }),
  expiresInSeconds: z.number().int().positive(),
})

export const paymentStatusSchema = z.object({
  paymentId: z.number().int().positive(),
  transactionId: z.number().int().positive().nullable(),
  status: paymentStatusValueSchema,
  merchantName: z.string().nullable(),
  amount: z.number().int().positive().nullable(),
  paidAt: z.string().datetime({ offset: true }).nullable(),
  remainingBalance: z.number().int().nonnegative().nullable(),
  failureCode: z.string().nullable(),
  failureMessage: z.string().nullable(),
  expiresAt: z.string().datetime({ offset: true }),
})

export const completedPaymentSchema = paymentStatusSchema.extend({
  transactionId: z.number().int().positive(),
  status: z.literal('COMPLETED'),
  merchantName: z.string().min(1),
  amount: z.number().int().positive(),
  paidAt: z.string().datetime({ offset: true }),
  remainingBalance: z.number().int().nonnegative(),
})

export const executePaymentRequestSchema = z.object({
  qrToken: z.string().min(1),
  merchantId: z.number().int().positive(),
  amount: z.number().int().positive(),
})

/** 가맹점 스캐너의 결제 실행 결과 — 완료 건 재요청 시 같은 응답이 멱등 반환된다 */
export const executedPaymentSchema = z.object({
  transactionId: z.number().int().positive(),
  status: z.string().min(1),
  amount: z.number().int().positive(),
  merchantName: z.string().min(1),
  createdAt: z.string().datetime({ offset: true }),
})

export const paymentCancelSchema = z.object({
  paymentId: z.number().int().positive(),
  status: z.literal('CANCELED'),
  canceledAt: z.string().datetime({ offset: true }),
})

export const paymentQrSessionResponseSchema = apiResponseSchema(
  paymentQrSessionSchema,
)
export const paymentStatusResponseSchema =
  apiResponseSchema(paymentStatusSchema)
export const paymentCancelResponseSchema =
  apiResponseSchema(paymentCancelSchema)
export const executedPaymentResponseSchema = apiResponseSchema(
  executedPaymentSchema,
)

export type CreateWardPaymentRequest = z.infer<
  typeof createWardPaymentRequestSchema
>
export type PaymentQrSession = z.infer<typeof paymentQrSessionSchema>
export type PaymentStatus = z.infer<typeof paymentStatusSchema>
export type CompletedPayment = z.infer<typeof completedPaymentSchema>
export type PaymentCancel = z.infer<typeof paymentCancelSchema>
export type ExecutePaymentRequest = z.infer<typeof executePaymentRequestSchema>
export type ExecutedPayment = z.infer<typeof executedPaymentSchema>
