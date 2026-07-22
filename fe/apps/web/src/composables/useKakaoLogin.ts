const KAKAO_AUTHORIZE_URL = 'https://kauth.kakao.com/oauth/authorize'

export function getKakaoRedirectUri() {
  return (
    import.meta.env.VITE_KAKAO_REDIRECT_URI ??
    `${globalThis.location.origin}/auth/kakao/callback`
  )
}

export function startKakaoLogin() {
  const clientId = import.meta.env.VITE_KAKAO_REST_API_KEY

  if (!clientId) {
    throw new Error('VITE_KAKAO_REST_API_KEY가 설정되지 않았습니다.')
  }

  const authorizationUrl = new URL(KAKAO_AUTHORIZE_URL)
  authorizationUrl.searchParams.set('client_id', clientId)
  authorizationUrl.searchParams.set('redirect_uri', getKakaoRedirectUri())
  authorizationUrl.searchParams.set('response_type', 'code')

  globalThis.location.assign(authorizationUrl)
}
