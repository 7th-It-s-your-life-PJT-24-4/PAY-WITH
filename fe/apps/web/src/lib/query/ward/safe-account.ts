import { queryOptions } from '@tanstack/vue-query'

import { getWardSafeAccounts } from '@/api/ward-safe-accounts'

export const wardSafeAccountKeys = {
  all: ['ward-safe-accounts'] as const,
  list: () => [...wardSafeAccountKeys.all, 'list'] as const,
}

export function wardSafeAccountsOptions() {
  return queryOptions({
    queryKey: wardSafeAccountKeys.list(),
    queryFn: getWardSafeAccounts,
  })
}
