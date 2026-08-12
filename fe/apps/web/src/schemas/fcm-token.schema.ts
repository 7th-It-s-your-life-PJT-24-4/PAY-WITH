import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const fcmTokenRequestSchema = z.object({
  fcmToken: z.string().trim().min(1).max(255),
})

export const fcmTokenResponseSchema = apiResponseSchema(z.null())

export type FcmTokenRequest = z.infer<typeof fcmTokenRequestSchema>
