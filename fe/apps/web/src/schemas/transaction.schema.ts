import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const transactionRiskLevelSchema = z.enum(['SAFE', 'CAUTION', 'DANGER'])

export const guardTransactionHistoryItemSchema = z.object({
  transactionId: z.number().int().positive(),
  type: z.enum(['CHARGE', 'PAYMENT', 'TRANSFER']),
  status: z.enum([
    'PENDING',
    'REQUESTED',
    'HELD',
    'APPROVED',
    'PROCESSING',
    'REJECTED',
    'COMPLETED',
    'CANCELED',
    'EXPIRED',
    'BLOCKED',
    'FAILED',
  ]),
  counterpartyName: z.string().nullable(),
  amount: z.number().int().nonnegative(),
  riskLevel: transactionRiskLevelSchema.nullable(),
  riskReason: z.string().nullable(),
  createdAt: z.string().min(1),
})

export const guardTransactionHistorySchema = z.object({
  transactions: z.array(guardTransactionHistoryItemSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
  hasNext: z.boolean(),
})

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
  riskAnalysis: transactionRiskAnalysisSchema.nullable(),
  occurredAt: z.string().min(1),
})

export const guardTransactionDetailResponseSchema = apiResponseSchema(
  guardTransactionDetailSchema,
)

export const guardTransactionHistoryResponseSchema = apiResponseSchema(
  guardTransactionHistorySchema,
)

export type GuardTransactionDetail = z.infer<
  typeof guardTransactionDetailSchema
>
export type GuardTransactionHistoryItem = z.infer<
  typeof guardTransactionHistoryItemSchema
>
export type GuardTransactionHistory = z.infer<
  typeof guardTransactionHistorySchema
>
export type TransactionRiskLevel = z.infer<typeof transactionRiskLevelSchema>
