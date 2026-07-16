import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { updateUser } from '@/api/users'
import type { UpdateUserRequest } from '@/schemas/user.schema'

type UpdateUserVariables = {
  id: number
  body: UpdateUserRequest
}

export function useUpdateUserMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, body }: UpdateUserVariables) => updateUser(id, body),
    onSuccess: (user) => {
      queryClient.setQueryData(['users', user.id], user)
      void queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })
}
