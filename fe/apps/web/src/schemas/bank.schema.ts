import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const bankSchema = z.object({
  bankCode: z.string().regex(/^\d{3}$/),
  bankName: z.string().min(1),
})

export const banksResponseSchema = apiResponseSchema(z.array(bankSchema))

export const bankFilterRequestSchema = z.object({
  accountNo: z.string().regex(/^\d{1,20}$/),
})

export const bankFilterResponseSchema = apiResponseSchema(
  z.object({ banks: z.array(bankSchema) }),
)

export type Bank = z.infer<typeof bankSchema>
export type BankFilterRequest = z.infer<typeof bankFilterRequestSchema>
