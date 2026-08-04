import { queryOptions } from '@tanstack/vue-query'

import { getWardWallet } from '@/api/wallet'

export const wardWalletKeys = {
  all: ['ward-wallet'] as const,
}

export function wardWalletOptions() {
  return queryOptions({
    queryKey: wardWalletKeys.all,
    queryFn: getWardWallet,
  })
}
