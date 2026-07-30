import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { usePairingStore } from '@/stores/pairing.store'

describe('pairing store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('보호자 인증 코드를 생성한다', async () => {
    const store = usePairingStore()

    await store.issueCode()

    expect(store.status).toBe('CODE_ISSUED')
    expect(store.code).toMatch(/^\d{5}$/)
    expect(store.inviteUrl).toBe(`https://paywith.link/${store.code}`)
    expect(store.expiresAt).toBeTypeOf('string')
  })

  it('올바른 코드로 보호자 연결을 완료한다', async () => {
    const store = usePairingStore()
    store.reset()

    expect(await store.verifyCode('00000')).toBe(false)
    expect(store.errorCode).toBe('PAIRING_002')
    expect(store.isPaired).toBe(false)

    expect(await store.verifyCode('72941')).toBe(true)
    expect(store.isPaired).toBe(true)
    expect(store.guardian).toMatchObject({ name: '김철수' })
    expect(store.pairingResult).toMatchObject({
      relationId: 21,
      guardId: 7,
      status: 'ACTIVE',
    })
  })
})
