import { useMutation } from '@tanstack/vue-query'

import { login } from '@/api/auth'
import { markSessionActive } from '@/api/auth-session'
import { scheduleAccessTokenRefresh } from '@/api/token-refresh'
import { tokenStorage } from '@/api/token-storage'

export function useLoginMutation() {
  return useMutation({
    mutationFn: login,
    onSuccess: (token) => {
      tokenStorage.setTokens(token.accessToken, token.refreshToken)
      markSessionActive()
      scheduleAccessTokenRefresh()
    },
  })
}
