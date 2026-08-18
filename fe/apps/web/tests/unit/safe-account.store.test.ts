import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useSafeAccountStore } from '@/stores/safe-account.store'

describe('safe account store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('유효한 은행 코드와 숫자 계좌번호일 때 등록 초안을 만든다', () => {
    const store = useSafeAccountStore()

    expect(store.registrationDraft).toBeNull()

    store.selectBank({ code: '092', name: '토스뱅크' })
    store.setAccountNumber('12-34a')
    expect(store.bankName).toBe('토스뱅크')
    expect(store.accountNumber).toBe('1234')
    expect(store.registrationDraft).toEqual({
      bankCode: '092',
      accountNo: '1234',
    })
  })

  it('초기화하면 등록 가능한 계좌 초안도 제거한다', () => {
    const store = useSafeAccountStore()
    store.selectBank({ code: '092', name: '토스뱅크' })
    store.setAccountNumber('1234')

    store.reset()
    expect(store.bankName).toBe('')
    expect(store.accountNumber).toBe('')
    expect(store.registrationDraft).toBeNull()
  })
})
