import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  approveApprovalRequest,
  getApprovalRequestDetail,
  getApprovalRequestHistory,
  getApprovalRequests,
  rejectApprovalRequest,
} from '@/api/approval-requests'
import { apiClient } from '@/api/client'

vi.mock('@/api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const decision = {
  approvalId: 3,
  transactionId: 41,
  status: 'APPROVED' as const,
  respondedAt: '2026-08-05T09:20:00',
  transfer: {
    status: 'COMPLETED' as const,
    failureReason: null,
    completedAt: '2026-08-05T09:20:01',
    balanceAfter: 120_000,
  },
}

describe('guard approval request API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('선택한 시니어의 승인 대기 목록과 상세를 조회한다', async () => {
    vi.mocked(apiClient.get)
      .mockResolvedValueOnce({ success: true, data: [], message: null })
      .mockResolvedValueOnce({
        success: true,
        data: { approvalId: 3 },
        message: null,
      })

    await getApprovalRequests(12)
    await getApprovalRequestDetail(3)

    expect(apiClient.get).toHaveBeenNthCalledWith(
      1,
      '/approval-requests?wardId=12',
      expect.anything(),
    )
    expect(apiClient.get).toHaveBeenNthCalledWith(
      2,
      '/approval-requests/3',
      expect.anything(),
    )
  })

  it('승인과 거절을 각각 결정 API로 요청한다', async () => {
    vi.mocked(apiClient.post)
      .mockResolvedValueOnce({ success: true, data: decision, message: null })
      .mockResolvedValueOnce({
        success: true,
        data: {
          ...decision,
          status: 'REJECTED' as const,
          transfer: null,
        },
        message: null,
      })

    await approveApprovalRequest(3)
    await rejectApprovalRequest(3)

    expect(apiClient.post).toHaveBeenNthCalledWith(
      1,
      '/approval-requests/3/approve',
      expect.anything(),
      {},
    )
    expect(apiClient.post).toHaveBeenNthCalledWith(
      2,
      '/approval-requests/3/reject',
      expect.anything(),
      {},
    )
  })

  it('선택한 시니어와 상태로 승인 이력을 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: [],
      message: null,
    })

    await getApprovalRequestHistory('APPROVED', 12)

    expect(apiClient.get).toHaveBeenCalledWith(
      '/approval-requests/history?status=APPROVED&wardId=12',
      expect.anything(),
    )
  })
})
