import { apiClient } from '@/api/client'
import {
  guardianPairingCodeResponseSchema,
  pairingRequestResponseSchema,
  pairingStatusResponseSchema,
  unpairWardResponseSchema,
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

export async function createWardPairingRequest(
  body: WardPairingRequest,
): Promise<{ requestId: string }> {
  const request = wardPairingRequestSchema.parse(body)
  const response = await apiClient.post(
    '/ward/pairing/request',
    pairingRequestResponseSchema,
    request,
  )
  return response.data
}

export async function getWardPairingRequestStatus(requestId: string) {
  const response = await apiClient.get(
    `/ward/pairing/request/${requestId}/status`,
    pairingStatusResponseSchema,
  )
  return response.data
}

export async function unpairGuardianWard(wardId: number): Promise<void> {
  await apiClient.delete(`/guard/pairing/${wardId}`, unpairWardResponseSchema)
}
