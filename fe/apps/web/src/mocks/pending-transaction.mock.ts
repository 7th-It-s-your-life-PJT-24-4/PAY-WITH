import { getMockHeldTransfers } from '@/mocks/transfer.mock'
import type {
  PendingPaymentTransaction,
  PendingTransaction,
} from '@/types/pending-transaction'

const mockHeldPayment: PendingPaymentTransaction = {
  transactionId: 81,
  type: 'PAYMENT',
  status: 'HELD',
  merchantName: '우리동네마트',
  paymentMethod: 'QR 결제',
  amount: 32_000,
  requestedAt: '2026-07-24T15:10:00+09:00',
  expiredAt: '2026-07-24T15:20:00+09:00',
}

let isMockHeldPaymentActive = true

export function getMockPendingTransactions(): PendingTransaction[] {
  const transfer = getMockHeldTransfers().at(0)
  const pendingTransactions: PendingTransaction[] = []

  if (transfer?.expiredAt) {
    pendingTransactions.push({
      transactionId: transfer.transactionId,
      type: 'TRANSFER',
      status: 'HELD',
      holderName: transfer.holderName,
      bankName: transfer.bankName,
      accountNo: transfer.accountNo,
      amount: transfer.amount,
      requestedAt: transfer.requestedAt,
      expiredAt: transfer.expiredAt,
    })
  }

  if (isMockHeldPaymentActive)
    pendingTransactions.push(structuredClone(mockHeldPayment))
  return pendingTransactions
}

export function getMockPendingTransaction(transactionId: number) {
  return getMockPendingTransactions().find(
    (transaction) => transaction.transactionId === transactionId,
  )
}

export async function cancelMockHeldPayment(transactionId: number) {
  await new Promise<void>((resolve) => window.setTimeout(resolve, 500))
  if (
    !isMockHeldPaymentActive ||
    transactionId !== mockHeldPayment.transactionId
  )
    throw new Error('승인 대기 중인 결제를 찾을 수 없습니다.')

  isMockHeldPaymentActive = false
  return { transactionId, status: 'CANCELED' as const }
}
