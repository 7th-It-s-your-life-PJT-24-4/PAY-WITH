import { useQuery } from '@tanstack/vue-query'

import { chargeAccountsOptions } from '@/lib/query/account'

export function useChargeAccountsQuery() {
  return useQuery(chargeAccountsOptions())
}
