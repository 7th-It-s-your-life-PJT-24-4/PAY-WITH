import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const walletBalanceSchema = z.object({
  walletId: z.number().int().positive(),
  balance: z.number().int().nonnegative(),
  updatedAt: z.string().min(1),
})

export const walletBalanceResponseSchema =
  apiResponseSchema(walletBalanceSchema)

export type WalletBalance = z.infer<typeof walletBalanceSchema>
