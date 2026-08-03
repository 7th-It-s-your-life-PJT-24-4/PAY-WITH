import { apiClient } from '@/api/client'
import {
  createChargeRequestSchema,
  chargeResultResponseSchema,
  type ChargeResult,
  type CreateChargeRequest,
} from '@/schemas/charge.schema'

export async function createWardCharge(
  body: CreateChargeRequest,
): Promise<ChargeResult> {
  const request = createChargeRequestSchema.parse(body)
  const response = await apiClient.post(
    '/ward/charges',
    chargeResultResponseSchema,
    request,
  )
  return response.data
}
