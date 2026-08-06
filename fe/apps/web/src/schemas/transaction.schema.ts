import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const transactionRiskLevelSchema = z.enum(['SAFE', 'CAUTION', 'DANGER'])

export const transactionRiskAnalysisSchema = z.object({
  riskScore: z.number().int().min(0).max(100),
  summary: z.string().nullable(),
  reasons: z.array(z.string()),
})

export const guardTransactionDetailSchema = z.object({
  transactionId: z.number().int().positive(),
  type: z.enum(['CHARGE', 'PAYMENT', 'TRANSFER']),
  direction: z.enum(['IN', 'OUT']),
  status: z.enum([
    'REQUESTED',
    'HELD',
    'APPROVED',
    'PROCESSING',
    'REJECTED',
    'COMPLETED',
    'CANCELED',
    'BLOCKED',
    'FAILED',
  ]),
  riskLevel: transactionRiskLevelSchema.nullable(),
  counterpartyName: z.string().nullable(),
  bankName: z.string().nullable(),
  accountNo: z.string().nullable(),
  amount: z.number().int().nonnegative(),
  memo: z.string().nullable(),
  balanceAfter: z.number().int().nonnegative().nullable(),
  riskScore: z.number().int().min(0).max(100).nullable(),
  riskAnalysis: transactionRiskAnalysisSchema.nullable(),
  occurredAt: z.string().min(1),
})

export const guardTransactionDetailResponseSchema = apiResponseSchema(
  guardTransactionDetailSchema,
)

export type GuardTransactionDetail = z.infer<
  typeof guardTransactionDetailSchema
>
