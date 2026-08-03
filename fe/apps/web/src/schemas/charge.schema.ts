import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

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

export type CreateChargeRequest = z.infer<typeof createChargeRequestSchema>
export type ChargeResult = z.infer<typeof chargeResultSchema>
