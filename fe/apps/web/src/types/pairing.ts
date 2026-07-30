export type PairingStatus = 'UNPAIRED' | 'CODE_ISSUED' | 'PAIRED'

export type PairingErrorCode =
  'PAIRING_001' | 'PAIRING_002' | 'PAIRING_003' | 'PAIRING_004'

export interface GuardianPairingCodeResponse {
  code: string
  inviteUrl: string
  expiresAt: string
}

export interface WardPairingRequest {
  pairingCode: string
}

export interface WardPairingResponse {
  relationId: number
  guardId: number
  guardName: string
  status: 'ACTIVE'
  connectedAt: string
}
