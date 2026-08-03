import { queryOptions } from '@tanstack/vue-query'

import { getChargeAccounts } from '@/api/accounts'

export const accountKeys = {
  all: ['accounts'] as const,
}

export function chargeAccountsOptions() {
  return queryOptions({
    queryKey: accountKeys.all,
    queryFn: getChargeAccounts,
  })
}
