import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import { getWardPaymentStatus } from '@/api/payments'
import type { PaymentStatus } from '@/schemas/payment.schema'

const pollingStatuses = new Set<PaymentStatus['status']>([
  'PENDING',
  'PROCESSING',
])

export const wardPaymentKeys = {
  all: ['ward-payment'] as const,
  detail: (paymentId: number | null) =>
    [...wardPaymentKeys.all, paymentId] as const,
}

export function wardPaymentStatusOptions(
  paymentId: MaybeRefOrGetter<number | null>,
) {
  const resolvedPaymentId = computed(() => toValue(paymentId))

  return queryOptions({
    queryKey: computed(() => wardPaymentKeys.detail(resolvedPaymentId.value)),
    queryFn: () => getWardPaymentStatus(resolvedPaymentId.value!),
    enabled: computed(() => resolvedPaymentId.value !== null),
    refetchInterval: (query) => {
      if (query.state.error) return false
      const payment = query.state.data
      return !payment || pollingStatuses.has(payment.status) ? 1_000 : false
    },
    refetchOnWindowFocus: true,
  })
}
