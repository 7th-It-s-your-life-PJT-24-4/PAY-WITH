import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const pairingStatusSchema = z.enum(['UNPAIRED', 'CODE_ISSUED', 'PAIRED'])

export const pairingErrorCodeSchema = z.enum([
  'PAIRING_001',
  'PAIRING_002',
  'PAIRING_003',
  'PAIRING_004',
  'PAIRING_005',
])

export const guardianPairingCodeSchema = z.object({
  code: z.string().regex(/^\d{5}$/),
  inviteUrl: z.string().url(),
  expiresAt: z.string().min(1),
})

export const wardPairingRequestSchema = z.object({
  pairingCode: z
    .string()
    .regex(/^\d{5}$/, '인증 코드는 숫자 5자리여야 합니다.'),
})

export const wardPairingSchema = z.object({
  relationId: z.number().int().positive(),
  guardId: z.number().int().positive(),
  guardName: z.string().min(1),
  status: z.literal('ACTIVE'),
  connectedAt: z.string().min(1),
})

export const guardianPairingCodeResponseSchema = apiResponseSchema(
  guardianPairingCodeSchema,
)
export const wardPairingResponseSchema = apiResponseSchema(wardPairingSchema)
export const unpairWardResponseSchema = apiResponseSchema(z.null())

export type PairingStatus = z.infer<typeof pairingStatusSchema>
export type PairingErrorCode = z.infer<typeof pairingErrorCodeSchema>
export type GuardianPairingCode = z.infer<typeof guardianPairingCodeSchema>
export type WardPairingRequest = z.infer<typeof wardPairingRequestSchema>
export type WardPairing = z.infer<typeof wardPairingSchema>
