export type PairingStatus = 'UNPAIRED' | 'CODE_ISSUED' | 'PAIRED'

export interface PairedGuardian {
  name: string
  phoneNumber: string
}
