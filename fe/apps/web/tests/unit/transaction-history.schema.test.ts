import { describe, expect, it } from 'vitest'

import {
  wardTransactionDetailResponseSchema,
  wardTransactionHistoryResponseSchema,
} from '@/schemas/transaction-history.schema'

describe('transaction history schemas', () => {
  it('위험 평가가 없는 충전 내역을 파싱한다', () => {
    const response = wardTransactionHistoryResponseSchema.parse({
      success: true,
      data: {
        transactions: [
          {
            transactionId: 1,
            type: 'CHARGE',
            direction: 'IN',
            title: 'KB국민은행',
            amount: 50_000,
            status: 'COMPLETED',
            riskLevel: null,
            occurredAt: '2026-08-05T09:30:00',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
      },
      message: null,
    })

    expect(response.data.transactions[0]?.riskLevel).toBeNull()
  })

  it('거래 내역 제목이 없으면 기본 문구로 파싱한다', () => {
    const response = wardTransactionHistoryResponseSchema.parse({
      success: true,
      data: {
        transactions: [
          {
            transactionId: 2,
            type: 'TRANSFER',
            direction: 'OUT',
            title: null,
            amount: 10_000,
            status: 'COMPLETED',
            riskLevel: 'SAFE',
            occurredAt: '2026-08-05T09:30:00',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
      },
      message: null,
    })

    expect(response.data.transactions[0]?.title).toBe('거래 상대')
  })

  it('실패한 거래의 음수 금액(또는 0원) 내역을 정상적으로 파싱한다', () => {
    const response = wardTransactionHistoryResponseSchema.parse({
      success: true,
      data: {
        transactions: [
          {
            transactionId: 10,
            type: 'TRANSFER',
            direction: 'OUT',
            title: '홍길동',
            amount: -320,
            status: 'FAILED',
            riskLevel: null,
            occurredAt: '2026-08-06T09:48:05',
          },
          {
            transactionId: 11,
            type: 'TRANSFER',
            direction: 'OUT',
            title: '홍길동',
            amount: 0,
            status: 'FAILED',
            riskLevel: null,
            occurredAt: '2026-08-06T09:48:15',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
        hasNext: false,
      },
      message: null,
    })

    expect(response.data.transactions[0]?.amount).toBe(-320)
    expect(response.data.transactions[1]?.amount).toBe(0)
  })

  it('최신 BE 거래 enum 값을 그대로 파싱한다', () => {
    const response = wardTransactionHistoryResponseSchema.parse({
      success: true,
      data: {
        transactions: [
          {
            transactionId: 3,
            type: 'TRANSFER',
            direction: 'OUT',
            title: '김철수',
            amount: 20_000,
            status: 'REQUESTED',
            riskLevel: null,
            occurredAt: '2026-08-05T09:30:00',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
      },
      message: null,
    })

    expect(response.data.transactions[0]?.type).toBe('TRANSFER')
    expect(response.data.transactions[0]?.status).toBe('REQUESTED')
    expect(response.data.transactions[0]?.riskLevel).toBeNull()
  })

  it('피보호자 거래 상세의 위험 분석을 파싱한다', () => {
    const response = wardTransactionDetailResponseSchema.parse({
      success: true,
      data: {
        transactionId: 104,
        type: 'TRANSFER',
        direction: 'OUT',
        status: 'COMPLETED',
        riskLevel: 'DANGER',
        counterpartyName: '김철수',
        bankName: '신한은행',
        accountNo: '110123456789',
        amount: 120_000,
        memo: '생활비',
        occurredAt: '2026-08-05T09:30:00',
        balanceAfter: 380_000,
        riskAnalysis: {
          riskScore: 72,
          summary: '평소보다 큰 금액이 송금됐어요.',
          reasons: ['HIGH_AMOUNT_L2', 'NEW_RECIPIENT'],
        },
      },
      message: null,
    })

    expect(response.data.riskAnalysis?.riskScore).toBe(72)
    expect(response.data.riskAnalysis?.reasons).toContain('NEW_RECIPIENT')
  })

  it('SAFE 거래도 BE가 내려주는 위험 분석 점수를 파싱한다', () => {
    const response = wardTransactionDetailResponseSchema.parse({
      success: true,
      data: {
        transactionId: 107,
        type: 'PAYMENT',
        direction: 'OUT',
        status: 'COMPLETED',
        riskLevel: 'SAFE',
        counterpartyName: '동네 약국',
        bankName: null,
        accountNo: null,
        amount: 12_000,
        memo: null,
        occurredAt: '2026-08-07T10:30:00',
        balanceAfter: 488_000,
        riskAnalysis: {
          riskScore: 8,
          summary: null,
          reasons: [],
        },
      },
      message: null,
    })

    expect(response.data.riskLevel).toBe('SAFE')
    expect(response.data.riskAnalysis?.riskScore).toBe(8)
  })

  it.each(['CANCELED', 'REJECTED', 'FAILED'] as const)(
    '%s 상태 상세의 거래 후 잔액 null을 파싱한다',
    (status) => {
      const response = wardTransactionDetailResponseSchema.parse({
        success: true,
        data: {
          transactionId: 105,
          type: 'TRANSFER',
          direction: 'OUT',
          status,
          riskLevel: null,
          counterpartyName: '김철수',
          bankName: '신한은행',
          accountNo: '110123456789',
          amount: 120_000,
          memo: null,
          occurredAt: '2026-08-05T09:30:00',
          balanceAfter: null,
          riskAnalysis: null,
        },
        message: null,
      })

      expect(response.data.type).toBe('TRANSFER')
      expect(response.data.status).toBe(status)
      expect(response.data.amount).toBe(120_000)
      expect(response.data.balanceAfter).toBeNull()
      expect(response.data.riskLevel).toBeNull()
    },
  )

  it('최신 BE enum에 없는 과거 거래 유형과 상태를 거부한다', () => {
    const detail = {
      success: true,
      data: {
        transactionId: 106,
        type: 'TRANSFER_OUT',
        direction: 'OUT',
        status: 'RJECTED',
        riskLevel: null,
        counterpartyName: '김철수',
        bankName: '신한은행',
        accountNo: '110123456789',
        amount: 120_000,
        memo: null,
        occurredAt: '2026-08-05T09:30:00',
        balanceAfter: null,
        riskAnalysis: null,
      },
      message: null,
    }

    expect(wardTransactionDetailResponseSchema.safeParse(detail).success).toBe(
      false,
    )
  })
})
