import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const transactionHistoryTypeSchema = z.enum([
  'CHARGE',
  'TRANSFER',
  'PAYMENT',
])

export const transactionDirectionSchema = z.enum(['IN', 'OUT'])
export const transactionRiskLevelSchema = z.enum(['SAFE', 'CAUTION', 'DANGER'])

export const wardTransactionHistoryItemSchema = z.object({
  transactionId: z.number().int().positive(),
  type: transactionHistoryTypeSchema,
  direction: transactionDirectionSchema,
  title: z.string(),
  amount: z.number().int().nonnegative(),
  status: z.string().min(1),
  riskLevel: transactionRiskLevelSchema.nullable(),
  occurredAt: z.string().min(1),
})

export const guardTransactionHistoryItemSchema = z.object({
  transactionId: z.number().int().positive(),
  type: transactionHistoryTypeSchema,
  status: z.string().min(1),
  counterpartyName: z.string().nullable(),
  amount: z.number().int().nonnegative(),
  riskLevel: transactionRiskLevelSchema.nullable(),
  riskReason: z.string().nullable(),
  createdAt: z.string().min(1),
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

export const wardTransactionHistorySchema = pagedTransactionHistorySchema(
  wardTransactionHistoryItemSchema,
)
export const guardTransactionHistorySchema = pagedTransactionHistorySchema(
  guardTransactionHistoryItemSchema,
)

export const wardTransactionHistoryResponseSchema = apiResponseSchema(
  wardTransactionHistorySchema,
)
export const guardTransactionHistoryResponseSchema = apiResponseSchema(
  guardTransactionHistorySchema,
)

export type TransactionHistoryType = z.infer<
  typeof transactionHistoryTypeSchema
>
export type TransactionRiskLevel = z.infer<typeof transactionRiskLevelSchema>
export type WardTransactionHistoryItem = z.infer<
  typeof wardTransactionHistoryItemSchema
>
export type GuardTransactionHistoryItem = z.infer<
  typeof guardTransactionHistoryItemSchema
>
export type WardTransactionHistory = z.infer<
  typeof wardTransactionHistorySchema
>
export type GuardTransactionHistory = z.infer<
  typeof guardTransactionHistorySchema
>
