import { apiClient } from '@/api/client'
import {
  accountResponseSchema,
  accountsResponseSchema,
  registerAccountRequestSchema,
  type Account,
  type RegisterAccountRequest,
} from '@/schemas/account.schema'

export async function getChargeAccounts(): Promise<Account[]> {
  const response = await apiClient.get('/accounts', accountsResponseSchema)
  return response.data
}

export async function registerChargeAccount(
  body: RegisterAccountRequest,
): Promise<Account> {
  const request = registerAccountRequestSchema.parse(body)
  const response = await apiClient.post(
    '/accounts',
    accountResponseSchema,
    request,
  )
  return response.data
}
