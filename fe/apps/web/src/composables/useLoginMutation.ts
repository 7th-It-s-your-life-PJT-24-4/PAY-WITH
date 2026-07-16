import { useMutation } from '@tanstack/vue-query'

import { login } from '@/api/auth'
import { tokenStorage } from '@/api/token-storage'

export function useLoginMutation() {
  return useMutation({
    mutationFn: login,
    onSuccess: (token) => {
      tokenStorage.setTokens(token.accessToken, token.refreshToken)
    },
  })
}
