import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

const bankCodeSchema = z.string().regex(/^\d{3}$/)

export const wardSafeAccountSchema = z.object({
  safeAccountId: z.number().int().positive(),
  recipientId: z.number().int().positive().nullable(),
  bankCode: bankCodeSchema,
  bankName: z.string().min(1),
  accountNo: z.string().min(1),
  holderName: z.string().min(1),
  accountAlias: z.string().nullable(),
  isVerified: z.boolean(),
  createdAt: z.string().min(1),
})

export const wardSafeAccountListResponseSchema = apiResponseSchema(
  z.object({ safeAccounts: z.array(wardSafeAccountSchema) }),
)

export type WardSafeAccount = z.infer<typeof wardSafeAccountSchema>
