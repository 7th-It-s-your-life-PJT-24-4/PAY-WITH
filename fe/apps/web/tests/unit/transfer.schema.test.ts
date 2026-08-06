import { describe, expect, it } from 'vitest'

import {
  recipientHistoryResponseSchema,
  recipientInquiryResponseSchema,
  transferResultResponseSchema,
} from '@/schemas/transfer.schema'
import { wardSafeAccountListResponseSchema } from '@/schemas/ward-safe-account.schema'

describe('transfer API schemas', () => {
  it('최근 수취인 목록 응답을 검증한다', () => {
    const response = recipientHistoryResponseSchema.parse({
      success: true,
      data: {
        recipients: [
          {
            recipientId: 1,
            holderName: '김민수',
            bankCode: '004',
            bankName: 'KB국민은행',
            accountNo: '1234567890123',
            lastSentAt: '2026-08-03T10:00:00',
            sendCount: 3,
            isRegisteredSafe: true,
            safeAccountId: 1,
            accountAlias: '민수 형',
          },
        ],
      },
      message: null,
    })

    expect(response.data.recipients[0]?.accountAlias).toBe('민수 형')
  })

  it('피보호자 안심계좌 목록 응답을 검증한다', () => {
    const response = wardSafeAccountListResponseSchema.parse({
      success: true,
      data: {
        safeAccounts: [
          {
            safeAccountId: 1,
            recipientId: 1,
            bankCode: '004',
            bankName: 'KB국민은행',
            accountNo: '1234567890123',
            holderName: '김민수',
            accountAlias: '민수 형',
            isVerified: true,
            createdAt: '2026-08-03T10:00:00',
          },
        ],
      },
      message: null,
    })

    expect(response.data.safeAccounts[0]?.recipientId).toBe(1)
  })
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
