import { apiClient } from '@/api/client'
import {
  wardGuardianResponseSchema,
  type WardGuardian,
} from '@/schemas/ward-guardian.schema'

export async function getWardGuardian(): Promise<WardGuardian> {
  const response = await apiClient.get(
    '/ward/guardian',
    wardGuardianResponseSchema,
  )

  return response.data
}
