import { apiClient } from '@/api/client'
import {
  guardTransactionDetailResponseSchema,
  type GuardTransactionDetail,
} from '@/schemas/transaction.schema'

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
