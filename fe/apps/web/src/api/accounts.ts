import { apiClient } from '@/api/client'
import {
  chargeAccountResponseSchema,
  chargeAccountsResponseSchema,
  type ChargeAccount,
  type RegisterChargeAccountRequest,
} from '@/schemas/charge.schema'

export async function getChargeAccounts(): Promise<ChargeAccount[]> {
  const response = await apiClient.get(
    '/accounts',
    chargeAccountsResponseSchema,
  )
  return response.data
}

export async function registerChargeAccount(
  body: RegisterChargeAccountRequest,
): Promise<ChargeAccount> {
  const response = await apiClient.post(
    '/accounts',
    chargeAccountResponseSchema,
    body,
  )
  return response.data
}
