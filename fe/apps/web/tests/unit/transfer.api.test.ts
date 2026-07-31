import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import { createTransfer, inquireTransferRecipient } from '@/api/transfers'

vi.mock('@/api/client', () => ({
  apiClient: { post: vi.fn() },
}))

describe('transfer API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('수취인 응답 이름을 holderName으로 정규화한다', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: {
        bankCode: '020',
        bankName: '우리은행',
        accountNo: '1234567890123',
        recipientName: '김준호',
      },
      message: null,
    })

    const result = await inquireTransferRecipient({
      bankCode: '020',
      accountNo: '1234567890123',
    })

    expect(result.holderName).toBe('김준호')
    expect(apiClient.post).toHaveBeenCalledWith(
      '/ward/transfers/recipient',
      expect.anything(),
      { bankCode: '020', accountNo: '1234567890123' },
    )
  })

  it('송금 요청에 멱등성 헤더를 포함한다', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: {
        transactionId: 74,
        status: 'HELD',
      },
      message: null,
    })

    await createTransfer(
      {
        bankCode: '020',
        accountNo: '1234567890123',
        amount: 50_000,
        memo: null,
        transferPin: '123456',
      },
      '550e8400-e29b-41d4-a716-446655440000',
    )

    expect(apiClient.post).toHaveBeenCalledWith(
      '/ward/transfers',
      expect.anything(),
      expect.objectContaining({ transferPin: '123456' }),
      {
        'Idempotency-Key': '550e8400-e29b-41d4-a716-446655440000',
      },
    )
  })
})
