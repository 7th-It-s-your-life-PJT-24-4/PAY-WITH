import { describe, expect, it } from 'vitest'

import {
  recipientInquiryResponseSchema,
  transferResultResponseSchema,
} from '@/schemas/transfer.schema'

describe('transfer API schemas', () => {
  it('수취인 조회 ApiResponse를 검증한다', () => {
    const response = recipientInquiryResponseSchema.parse({
      success: true,
      data: {
        bankCode: '020',
        bankName: '우리은행',
        accountNo: '1234567890123',
        recipientName: '김준호',
      },
      message: null,
    })

    expect(response.data.recipientName).toBe('김준호')
  })

  it('완료 및 승인 대기 송금 응답을 검증한다', () => {
    const completed = transferResultResponseSchema.parse({
      success: true,
      data: {
        transactionId: 73,
        status: 'COMPLETED',
        holderName: '김준호',
        bankCode: '020',
        bankName: '우리은행',
        accountNo: '1234567890123',
        amount: 50_000,
        memo: null,
        completedAt: '2026-07-31T12:00:00',
        balanceAfter: 1_200_000,
      },
      message: null,
    })
    const held = transferResultResponseSchema.parse({
      success: true,
      data: {
        transactionId: 74,
        status: 'HELD',
        holderName: null,
        bankCode: null,
        bankName: null,
        accountNo: null,
        amount: null,
        memo: null,
        completedAt: null,
        balanceAfter: null,
      },
      message: null,
    })

    expect(completed.data.status).toBe('COMPLETED')
    expect(held.data.status).toBe('HELD')
  })
})
