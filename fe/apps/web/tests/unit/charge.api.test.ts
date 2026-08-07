import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getChargeAccounts, registerChargeAccount } from '@/api/accounts'
import { createWardCharge } from '@/api/charges'
import { apiClient } from '@/api/client'
import { createGuardCharge } from '@/api/guard-charges'

vi.mock('@/api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

const account = {
  accountId: 7,
  bankCode: '004',
  bankName: 'KB국민은행',
  accountNo: '12345612123456',
}

describe('charge API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('ApiResponse의 data에서 계좌 목록을 반환한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: [account],
      message: null,
    })

    await expect(getChargeAccounts()).resolves.toEqual([account])
    expect(apiClient.get).toHaveBeenCalledWith('/accounts', expect.anything())
  })

  it('금융결제원 코드로 계좌 등록을 요청한다', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: account,
      message: null,
    })
    const request = {
      bankCode: '004',
      accountNo: '12345612123456',
      accountPassword: '1004',
    }

    await expect(registerChargeAccount(request)).resolves.toEqual(account)
    expect(apiClient.post).toHaveBeenCalledWith(
      '/accounts',
      expect.anything(),
      request,
    )
  })

  it('정상 반환된 충전 응답을 완료 결과로 사용한다', async () => {
    const result = {
      transactionId: 43,
      chargeAmount: 50_000,
      balanceAfter: 150_000,
      bankName: 'KB국민은행',
      accountNo: '12345612123456',
      createdAt: '2026-07-23T17:30:00',
    }
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: result,
      message: null,
    })

    await expect(
      createWardCharge({ accountId: 7, amount: 50_000 }),
    ).resolves.toEqual(result)
    expect(apiClient.post).toHaveBeenCalledWith(
      '/ward/charges',
      expect.anything(),
      { accountId: 7, amount: 50_000 },
    )
  })

  it('보호자 충전 요청에 6자리 PIN을 포함한다', async () => {
    const result = {
      transactionId: 44,
      chargeAmount: 50_000,
      balanceAfter: 200_000,
      bankName: 'KB국민은행',
      accountNo: '12345612123456',
      createdAt: '2026-08-07T10:30:00',
      wardId: 13,
      wardName: '김시니어',
    }
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: result,
      message: null,
    })

    const request = { accountId: 7, amount: 50_000, pin: '123456' }

    await expect(createGuardCharge(13, request)).resolves.toEqual(result)
    expect(apiClient.post).toHaveBeenCalledWith(
      '/guard/wards/13/charges',
      expect.anything(),
      request,
    )
  })

  it('잘못된 계좌 등록 및 충전 요청은 전송하지 않는다', async () => {
    await expect(
      registerChargeAccount({
        bankCode: 'KB',
        accountNo: '1234',
        accountPassword: '12',
      }),
    ).rejects.toBeDefined()
    await expect(
      createWardCharge({ accountId: 7, amount: 0 }),
    ).rejects.toBeDefined()
    await expect(
      createGuardCharge(13, {
        accountId: 7,
        amount: 50_000,
        pin: '12345',
      }),
    ).rejects.toBeDefined()

    expect(apiClient.post).not.toHaveBeenCalled()
  })
})
