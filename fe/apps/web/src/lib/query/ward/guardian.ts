import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import { getApiErrorCode } from '@/api/error'
import { getWardGuardian } from '@/api/ward-guardian'

export const wardGuardianKeys = {
  all: ['ward-guardian'] as const,
}

export function wardGuardianQueryOptions(
  enabled: MaybeRefOrGetter<boolean> = true,
) {
  return queryOptions({
    queryKey: wardGuardianKeys.all,
    queryFn: getWardGuardian,
    enabled: computed(() => toValue(enabled)),
    retry: (failureCount, error) =>
      getApiErrorCode(error) === 'WARD_001' ? false : failureCount < 1,
  })
}
