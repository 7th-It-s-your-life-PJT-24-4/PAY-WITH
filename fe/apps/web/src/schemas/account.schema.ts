import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const accountSchema = z.object({
  accountId: z.number().int().positive(),
  bankCode: z.string().regex(/^\d{3}$/),
  bankName: z.string().min(1),
  accountNo: z.string().min(1),
})

export const accountsSchema = z.array(accountSchema)
export const accountsResponseSchema = apiResponseSchema(accountsSchema)
export const accountResponseSchema = apiResponseSchema(accountSchema)

export const registerAccountRequestSchema = z.object({
  bankCode: z.string().regex(/^\d{3}$/),
  accountNo: z.string().regex(/^\d{8,16}$/),
  accountPassword: z.string().regex(/^\d{4}$/),
})

export type Account = z.infer<typeof accountSchema>
export type RegisterAccountRequest = z.infer<
  typeof registerAccountRequestSchema
>
