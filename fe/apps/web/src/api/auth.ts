import { apiClient } from '@/api/client'
import {
  loginRequestSchema,
  phoneCodeRequestSchema,
  phoneCodeResponseSchema,
  phoneVerifyRequestSchema,
  phoneVerifyResponseSchema,
  tokenResponseSchema,
  type LoginRequest,
  type PhoneCodeRequest,
  type PhoneCodeResponse,
  type PhoneVerifyRequest,
  type PhoneVerifyResponse,
  type Token,
} from '@/schemas/auth.schema'

export async function login(body: LoginRequest): Promise<Token> {
  const request = loginRequestSchema.parse(body)
  const response = await apiClient.post(
    '/auth/login',
    tokenResponseSchema,
    request,
  )
  return response.data
}

export async function sendPhoneCode(
  body: PhoneCodeRequest,
): Promise<PhoneCodeResponse> {
  const request = phoneCodeRequestSchema.parse(body)
  const response = await apiClient.post(
    '/auth/phone/code',
    phoneCodeResponseSchema,
    request,
  )
  return response.data
}

export async function verifyPhoneCode(
  body: PhoneVerifyRequest,
): Promise<PhoneVerifyResponse> {
  const request = phoneVerifyRequestSchema.parse(body)
  const response = await apiClient.post(
    '/auth/phone/verify',
    phoneVerifyResponseSchema,
    request,
  )
  return response.data
}
