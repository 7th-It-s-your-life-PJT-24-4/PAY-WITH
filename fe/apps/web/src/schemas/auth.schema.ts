import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const phoneNumberSchema = z
  .string()
  .transform((value) => value.replace(/\D/g, ''))
  .pipe(z.string().regex(/^01[016789]\d{7,8}$/, '휴대폰 번호를 확인해 주세요.'))

export const loginRequestSchema = z.object({
  phone: phoneNumberSchema,
  password: z.string().min(1),
})

export const refreshTokenRequestSchema = z.object({
  refreshToken: z.string().min(1),
})

export const phoneCodePurposeSchema = z.enum(['SIGNUP', 'PASSWORD_RESET'])

export const phoneCodeRequestSchema = z.object({
  phone: phoneNumberSchema,
  purpose: phoneCodePurposeSchema,
})

export const phoneVerifyRequestSchema = z.object({
  phone: phoneNumberSchema,
  code: z.string().regex(/^\d{6}$/, '인증번호 6자리를 입력해 주세요.'),
})

export const tokenSchema = z.object({
  accessToken: z.string().min(1),
  refreshToken: z.string().min(1),
  tokenType: z.literal('Bearer'),
})

export const tokenResponseSchema = apiResponseSchema(tokenSchema)
export const phoneCodeResponseSchema = apiResponseSchema(
  z.object({
    expireIn: z.number().int().positive(),
  }),
)
export const phoneVerifyResponseSchema = apiResponseSchema(
  z.object({
    verificationToken: z.string().min(1),
  }),
)

export type LoginRequest = z.infer<typeof loginRequestSchema>
export type RefreshTokenRequest = z.infer<typeof refreshTokenRequestSchema>
export type PhoneCodeRequest = z.infer<typeof phoneCodeRequestSchema>
export type PhoneCodePurpose = z.infer<typeof phoneCodePurposeSchema>
export type PhoneCodeResponse = z.infer<typeof phoneCodeResponseSchema>['data']
export type PhoneVerifyRequest = z.infer<typeof phoneVerifyRequestSchema>
export type PhoneVerifyResponse = z.infer<
  typeof phoneVerifyResponseSchema
>['data']
export type Token = z.infer<typeof tokenSchema>
