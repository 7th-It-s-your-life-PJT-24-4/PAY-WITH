import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import { getGuardSafeAccounts } from '@/api/guard-safe-accounts'
import { guardSafeAccountListResponseSchema } from '@/schemas/guard-safe-account.schema'

vi.mock('@/api/client', () => ({
  apiClient: { get: vi.fn() },
}))

const safeAccount = {
  safeAccountId: 10,
  recipientId: null,
  bankCode: '004',
  bankName: 'KB국민은행',
  accountNo: '11012300006781',
  holderName: '안유진',
  accountAlias: null,
  isVerified: true,
  createdAt: '2026-08-06T10:00:00',
}

describe('guard safe account API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('선택한 피보호자의 안전계좌 목록을 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { safeAccounts: [safeAccount] },
      message: null,
    })

    await expect(getGuardSafeAccounts(1)).resolves.toEqual([safeAccount])
    expect(apiClient.get).toHaveBeenCalledWith(
      '/guard/wards/1/safe-accounts',
      guardSafeAccountListResponseSchema,
    )
  })

  it('목록 응답은 등록 응답 전용 status 없이 파싱한다', () => {
    expect(
      guardSafeAccountListResponseSchema.parse({
        success: true,
        data: { safeAccounts: [safeAccount] },
        message: null,
      }).data.safeAccounts[0],
    ).toEqual(safeAccount)
  })
})
