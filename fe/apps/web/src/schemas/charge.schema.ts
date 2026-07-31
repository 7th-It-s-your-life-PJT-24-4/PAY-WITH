import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const chargeAccountSchema = z.object({
  accountId: z.number().int().positive(),
  bankCode: z.string().regex(/^\d{3}$/),
  bankName: z.string().min(1),
  accountNo: z.string().min(1),
})

export const chargeAccountsSchema = z.array(chargeAccountSchema)
export const chargeAccountsResponseSchema =
  apiResponseSchema(chargeAccountsSchema)
export const chargeAccountResponseSchema =
  apiResponseSchema(chargeAccountSchema)

export const registerChargeAccountRequestSchema = z.object({
  bankCode: z.string().regex(/^\d{3}$/),
  accountNo: z.string().regex(/^\d{8,16}$/),
  accountPassword: z.string().regex(/^\d{4}$/),
})

export const createChargeRequestSchema = z.object({
  accountId: z.number().int().positive(),
  amount: z.number().int().positive(),
})

export const chargeResultSchema = z.object({
  transactionId: z.number().int().positive(),
  chargeAmount: z.number().int().positive(),
  balanceAfter: z.number().int().nonnegative(),
  bankName: z.string().min(1),
  accountNo: z.string().min(1),
  createdAt: z.string().min(1),
})

export const chargeResultResponseSchema = apiResponseSchema(chargeResultSchema)

export type ChargeAccount = z.infer<typeof chargeAccountSchema>
export type RegisterChargeAccountRequest = z.infer<
  typeof registerChargeAccountRequestSchema
>
export type CreateChargeRequest = z.infer<typeof createChargeRequestSchema>
export type ChargeResult = z.infer<typeof chargeResultSchema>
