import { useQuery } from '@tanstack/vue-query'
import type { MaybeRefOrGetter } from 'vue'

import { wardPaymentStatusOptions } from '@/lib/query/ward/payment'

export function useWardPaymentStatusQuery(
  paymentId: MaybeRefOrGetter<number | null>,
) {
  return useQuery(wardPaymentStatusOptions(paymentId))
}
