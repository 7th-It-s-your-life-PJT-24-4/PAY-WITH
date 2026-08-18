import { apiClient } from '@/api/client'
import {
  guardianPairingCodeResponseSchema,
  pairingRequestResponseSchema,
  pairingStatusResponseApiSchema,
  pendingPairingRequestResponseSchema,
  unpairWardResponseSchema,
  wardPairingRequestSchema,
  wardPairingResponseSchema,
  type GuardianPairingCode,
  type PendingPairingRequest,
  type PairingRequestStatus,
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

export async function requestWardPairing(
  body: WardPairingRequest,
): Promise<string> {
  const request = wardPairingRequestSchema.parse(body)
  const response = await apiClient.post(
    '/ward/pairing/request',
    pairingRequestResponseSchema,
    request,
  )

  return response.data.requestId
}

export async function getWardPairingRequestStatus(
  requestId: string,
): Promise<PairingRequestStatus> {
  const response = await apiClient.get(
    `/ward/pairing/request/${encodeURIComponent(requestId)}/status`,
    pairingStatusResponseApiSchema,
  )

  return response.data.status
}

export async function getPendingPairingRequest(): Promise<PendingPairingRequest | null> {
  const response = await apiClient.get(
    '/guard/pairing/pending-request',
    pendingPairingRequestResponseSchema,
  )

  return response.data
}

export async function confirmPairingRequest(
  requestId: string,
): Promise<WardPairing> {
  const response = await apiClient.post(
    `/guard/pairing/request/${encodeURIComponent(requestId)}/confirm`,
    wardPairingResponseSchema,
    undefined,
  )

  return response.data
}

export async function unpairGuardianWard(wardId: number): Promise<void> {
  await apiClient.delete(`/guard/pairing/${wardId}`, unpairWardResponseSchema)
}
