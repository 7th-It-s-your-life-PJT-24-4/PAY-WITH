import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { usePairingStore } from '@/stores/pairing.store'

describe('pairing store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('보호자 인증 코드를 생성한다', () => {
    const store = usePairingStore()

    store.issueCode()

    expect(store.status).toBe('CODE_ISSUED')
    expect(store.code).toBe('72941')
    expect(store.expiresAt).toBeTypeOf('number')
  })

  it('올바른 코드로 보호자 연결을 완료한다', () => {
    const store = usePairingStore()

    expect(store.verifyCode('00000')).toBe(false)
    expect(store.isPaired).toBe(false)

    expect(store.verifyCode('72941')).toBe(true)
    expect(store.isPaired).toBe(true)
    expect(store.guardian).toMatchObject({ name: '김철수' })
  })
})
