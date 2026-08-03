import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { registerChargeAccount } from '@/api/accounts'
import { accountKeys } from '@/lib/query/account'
import type { Account } from '@/schemas/account.schema'

export function useRegisterChargeAccountMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: registerChargeAccount,
    onSuccess: (account) => {
      queryClient.setQueryData<Account[]>(accountKeys.all, (current = []) => [
        account,
        ...current.filter(({ accountId }) => accountId !== account.accountId),
      ])
      return queryClient.invalidateQueries({ queryKey: accountKeys.all })
    },
  })
}
