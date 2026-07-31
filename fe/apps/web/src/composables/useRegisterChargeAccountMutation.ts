import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { registerChargeAccount } from '@/api/accounts'
import { chargeAccountsQueryKey } from '@/composables/useChargeAccountsQuery'
import type { ChargeAccount } from '@/schemas/charge.schema'

export function useRegisterChargeAccountMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: registerChargeAccount,
    onSuccess: (account) => {
      queryClient.setQueryData<ChargeAccount[]>(
        chargeAccountsQueryKey,
        (current = []) => [
          account,
          ...current.filter(({ accountId }) => accountId !== account.accountId),
        ],
      )
      return queryClient.invalidateQueries({ queryKey: chargeAccountsQueryKey })
    },
  })
}
