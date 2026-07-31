import type {
  GuardianPairingCodeResponse,
  PairingErrorCode,
  WardPairingRequest,
  WardPairingResponse,
} from '@/types/pairing'

export const mockPairingCode = '72941'
const mockDelay = 300
const pairingCodeLifetime = 5 * 60 * 1000
const validPairingCodes = new Set([mockPairingCode])

export const mockActivePairing: WardPairingResponse = {
  relationId: 21,
  guardId: 7,
  guardName: '김철수',
  status: 'ACTIVE',
  connectedAt: '2026-07-16T15:20:00+09:00',
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

  const code = String(Math.floor(10000 + Math.random() * 90000))
  validPairingCodes.add(code)

  return {
    code,
    inviteUrl: `https://paywith.link/${code}`,
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

  if (!validPairingCodes.has(request.pairingCode)) {
    throw new MockPairingError(
      'PAIRING_002',
      '인증 코드가 유효하지 않거나 만료되었습니다.',
    )
  }

  return structuredClone(mockActivePairing)
}
