import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useChargeStore } from '@/stores/charge.store'

const accounts = [
  {
    accountId: 7,
    bankCode: '004',
    bankName: 'KB국민은행',
    accountNo: '12345612123456',
  },
  {
    accountId: 9,
    bankCode: '011',
    bankName: 'NH농협은행',
    accountNo: '2345634234567',
  },
]

const chargeResult = {
  transactionId: 43,
  chargeAmount: 50_000,
  balanceAfter: 150_000,
  bankName: 'KB국민은행',
  accountNo: '12345612123456',
  createdAt: '2026-07-23T17:30:00',
}

describe('charge store', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
  })

  it('조회된 첫 계좌를 기본 충전 계좌로 선택한다', () => {
    const store = useChargeStore()

    store.syncAccounts(accounts)
    expect(store.selectedAccountId).toBe(7)

    store.selectAccount(9)
    store.syncAccounts(accounts)
    expect(store.selectedAccountId).toBe(9)
  })

  it('빠른 입력 금액을 누적하고 초기화한다', () => {
    const store = useChargeStore()

    store.addAmount(10_000)
    store.addAmount(50_000)
    expect(store.amount).toBe(60_000)

    store.setAmount(0)
    expect(store.amount).toBe(0)
  })

  it('새 계좌를 기본 충전 계좌로 저장한다', () => {
    const store = useChargeStore()

    store.saveRegisteredAccount(accounts[1]!)

    expect(store.registeredAccount).toEqual(accounts[1])
    expect(store.selectedAccountId).toBe(9)
  })

  it('완료 응답을 새 Pinia 인스턴스에서 복구한다', () => {
    const store = useChargeStore()
    store.saveResult(chargeResult)

    setActivePinia(createPinia())
    const restoredStore = useChargeStore()

    expect(restoredStore.restoreResult(43)).toEqual(chargeResult)
    expect(restoredStore.restoreResult(44)).toBeNull()
  })
})
