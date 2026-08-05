import { apiClient } from '@/api/client'
import { banksResponseSchema, type Bank } from '@/schemas/bank.schema'

export async function getBanks(): Promise<Bank[]> {
  const response = await apiClient.get('/banks', banksResponseSchema)
  return response.data
}
