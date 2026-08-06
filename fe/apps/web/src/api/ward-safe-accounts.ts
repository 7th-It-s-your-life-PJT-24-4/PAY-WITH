import { apiClient } from '@/api/client'
import {
  wardSafeAccountListResponseSchema,
  type WardSafeAccount,
} from '@/schemas/ward-safe-account.schema'

export async function getWardSafeAccounts(): Promise<WardSafeAccount[]> {
  const response = await apiClient.get(
    '/ward/safe-accounts',
    wardSafeAccountListResponseSchema,
  )
  return response.data.safeAccounts
}
