import { apiClient } from '@/api/client'
import {
  bankFilterRequestSchema,
  bankFilterResponseSchema,
  banksResponseSchema,
  type Bank,
  type BankFilterRequest,
} from '@/schemas/bank.schema'

export async function getBanks(): Promise<Bank[]> {
  const response = await apiClient.get('/banks', banksResponseSchema)
  return response.data
}

export async function filterBanks(request: BankFilterRequest): Promise<Bank[]> {
  const body = bankFilterRequestSchema.parse(request)
  const response = await apiClient.post(
    '/ward/filter-bank',
    bankFilterResponseSchema,
    body,
  )
  return response.data.banks
}
