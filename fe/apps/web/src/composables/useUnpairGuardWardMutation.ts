import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { unpairGuardianWard } from '@/api/pairing'
import { guardHomeKeys } from '@/lib/query/guard/home'

export function useUnpairGuardWardMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: unpairGuardianWard,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: guardHomeKeys.all }),
  })
}
