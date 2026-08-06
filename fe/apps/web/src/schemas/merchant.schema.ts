import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const merchantSchema = z.object({
  merchantId: z.number().int().positive(),
  name: z.string().min(1),
  categoryCode: z.string().nullable(),
  region: z.string().nullable(),
})

export const merchantListSchema = z.object({
  merchants: z.array(merchantSchema),
})

export const merchantListResponseSchema = apiResponseSchema(merchantListSchema)

export type Merchant = z.infer<typeof merchantSchema>
