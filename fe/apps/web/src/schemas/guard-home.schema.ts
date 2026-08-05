import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

const guardWardSchema = z.object({
  wardId: z.number().int().positive(),
  name: z.string().min(1),
  avatarId: z.number().int().min(1).max(6),
  hasPending: z.boolean(),
})

const pendingApprovalSchema = z.object({
  transactionId: z.number().int().positive(),
  amount: z.number().int().nonnegative().nullable(),
  holderName: z.string().min(1).nullable(),
  accountNo: z.string().min(1).nullable(),
  riskScore: z.number().int().nonnegative().nullable(),
  riskReason: z.string().min(1).nullable(),
  createdAt: z.string().min(1),
})

const recentTransactionSchema = z.object({
  transactionId: z.number().int().positive(),
  type: z.enum(['CHARGE', 'TRANSFER', 'PAYMENT']),
  status: z.enum(['COMPLETED', 'REJECTED', 'CANCELED']),
  counterpartyName: z.string().min(1).nullable(),
  amount: z.number().int().nonnegative(),
  riskLevel: z.enum(['SAFE', 'CAUTION', 'DANGER']).nullable(),
  riskReason: z.string().min(1).nullable(),
  createdAt: z.string().min(1),
})

const selectedWardSchema = z.object({
  wardId: z.number().int().positive(),
  name: z.string().min(1),
  balance: z.number().int().nonnegative(),
  pendingApproval: pendingApprovalSchema.nullable(),
  recentTransactions: z.array(recentTransactionSchema),
})

export const guardHomeSchema = z.object({
  wards: z.array(guardWardSchema),
  selectedWard: selectedWardSchema.nullable(),
})

export const guardHomeResponseSchema = apiResponseSchema(guardHomeSchema)

export type GuardHome = z.infer<typeof guardHomeSchema>
export type GuardWard = z.infer<typeof guardWardSchema>
export type GuardRecentTransaction = z.infer<typeof recentTransactionSchema>
