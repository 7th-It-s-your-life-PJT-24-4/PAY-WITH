import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const transactionTypeSchema = z.enum(['CHARGE', 'TRANSFER', 'PAYMENT'])
export const transactionRiskLevelSchema = z.enum(['SAFE', 'CAUTION', 'DANGER'])

const guardTransactionHistoryItemSchema = z.object({
  transactionId: z.number().int().positive(),
  type: transactionTypeSchema,
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
  counterpartyName: z.string().nullable(),
  amount: z.number().nonnegative(),
  riskLevel: transactionRiskLevelSchema.nullable(),
  riskReason: z.string().nullable(),
  createdAt: z.string().min(1),
})

const riskReasonDetailSchema = z.object({
  ruleCode: z.string().min(1),
  description: z.string().min(1),
  score: z.number().int(),
})

const riskAnalysisSchema = z.object({
  riskScore: z.number().int().min(0).max(100),
  summary: z.string().nullable(),
  reasons: z.array(riskReasonDetailSchema),
  analyzedAt: z.string().nullable(),
})

const guardTransactionDetailSchema = z.object({
  transactionId: z.number().int().positive(),
  type: transactionTypeSchema,
  direction: z.enum(['IN', 'OUT']),
  status: z.string().min(1),
  riskLevel: transactionRiskLevelSchema.nullable(),
  counterpartyName: z.string().nullable(),
  bankName: z.string().nullable(),
  accountNo: z.string().nullable(),
  amount: z.number().nonnegative(),
  memo: z.string().nullable(),
  occurredAt: z.string().min(1),
  balanceAfter: z.number().nonnegative().nullable(),
  riskScore: z.null(),
  analyzedAt: z.null(),
  riskAnalysis: riskAnalysisSchema.nullable(),
})

const guardTransactionHistorySchema = z.object({
  transactions: z.array(guardTransactionHistoryItemSchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
  hasNext: z.boolean(),
})

export const guardTransactionHistoryResponseSchema = apiResponseSchema(
  guardTransactionHistorySchema,
)
export const guardTransactionDetailResponseSchema = apiResponseSchema(
  guardTransactionDetailSchema,
)

export type TransactionRiskLevel = z.infer<typeof transactionRiskLevelSchema>
export type GuardTransactionHistory = z.infer<
  typeof guardTransactionHistorySchema
>
export type GuardTransactionHistoryItem = z.infer<
  typeof guardTransactionHistoryItemSchema
>
export type GuardTransactionDetail = z.infer<
  typeof guardTransactionDetailSchema
>
