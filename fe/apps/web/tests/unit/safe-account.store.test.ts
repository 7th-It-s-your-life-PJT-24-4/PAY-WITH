import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useSafeAccountStore } from '@/stores/safe-account.store'

describe('safe account store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('계좌 정보를 숫자로 정리하고 완료 후 초기값으로 되돌린다', () => {
    const store = useSafeAccountStore()

    store.selectBank('토스뱅크')
    store.setAccountNumber('12-34a')
    expect(store.bankName).toBe('토스뱅크')
    expect(store.accountNumber).toBe('1234')

    store.reset()
    expect(store.bankName).toBe('')
    expect(store.accountNumber).toBe('')
  })
})
