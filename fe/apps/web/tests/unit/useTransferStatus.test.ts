import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'

import { useTransferStatus } from '@/composables/useTransferStatus'

vi.mock('@/api/transaction-history', () => ({
  getWardTransactionDetail: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    name: 'ward-transfer-complete',
    params: { transactionId: '101' },
  }),
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  onBeforeRouteLeave: vi.fn(),
}))

describe('useTransferStatus', () => {
  let queryClient: QueryClient

  beforeEach(() => {
    setActivePinia(createPinia())
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    })
  })

  it('COMPLETED 거래 조회 시 completedAt과 requestedAt에 발생 시각을 매핑한다', async () => {
    const { getWardTransactionDetail } =
      await import('@/api/transaction-history')
    vi.mocked(getWardTransactionDetail).mockResolvedValue({
      transactionId: 101,
      type: 'TRANSFER',
      direction: 'OUT',
      status: 'COMPLETED',
      riskLevel: null,
      counterpartyName: '홍길동',
      bankName: '국민은행',
      accountNo: '123456789012',
      amount: 50000,
      memo: '용돈',
      occurredAt: '2026-08-10T15:30:00',
      balanceAfter: 150000,
      riskAnalysis: null,
    })

    let hookResult: ReturnType<typeof useTransferStatus> | null = null

    const TestComponent = defineComponent({
      setup() {
        hookResult = useTransferStatus(101)
        return () => h('div')
      },
    })

    mount(TestComponent, {
      global: {
        plugins: [[VueQueryPlugin, { queryClient }]],
      },
    })

    await vi.waitFor(() => {
      expect(hookResult?.transferDetail.value).not.toBeNull()
    })

    const detail = hookResult!.transferDetail.value!
    expect(detail.status).toBe('COMPLETED')
    expect(detail.requestedAt).toBe('2026-08-10T15:30:00')
    expect(detail.completedAt).toBe('2026-08-10T15:30:00')
    expect(detail.holderName).toBe('홍길동')
    expect(detail.bankName).toBe('국민은행')
    expect(detail.amount).toBe(50000)
    expect(detail.balanceAfter).toBe(150000)
  })
})
