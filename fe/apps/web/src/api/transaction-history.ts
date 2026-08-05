import { apiClient } from '@/api/client'
import {
  guardTransactionHistoryResponseSchema,
  wardTransactionHistoryResponseSchema,
  type GuardTransactionHistory,
  type TransactionHistoryType,
  type TransactionRiskLevel,
  type WardTransactionHistory,
} from '@/schemas/transaction-history.schema'

export interface WardTransactionHistoryParams {
  category?: 'ALL' | TransactionHistoryType
  keyword?: string
  page?: number
  size?: number
}

export interface GuardTransactionHistoryParams {
  type?: TransactionHistoryType
  riskLevel?: TransactionRiskLevel
  page?: number
  size?: number
}

function createSearchParams(params: object) {
  const searchParams = new URLSearchParams()

  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== '')
      searchParams.set(key, String(value))
  }

  const query = searchParams.toString()
  return query ? `?${query}` : ''
}

export async function getWardTransactionHistory(
  params: WardTransactionHistoryParams = {},
): Promise<WardTransactionHistory> {
  const query = createSearchParams(params)
  const response = await apiClient.get(
    `/ward/transactions${query}`,
    wardTransactionHistoryResponseSchema,
  )
  return response.data
}

export async function getGuardTransactionHistory(
  wardId: number,
  params: GuardTransactionHistoryParams = {},
): Promise<GuardTransactionHistory> {
  const query = createSearchParams(params)
  const response = await apiClient.get(
    `/guard/wards/${wardId}/transactions${query}`,
    guardTransactionHistoryResponseSchema,
  )
  return response.data
}
