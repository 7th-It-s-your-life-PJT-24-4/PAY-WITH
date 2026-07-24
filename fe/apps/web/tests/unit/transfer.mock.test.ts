import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  advanceMockTransferStatus,
  cancelMockTransfer,
  getMockTransferDetail,
  resetMockTransferDetails,
} from '@/mocks/transfer.mock'

describe('transfer mock API', () => {
  beforeEach(() => {
    resetMockTransferDetails()
    vi.useFakeTimers()
  })

  it('승인 대기 거래를 완료 상태로 갱신해 조회한다', async () => {
    await advanceMockTransferStatus(74, 'COMPLETED')
    const request = getMockTransferDetail(74)

    await vi.advanceTimersByTimeAsync(500)

    await expect(request).resolves.toMatchObject({
      transactionId: 74,
      status: 'COMPLETED',
      remainingBalance: 1_200_000,
    })
  })

  it('승인 대기 거래를 취소한다', async () => {
    const request = cancelMockTransfer(74)

    await vi.advanceTimersByTimeAsync(500)

    await expect(request).resolves.toEqual({
      transactionId: 74,
      status: 'CANCELED',
    })
  })

  it('최종 상태 거래의 취소를 거부한다', async () => {
    const request = cancelMockTransfer(73)
    const rejection = expect(request).rejects.toMatchObject({
      name: 'TRANSFER_008',
    })

    await vi.advanceTimersByTimeAsync(500)

    await rejection
  })
})
