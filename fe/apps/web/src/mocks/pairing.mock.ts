import type {
  GuardianPairingCodeResponse,
  PairingErrorCode,
  WardPairingRequest,
  WardPairingResponse,
} from '@/types/pairing'

export const mockPairingCode = '72941'
const mockDelay = 300
const pairingCodeLifetime = 5 * 60 * 1000

export const mockActivePairing: WardPairingResponse = {
  pairingId: 21,
  guardianId: 7,
  guardianName: '김철수',
  status: 'ACTIVE',
  pairedAt: '2026-07-16T15:20:00+09:00',
}

export class MockPairingError extends Error {
  readonly code: PairingErrorCode

  constructor(code: PairingErrorCode, message: string) {
    super(message)
    this.name = 'MockPairingError'
    this.code = code
  }
}

function waitForMockResponse() {
  return new Promise<void>((resolve) =>
    globalThis.setTimeout(resolve, mockDelay),
  )
}

export async function issueMockGuardianPairingCode(): Promise<GuardianPairingCodeResponse> {
  await waitForMockResponse()

  return {
    code: mockPairingCode,
    inviteUrl: `https://paywith.link/${mockPairingCode}`,
    expiresAt: new Date(Date.now() + pairingCodeLifetime).toISOString(),
  }
}

export async function submitMockWardPairing(
  request: WardPairingRequest,
): Promise<WardPairingResponse> {
  await waitForMockResponse()

  if (!/^\d{5}$/.test(request.pairingCode)) {
    throw new MockPairingError(
      'PAIRING_001',
      '인증 코드는 숫자 5자리여야 합니다.',
    )
  }

  if (request.pairingCode !== mockPairingCode) {
    throw new MockPairingError(
      'PAIRING_002',
      '인증 코드가 유효하지 않거나 만료되었습니다.',
    )
  }

  return structuredClone(mockActivePairing)
}
