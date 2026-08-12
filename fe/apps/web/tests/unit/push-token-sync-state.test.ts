import { beforeEach, describe, expect, it } from 'vitest'

import {
  isPushTokenSessionSynced,
  markPushTokenSessionSynced,
  resetPushTokenSyncState,
} from '@/lib/push-token-sync-state'

describe('push token sync state', () => {
  beforeEach(() => {
    resetPushTokenSyncState()
  })

  it('등록한 사용자와 토큰 조합만 동기화 상태로 판단한다', () => {
    markPushTokenSessionSynced('7:device-token')

    expect(isPushTokenSessionSynced('7:device-token')).toBe(true)
    expect(isPushTokenSessionSynced('8:device-token')).toBe(false)
  })

  it('로그아웃 정리 후 같은 사용자와 토큰도 다시 등록할 수 있다', () => {
    markPushTokenSessionSynced('7:device-token')

    resetPushTokenSyncState()

    expect(isPushTokenSessionSynced('7:device-token')).toBe(false)
  })
})
