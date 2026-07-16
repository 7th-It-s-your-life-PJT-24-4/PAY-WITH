import { useMutation, useQueryClient } from '@tanstack/vue-query'

import { deleteUser } from '@/api/users'

export function useDeleteUserMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deleteUser,
    onSuccess: (_, id) => {
      queryClient.removeQueries({ queryKey: ['users', id] })
      void queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })
}
