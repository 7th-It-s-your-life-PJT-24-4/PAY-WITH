import { apiClient } from '@/api/client'
import {
  tokenResponseSchema,
  type LoginRequest,
  type Token,
} from '@/schemas/auth.schema'

export async function login(body: LoginRequest): Promise<Token> {
  const response = await apiClient.post(
    '/auth/login',
    tokenResponseSchema,
    body,
  )
  return response.data
}
