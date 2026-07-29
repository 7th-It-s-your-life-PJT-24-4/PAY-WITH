import { useMutation } from '@tanstack/vue-query'

import { cancelWardPayment } from '@/api/payments'

export function useCancelWardPaymentMutation() {
  return useMutation({ mutationFn: cancelWardPayment })
}
