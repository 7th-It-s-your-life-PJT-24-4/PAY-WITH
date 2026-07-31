import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useTransferStore } from '@/stores/transfer.store'

describe('transfer store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useRealTimers()
  })

  function prepareTransfer() {
    const store = useTransferStore()
    store.selectRecipient({
      id: 1,
      name: '김민수',
      bank: '국민은행',
      accountNumber: '432102-01-234567',
    })
    store.amount = 50_000
    return store
  }

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

  it('비밀번호 입력 후 실제 송금 응답을 저장한다', async () => {
    const store = prepareTransfer()
    store.createTransferIntent('550e8400-e29b-41d4-a716-446655440000')
    const execute = vi.fn().mockResolvedValue({
      transactionId: 73,
      status: 'COMPLETED',
      holderName: '김민수',
      bankCode: '004',
      bankName: 'KB국민은행',
      accountNo: '43210201234567',
      amount: 50_000,
      memo: null,
      completedAt: '2026-07-31T12:00:00',
      balanceAfter: 1_200_000,
    })

    await store.beginTransfer('123456', execute)

    expect(store.processingStatus).toBe('success')
    expect(store.transferResult?.transactionId).toBe(73)
    expect(store.transferDetail?.status).toBe('COMPLETED')
    expect(store.transferDetailSource).toBe('api')
    expect(execute).toHaveBeenCalledWith({
      request: expect.objectContaining({ transferPin: '123456' }),
      idempotencyKey: '550e8400-e29b-41d4-a716-446655440000',
    })
  })

  it('이상 거래가 감지되면 승인 대기 거래 정보를 저장한다', async () => {
    const store = prepareTransfer()
    store.createTransferIntent('held-key')
    const execute = vi.fn().mockResolvedValue({
      transactionId: 74,
      status: 'HELD',
    })

    await store.beginTransfer('222222', execute)

    expect(store.processingStatus).toBe('held')
    expect(store.transferResult?.status).toBe('HELD')
    expect(store.transferDetail).toMatchObject({
      transactionId: 74,
      holderName: '김민수',
      bankName: '국민은행',
      amount: 50_000,
    })
    expect(store.transferDetail?.status).toBe('HELD')
  })

  it('같은 송금 내용에는 기존 요청 식별자를 재사용한다', () => {
    const store = prepareTransfer()

    const first = store.createTransferIntent('first-key')
    const second = store.createTransferIntent('second-key')

    expect(first?.idempotencyKey).toBe('first-key')
    expect(second?.idempotencyKey).toBe('first-key')
  })

  it('송금 요청을 중복 실행하지 않는다', async () => {
    const store = prepareTransfer()
    store.createTransferIntent('transfer-key')
    let resolveTransfer: ((value: unknown) => void) | undefined
    const execute = vi.fn(
      () =>
        new Promise((resolve) => {
          resolveTransfer = resolve
        }),
    )

    const firstRequest = store.beginTransfer('123456', execute)
    const duplicateRequest = store.beginTransfer('123456', execute)

    expect(store.requestStarted).toBe(true)
    expect(execute).toHaveBeenCalledTimes(1)
    resolveTransfer?.({
      transactionId: 73,
      status: 'COMPLETED',
      holderName: '김민수',
      bankCode: '004',
      bankName: 'KB국민은행',
      accountNo: '43210201234567',
      amount: 50_000,
      memo: null,
      completedAt: '2026-07-31T12:00:00',
      balanceAfter: 1_200_000,
    })
    await Promise.all([firstRequest, duplicateRequest])

    expect(store.transferResult?.idempotencyKey).toBe('transfer-key')
  })

  it('실패 후 재시도에는 새 요청 식별자를 사용한다', () => {
    const store = prepareTransfer()
    store.createTransferIntent('failed-key')

    store.restartAfterFailure('retry-key')

    expect(store.transferIntent?.idempotencyKey).toBe('retry-key')
  })
})
