import { apiClient } from '@/api/client'
import {
  chargeResultResponseSchema,
  type ChargeResult,
  type CreateChargeRequest,
} from '@/schemas/charge.schema'

export async function createWardCharge(
  body: CreateChargeRequest,
): Promise<ChargeResult> {
  const response = await apiClient.post(
    '/ward/charges',
    chargeResultResponseSchema,
    body,
  )
  return response.data
}
