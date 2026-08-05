import { queryOptions } from '@tanstack/vue-query'

import { getBanks } from '@/api/banks'

export const bankKeys = {
  all: ['banks'] as const,
}

export function banksOptions() {
  return queryOptions({
    queryKey: bankKeys.all,
    queryFn: getBanks,
    staleTime: 5 * 60_000,
  })
}
