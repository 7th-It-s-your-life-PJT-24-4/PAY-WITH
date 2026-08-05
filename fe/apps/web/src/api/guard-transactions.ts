import { apiClient } from '@/api/client'
import {
  guardTransactionDetailResponseSchema,
  guardTransactionHistoryResponseSchema,
  type GuardTransactionDetail,
  type GuardTransactionHistory,
  type TransactionRiskLevel,
} from '@/schemas/guard-transaction.schema'

export async function getGuardTransactionHistory(
  wardId: number,
  riskLevel?: TransactionRiskLevel,
): Promise<GuardTransactionHistory> {
  const params = new URLSearchParams({ page: '0', size: '100' })
  if (riskLevel) params.set('riskLevel', riskLevel)

  const response = await apiClient.get(
    `/guard/wards/${wardId}/transactions?${params.toString()}`,
    guardTransactionHistoryResponseSchema,
  )
  return response.data
}

export async function getGuardTransactionDetail(
  wardId: number,
  transactionId: number,
): Promise<GuardTransactionDetail> {
  const response = await apiClient.get(
    `/guard/wards/${wardId}/transactions/${transactionId}`,
    guardTransactionDetailResponseSchema,
  )
  return response.data
}
