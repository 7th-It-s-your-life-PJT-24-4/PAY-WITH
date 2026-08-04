import { apiClient } from '@/api/client'
import {
  guardHomeResponseSchema,
  type GuardHome,
} from '@/schemas/guard-home.schema'

export async function getGuardHome(wardId?: number): Promise<GuardHome> {
  const query = wardId === undefined ? '' : `?wardId=${wardId}`
  const response = await apiClient.get(
    `/guard${query}`,
    guardHomeResponseSchema,
  )
  return response.data
}
