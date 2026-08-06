import { apiClient } from '@/api/client'
import {
  registerWardSafeAccountRequestSchema,
  wardSafeAccountListResponseSchema,
  wardSafeAccountResponseSchema,
  type RegisterWardSafeAccountRequest,
  type WardSafeAccount,
} from '@/schemas/ward-safe-account.schema'

export async function getWardSafeAccounts(): Promise<WardSafeAccount[]> {
  const response = await apiClient.get(
    '/ward/safe-accounts',
    wardSafeAccountListResponseSchema,
  )
  return response.data.safeAccounts
}

export async function registerWardSafeAccount(
  body: RegisterWardSafeAccountRequest,
): Promise<WardSafeAccount> {
  const request = registerWardSafeAccountRequestSchema.parse(body)
  const response = await apiClient.post(
    '/ward/safe-accounts',
    wardSafeAccountResponseSchema,
    request,
  )
  return response.data
}
