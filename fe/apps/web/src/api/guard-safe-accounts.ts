import { apiClient } from '@/api/client'
import {
  guardSafeAccountListResponseSchema,
  guardSafeAccountResponseSchema,
  registerGuardSafeAccountRequestSchema,
  type GuardSafeAccount,
  type GuardSafeAccountListItem,
  type RegisterGuardSafeAccountRequest,
} from '@/schemas/guard-safe-account.schema'

export async function getGuardSafeAccounts(
  wardId: number,
): Promise<GuardSafeAccountListItem[]> {
  const response = await apiClient.get(
    `/guard/wards/${wardId}/safe-accounts`,
    guardSafeAccountListResponseSchema,
  )

  return response.data.safeAccounts
}

export async function registerGuardSafeAccount(
  wardId: number,
  body: RegisterGuardSafeAccountRequest,
): Promise<GuardSafeAccount> {
  const request = registerGuardSafeAccountRequestSchema.parse(body)
  const response = await apiClient.post(
    `/guard/wards/${wardId}/safe-accounts`,
    guardSafeAccountResponseSchema,
    request,
  )

  return response.data
}
