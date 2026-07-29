import { useMutation } from '@tanstack/vue-query'

import { createWardPayment } from '@/api/payments'

export function useCreateWardPaymentMutation() {
  return useMutation({ mutationFn: createWardPayment })
}
