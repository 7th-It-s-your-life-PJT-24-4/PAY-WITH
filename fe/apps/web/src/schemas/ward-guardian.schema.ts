import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const wardGuardianSchema = z.object({
  name: z.string().min(1),
  phone: z.string().min(1),
  avatarId: z.number().int().min(1).max(6).nullable().optional(),
})

export const wardGuardianResponseSchema = apiResponseSchema(wardGuardianSchema)

export type WardGuardian = z.infer<typeof wardGuardianSchema>
