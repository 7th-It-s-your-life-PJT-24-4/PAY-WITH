import { apiClient } from '@/api/client'
import {
  createGuardChargeRequestSchema,
  guardChargeDetailResponseSchema,
  guardChargeHistoriesResponseSchema,
  guardChargeResultResponseSchema,
  type CreateGuardChargeRequest,
  type GuardChargeDetail,
  type GuardChargeHistoryItem,
  type GuardChargeResult,
} from '@/schemas/charge.schema'

export async function createGuardCharge(
  wardId: number,
  body: CreateGuardChargeRequest,
): Promise<GuardChargeResult> {
  const request = createGuardChargeRequestSchema.parse(body)
  const response = await apiClient.post(
    `/guard/wards/${wardId}/charges`,
    guardChargeResultResponseSchema,
    request,
  )

  return response.data
}

export async function getGuardChargeHistories(): Promise<
  GuardChargeHistoryItem[]
> {
  const response = await apiClient.get(
    '/guard/charges',
    guardChargeHistoriesResponseSchema,
  )

  return response.data.charges
}

export async function getGuardChargeDetail(
  chargeId: number,
): Promise<GuardChargeDetail> {
  const response = await apiClient.get(
    `/guard/charges/${chargeId}`,
    guardChargeDetailResponseSchema,
  )

  return response.data
}
