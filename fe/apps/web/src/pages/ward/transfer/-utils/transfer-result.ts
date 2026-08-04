import type { TransferStatus } from '@/types/transfer'

export interface TransferResultAction {
  label: string
  routeName: 'ward-charge' | 'ward-transfer'
}

export function getTransferResultAction(
  status: TransferStatus,
  failureCode: string | null,
): TransferResultAction | null {
  if (status === 'COMPLETED') return null

  if (status === 'FAILED' && failureCode === 'INSUFFICIENT_BALANCE')
    return { label: '충전하기', routeName: 'ward-charge' }

  return { label: '송금 다시하기', routeName: 'ward-transfer' }
}
