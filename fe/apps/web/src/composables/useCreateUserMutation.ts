import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { createUser } from '@/api/users'

export function useCreateUserMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  })
}
