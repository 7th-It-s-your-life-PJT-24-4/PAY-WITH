import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useTransferStore } from '@/stores/transfer.store'

describe('transfer store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useRealTimers()
  })

  it('선택한 수취인의 계좌 정보를 저장한다', () => {
    const store = useTransferStore()

    store.selectRecipient({
      id: 1,
      name: '김민수',
      bank: '국민은행',
      accountNumber: '432102-01-234567',
    })

    expect(store.recipient?.name).toBe('김민수')
    expect(store.bank).toBe('국민은행')
    expect(store.accountNumber).toBe('432102-01-234567')
  })

  it('잔액을 초과하지 않는 범위에서 송금 금액을 계산한다', () => {
    const store = useTransferStore()

    store.selectRecipient({
      id: 1,
      name: '김민수',
      bank: '국민은행',
      accountNumber: '432102-01-234567',
    })
    store.addAmount(50_000)

    expect(store.amount).toBe(50_000)
    expect(store.remainingBalance).toBe(1_200_000)
    expect(store.canTransfer).toBe(true)

    store.addAmount(2_000_000)

    expect(store.amount).toBe(store.balance)
    expect(store.remainingBalance).toBe(0)
  })

  it('송금 상태를 초기값으로 되돌린다', () => {
    const store = useTransferStore()

    store.accountNumber = '1234567890'
    store.bank = '우리은행'
    store.amount = 100_000
    store.memo = '생활비'
    store.reset()

    expect(store.recipient).toBeNull()
    expect(store.accountNumber).toBe('')
    expect(store.bank).toBe('')
    expect(store.amount).toBe(0)
    expect(store.memo).toBe('')
    expect(store.balance).toBe(1_250_000)
  })

  it('비밀번호 입력 후 Mock 송금 처리 상태를 갱신한다', async () => {
    vi.useFakeTimers()
    const store = useTransferStore()

    const request = store.beginMockTransfer('123456')

    expect(store.processingStatus).toBe('pending')
    await vi.advanceTimersByTimeAsync(500)
    await request

    expect(store.processingStatus).toBe('success')
  })
})
