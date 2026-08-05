import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const registerGuardSafeAccountRequestSchema = z.object({
  bankCode: z.string().regex(/^\d{3}$/),
  accountNo: z.string().regex(/^\d+$/),
  accountAlias: z.string().max(50).optional(),
})

export const guardSafeAccountSchema = z.object({
  safeAccountId: z.number().int().positive(),
  recipientId: z.number().int().positive().nullable(),
  bankCode: z.string().regex(/^\d{3}$/),
  bankName: z.string().min(1),
  accountNo: z.string().min(1),
  holderName: z.string().min(1),
  accountAlias: z.string().nullable(),
  isVerified: z.boolean(),
  status: z.literal('ACTIVE'),
  createdAt: z.string().min(1),
})

export const guardSafeAccountResponseSchema = apiResponseSchema(
  guardSafeAccountSchema,
)

export type RegisterGuardSafeAccountRequest = z.infer<
  typeof registerGuardSafeAccountRequestSchema
>
export type GuardSafeAccount = z.infer<typeof guardSafeAccountSchema>
