import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const transactionHistoryTypeSchema = z.enum([
  'CHARGE',
  'TRANSFER',
  'PAYMENT',
])

export const transactionDirectionSchema = z.enum(['IN', 'OUT'])
export const transactionRiskLevelSchema = z.enum(['SAFE', 'CAUTION', 'DANGER'])
export const transactionStatusSchema = z.enum([
  'REQUESTED',
  'HELD',
  'APPROVED',
  'PROCESSING',
  'REJECTED',
  'COMPLETED',
  'CANCELED',
  'BLOCKED',
  'FAILED',
])

export const wardTransactionHistoryItemSchema = z.object({
  transactionId: z.number().int().positive(),
  type: transactionHistoryTypeSchema,
  direction: transactionDirectionSchema,
  title: z.string().nullable().transform(formatTransactionTitle),
  amount: z.number().int().nonnegative(),
  status: transactionStatusSchema,
  riskLevel: transactionRiskLevelSchema.nullable(),
  occurredAt: z.string().min(1),
})

export const transactionRiskAnalysisSchema = z.object({
  riskScore: z.number().int().min(0).max(100),
  summary: z.string().nullable(),
  reasons: z.array(z.string()),
})

export const wardTransactionDetailSchema = z.object({
  transactionId: z.number().int().positive(),
  type: transactionHistoryTypeSchema,
  direction: transactionDirectionSchema,
  status: transactionStatusSchema,
  riskLevel: transactionRiskLevelSchema.nullable(),
  counterpartyName: z.string().nullable(),
  bankName: z.string().nullable(),
  accountNo: z.string().nullable(),
  amount: z.number().int().nonnegative(),
  memo: z.string().nullable(),
  occurredAt: z.string().min(1),
  balanceAfter: z.number().int().nonnegative().nullable(),
  riskAnalysis: transactionRiskAnalysisSchema.nullable(),
})

function pagedTransactionHistorySchema<T extends z.ZodTypeAny>(itemSchema: T) {
  return z.object({
    transactions: z.array(itemSchema),
    page: z.number().int().nonnegative(),
    size: z.number().int().positive(),
    totalElements: z.number().int().nonnegative(),
    totalPages: z.number().int().nonnegative(),
    hasNext: z.boolean(),
  })
}

function formatTransactionTitle(title: string | null) {
  return title ?? '거래 상대'
}

export const wardTransactionHistorySchema = pagedTransactionHistorySchema(
  wardTransactionHistoryItemSchema,
)
export const wardTransactionHistoryResponseSchema = apiResponseSchema(
  wardTransactionHistorySchema,
)
export const wardTransactionDetailResponseSchema = apiResponseSchema(
  wardTransactionDetailSchema,
)

export type TransactionHistoryType = z.infer<
  typeof transactionHistoryTypeSchema
>
export type TransactionRiskLevel = z.infer<typeof transactionRiskLevelSchema>
export type TransactionStatus = z.infer<typeof transactionStatusSchema>
export type WardTransactionDetailResponse = z.infer<
  typeof wardTransactionDetailSchema
>
export type WardTransactionHistoryItem = z.infer<
  typeof wardTransactionHistoryItemSchema
>
export type WardTransactionHistory = z.infer<
  typeof wardTransactionHistorySchema
>
