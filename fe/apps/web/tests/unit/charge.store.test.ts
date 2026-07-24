import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useChargeStore } from '@/stores/charge.store'

describe('charge store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useRealTimers()
  })

  it('최근 사용 순 계좌의 첫 항목을 기본 선택한다', async () => {
    vi.useFakeTimers()
    const store = useChargeStore()
    const request = store.loadAccounts()

    await vi.advanceTimersByTimeAsync(150)
    await request

    expect(store.accounts).toHaveLength(2)
    expect(store.selectedAccount).toMatchObject({
      accountId: 7,
      bankCode: 'KB',
    })
  })

  it('빠른 입력 금액을 누적하고 초기화한다', () => {
    const store = useChargeStore()

    store.addAmount(10_000)
    store.addAmount(50_000)
    expect(store.amount).toBe(60_000)

    store.setAmount(0)
    expect(store.amount).toBe(0)
  })

  it('새 계좌를 등록하면 기본 충전 계좌로 선택한다', async () => {
    vi.useFakeTimers()
    const store = useChargeStore()
    const request = store.registerAccount({
      bankCode: 'WOORI',
      accountNumber: '1002123456789',
      accountPassword: '1004',
    })

    await vi.advanceTimersByTimeAsync(450)
    const registeredAccount = await request

    expect(registeredAccount?.bankName).toBe('우리은행')
    expect(store.selectedAccountId).toBe(registeredAccount?.accountId)
    expect(store.accounts[0]?.accountId).toBe(registeredAccount?.accountId)
  })

  it('충전 요청을 처리하고 완료 결과를 저장한다', async () => {
    vi.useFakeTimers()
    const store = useChargeStore()
    const accountsRequest = store.loadAccounts()
    await vi.advanceTimersByTimeAsync(150)
    await accountsRequest
    store.setAmount(50_000)

    const chargeRequest = store.charge()
    expect(store.processingStatus).toBe('pending')
    expect(store.canCharge).toBe(false)

    await vi.advanceTimersByTimeAsync(500)
    await chargeRequest

    expect(store.processingStatus).toBe('success')
    expect(store.result).toMatchObject({
      status: 'COMPLETED',
      chargedAmount: 50_000,
      balanceAfter: 150_000,
    })
  })
})
