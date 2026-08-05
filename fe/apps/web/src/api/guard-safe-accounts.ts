import { apiClient } from '@/api/client'
import {
  guardSafeAccountResponseSchema,
  registerGuardSafeAccountRequestSchema,
  type GuardSafeAccount,
  type RegisterGuardSafeAccountRequest,
} from '@/schemas/guard-safe-account.schema'

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
