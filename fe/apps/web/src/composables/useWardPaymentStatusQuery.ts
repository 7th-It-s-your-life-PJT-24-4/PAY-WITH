import { useQuery } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import { getWardPaymentStatus } from '@/api/payments'
import type { PaymentStatus } from '@/schemas/payment.schema'

const pollingStatuses = new Set<PaymentStatus['status']>([
  'PENDING',
  'PROCESSING',
])

export function useWardPaymentStatusQuery(
  paymentId: MaybeRefOrGetter<number | null>,
) {
  const resolvedPaymentId = computed(() => toValue(paymentId))

  return useQuery({
    queryKey: computed(() => ['ward-payment', resolvedPaymentId.value]),
    queryFn: () => getWardPaymentStatus(resolvedPaymentId.value!),
    enabled: computed(() => resolvedPaymentId.value !== null),
    refetchInterval: (query) => {
      const payment = query.state.data as PaymentStatus | undefined
      return !payment || pollingStatuses.has(payment.status) ? 1_000 : false
    },
    refetchOnWindowFocus: true,
  })
}
