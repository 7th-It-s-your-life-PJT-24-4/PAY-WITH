import { queryOptions } from '@tanstack/vue-query'

import {
  getGuardChargeDetail,
  getGuardChargeHistories,
} from '@/api/guard-charges'

export const guardChargeKeys = {
  all: ['guard-charges'] as const,
  detail: (chargeId: number) => [...guardChargeKeys.all, chargeId] as const,
}

export function guardChargeHistoriesOptions() {
  return queryOptions({
    queryKey: guardChargeKeys.all,
    queryFn: getGuardChargeHistories,
  })
}

export function guardChargeDetailOptions(chargeId: number) {
  return queryOptions({
    queryKey: guardChargeKeys.detail(chargeId),
    queryFn: () => getGuardChargeDetail(chargeId),
    enabled: Number.isSafeInteger(chargeId) && chargeId > 0,
  })
}
