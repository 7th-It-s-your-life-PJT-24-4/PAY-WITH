import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { createWardCharge } from '@/api/charges'
import { accountKeys } from '@/lib/query/account'

export function useCreateWardChargeMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createWardCharge,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: accountKeys.all }),
  })
}
