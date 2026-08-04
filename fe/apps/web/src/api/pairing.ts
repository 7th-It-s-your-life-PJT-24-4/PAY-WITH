import { apiClient } from '@/api/client'
import {
  guardianPairingCodeResponseSchema,
  wardPairingRequestSchema,
  wardPairingResponseSchema,
  type GuardianPairingCode,
  type WardPairing,
  type WardPairingRequest,
} from '@/schemas/pairing.schema'

export async function issueGuardianPairingCode(): Promise<GuardianPairingCode> {
  const response = await apiClient.post(
    '/guard/pairing',
    guardianPairingCodeResponseSchema,
    undefined,
  )

  return response.data
}

export async function pairWardWithGuardian(
  body: WardPairingRequest,
): Promise<WardPairing> {
  const request = wardPairingRequestSchema.parse(body)
  const response = await apiClient.post(
    '/ward/pairing',
    wardPairingResponseSchema,
    request,
  )

  return response.data
}
