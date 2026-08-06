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

  it('전체 조회에 섞인 이전 계약 값을 목록 표시 값으로 정규화한다', () => {
    const response = wardTransactionHistoryResponseSchema.parse({
      success: true,
      data: {
        transactions: [
          {
            transactionId: 3,
            type: 'TRANSFER_OUT',
            direction: 'OUT',
            title: '김철수',
            amount: -20_000,
            status: 'PENDING',
            riskLevel: '',
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
    expect(response.data.transactions[0]?.amount).toBe(20_000)
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
        riskScore: null,
        riskAnalysis: {
          riskScore: 72,
          summary: '평소보다 큰 금액이 송금됐어요.',
          reasons: ['HIGH_AMOUNT_L2', 'NEW_RECIPIENT'],
        },
      },
      message: null,
    })

    expect(response.data.riskScore).toBeNull()
    expect(response.data.riskAnalysis?.reasons).toContain('NEW_RECIPIENT')
  })
})
