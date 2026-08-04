import { apiClient } from '@/api/client'
import {
  walletBalanceResponseSchema,
  type WalletBalance,
} from '@/schemas/wallet.schema'

export async function getWardWallet(): Promise<WalletBalance> {
  const response = await apiClient.get(
    '/ward/wallet',
    walletBalanceResponseSchema,
  )
  return response.data
}
