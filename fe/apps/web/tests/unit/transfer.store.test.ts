import { createPinia, setActivePinia } from 'pinia'
import { HTTPError } from 'ky'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useTransferStore } from '@/stores/transfer.store'

describe('transfer store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sessionStorage.clear()
    vi.useRealTimers()
  })

  function prepareTransfer() {
    const store = useTransferStore()
    store.setBalance(1_250_000)
    store.selectRecipient({
      id: 1,
      name: '김민수',
      bankCode: '004',
      bank: '국민은행',
      accountNumber: '432102-01-234567',
    })
    store.amount = 50_000
    return store
  }

  function createTransferError(status: number, message: string) {
    const request = new Request('http://localhost/api/ward/transfers')
    const response = new Response(null, { status })
    const error = new HTTPError(response, request, {} as never)
    error.data = { success: false, data: null, code: null, message }
    return error
  }

  it('선택한 수취인의 계좌 정보를 저장한다', () => {
    const store = useTransferStore()

    store.selectRecipient({
      id: 1,
      name: '김민수',
      bankCode: '004',
      bank: '국민은행',
      accountNumber: '432102-01-234567',
    })

    expect(store.recipient?.name).toBe('김민수')
    expect(store.bank).toBe('국민은행')
    expect(store.accountNumber).toBe('432102-01-234567')
  })

  it('검증된 수취인 정보(setVerifiedRecipient)를 올바르게 저장한다', () => {
    const store = useTransferStore()

    store.setVerifiedRecipient('홍길동', 'NH농협은행', '011')

    expect(store.recipient?.name).toBe('홍길동')
    expect(store.bank).toBe('NH농협은행')
    expect(store.recipient?.bankCode).toBe('011')
  })

  it('빠른 금액 입력이 잔액을 초과해도 입력값을 유지한다', () => {
    const store = useTransferStore()
    store.setBalance(1_250_000)

    store.selectRecipient({
      id: 1,
      name: '김민수',
      bankCode: '004',
      bank: '국민은행',
      accountNumber: '432102-01-234567',
    })
    store.addAmount(50_000)

    expect(store.amount).toBe(50_000)
    expect(store.remainingBalance).toBe(1_200_000)
    expect(store.canTransfer).toBe(true)

    store.addAmount(2_000_000)

    expect(store.amount).toBe(2_050_000)
    expect(store.remainingBalance).toBe(-800_000)
    expect(store.isAmountOverBalance).toBe(true)
    expect(store.canTransfer).toBe(false)
  })

  it('키패드 입력이 잔액을 초과해도 입력값을 유지한다', () => {
    const store = useTransferStore()
    store.setBalance(1_250_000)

    for (const digit of '2000000') store.appendAmountDigit(digit)

    expect(store.amount).toBe(2_000_000)
    expect(store.isAmountOverBalance).toBe(true)
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
    expect(store.balance).toBeNull()
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

  it('실제 송금 결과를 거래 ID로 복원한다', async () => {
    const store = prepareTransfer()
    store.createTransferIntent('restore-key')
    await store.beginTransfer(
      '123456',
      vi.fn().mockResolvedValue({
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
      }),
    )

    setActivePinia(createPinia())
    const restoredStore = useTransferStore()

    expect(restoredStore.restoreTransferDetail(73)).toMatchObject({
      transactionId: 73,
      status: 'COMPLETED',
      holderName: '김민수',
    })
  })

  it('손상된 송금 결과는 제거하고 복원하지 않는다', () => {
    sessionStorage.setItem('pay-with:ward-transfer:73', '{invalid')
    const store = useTransferStore()

    expect(store.restoreTransferDetail(73)).toBeNull()
    expect(sessionStorage.getItem('pay-with:ward-transfer:73')).toBeNull()
  })

  it('초기화할 때 현재 송금 결과를 storage에서 제거한다', async () => {
    const store = prepareTransfer()
    store.createTransferIntent('reset-key')
    await store.beginTransfer(
      '222222',
      vi.fn().mockResolvedValue({ transactionId: 74, status: 'HELD' }),
    )

    expect(sessionStorage.getItem('pay-with:ward-transfer:74')).not.toBeNull()
    store.reset()
    expect(sessionStorage.getItem('pay-with:ward-transfer:74')).toBeNull()
  })

  it('같은 송금 내용에는 기존 요청 식별자를 재사용한다', () => {
    const store = prepareTransfer()

    const first = store.createTransferIntent('first-key')
    const second = store.createTransferIntent('second-key')

    expect(first?.idempotencyKey).toBe('first-key')
    expect(second?.idempotencyKey).toBe('first-key')
  })

  it('API 수취인의 은행 코드를 표시용 은행명보다 우선 사용한다', () => {
    const store = useTransferStore()
    store.setBalance(1_250_000)
    store.selectRecipient({
      id: 1,
      name: '김민수',
      bankCode: '004',
      bank: '국민',
      accountNumber: '43210201234567',
    })
    store.amount = 50_000

    const intent = store.createTransferIntent('api-bank-code')

    expect(intent?.bankCode).toBe('004')
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

  it('비밀번호 오류에는 기존 요청 식별자를 유지하고 비밀번호 재입력을 허용한다', async () => {
    const store = prepareTransfer()
    store.createTransferIntent('pin-error-key')

    await store.beginTransfer(
      '000000',
      vi
        .fn()
        .mockRejectedValue(
          createTransferError(400, '송금 비밀번호가 올바르지 않습니다.'),
        ),
    )

    expect(store.processingStatus).toBe('error')
    expect(store.processingFailureAction).toBe('retry-pin')
    expect(store.transferIntent?.idempotencyKey).toBe('pin-error-key')
    expect(store.requestStarted).toBe(false)
  })

  it('잔액 부족 오류에는 충전 행동을 제공한다', async () => {
    const store = prepareTransfer()
    store.createTransferIntent('balance-error-key')

    await store.beginTransfer(
      '123456',
      vi
        .fn()
        .mockRejectedValue(
          createTransferError(422, '송금 가능한 잔액이 부족합니다.'),
        ),
    )

    expect(store.processingFailureAction).toBe('charge')
  })

  it('처리 중 실패에는 새 송금 재시도를 허용하지 않는다', async () => {
    const store = prepareTransfer()
    store.createTransferIntent('irrecoverable-key')

    await store.beginTransfer(
      '123456',
      vi
        .fn()
        .mockRejectedValue(
          createTransferError(
            409,
            '이전 요청이 처리 중 실패했습니다. 잔액을 확인 후 고객센터로 문의해주세요.',
          ),
        ),
    )

    expect(store.processingStatus).toBe('error')
    expect(store.processingFailureAction).toBe('go-home')
    expect(store.transferIntent?.idempotencyKey).toBe('irrecoverable-key')
  })
})
