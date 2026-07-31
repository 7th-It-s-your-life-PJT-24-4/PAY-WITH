import { useQuery } from '@tanstack/vue-query'

import { getChargeAccounts } from '@/api/accounts'

export const chargeAccountsQueryKey = ['accounts'] as const

export function useChargeAccountsQuery() {
  return useQuery({
    queryKey: chargeAccountsQueryKey,
    queryFn: getChargeAccounts,
  })
}
