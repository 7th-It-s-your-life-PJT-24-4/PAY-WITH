import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { createWardCharge } from '@/api/charges'
import { chargeAccountsQueryKey } from '@/composables/useChargeAccountsQuery'

export function useCreateWardChargeMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createWardCharge,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: chargeAccountsQueryKey }),
  })
}
