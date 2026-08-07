import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const createChargeRequestSchema = z.object({
  accountId: z.number().int().positive(),
  amount: z.number().int().positive(),
})

export const createGuardChargeRequestSchema = createChargeRequestSchema.extend({
  pin: z.string().regex(/^\d{6}$/),
})

export const chargeResultSchema = z.object({
  transactionId: z.number().int().positive(),
  chargeAmount: z.number().int().positive(),
  balanceAfter: z.number().int().nonnegative(),
  bankName: z.string().min(1),
  accountNo: z.string().min(1),
  createdAt: z.string().min(1),
})

export const guardChargeResultSchema = chargeResultSchema.extend({
  wardId: z.number().int().positive(),
  wardName: z.string().min(1),
})

export const guardChargeHistoryItemSchema = z.object({
  transactionId: z.number().int().positive(),
  wardId: z.number().int().positive(),
  wardName: z.string().min(1),
  amount: z.number().int().positive(),
  createdAt: z.string().min(1),
})

export const guardChargeHistoriesSchema = z.object({
  charges: z.array(guardChargeHistoryItemSchema),
})

export const guardChargeDetailSchema = z.object({
  transactionId: z.number().int().positive(),
  wardId: z.number().int().positive(),
  wardName: z.string().min(1),
  amount: z.number().int().positive(),
  memo: z.string().nullable(),
  accountId: z.number().int().positive(),
  account: z.object({
    bankCode: z.string().min(1),
    bankName: z.string().min(1),
    accountNo: z.string().min(1),
  }),
  createdAt: z.string().min(1),
  balanceAfter: z.number().int().nonnegative(),
})

export const chargeResultResponseSchema = apiResponseSchema(chargeResultSchema)
export const guardChargeResultResponseSchema = apiResponseSchema(
  guardChargeResultSchema,
)
export const guardChargeHistoriesResponseSchema = apiResponseSchema(
  guardChargeHistoriesSchema,
)
export const guardChargeDetailResponseSchema = apiResponseSchema(
  guardChargeDetailSchema,
)

export type CreateChargeRequest = z.infer<typeof createChargeRequestSchema>
export type CreateGuardChargeRequest = z.infer<
  typeof createGuardChargeRequestSchema
>
export type ChargeResult = z.infer<typeof chargeResultSchema>
export type GuardChargeResult = z.infer<typeof guardChargeResultSchema>
export type GuardChargeHistoryItem = z.infer<
  typeof guardChargeHistoryItemSchema
>
export type GuardChargeDetail = z.infer<typeof guardChargeDetailSchema>
