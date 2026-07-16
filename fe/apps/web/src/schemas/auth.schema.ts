import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const loginRequestSchema = z.object({
  email: z.email(),
  password: z.string().min(1),
})

export const refreshTokenRequestSchema = z.object({
  refreshToken: z.string().min(1),
})

export const tokenSchema = z.object({
  accessToken: z.string().min(1),
  refreshToken: z.string().min(1),
  tokenType: z.literal('Bearer'),
})

export const tokenResponseSchema = apiResponseSchema(tokenSchema)

export type LoginRequest = z.infer<typeof loginRequestSchema>
export type RefreshTokenRequest = z.infer<typeof refreshTokenRequestSchema>
export type Token = z.infer<typeof tokenSchema>
