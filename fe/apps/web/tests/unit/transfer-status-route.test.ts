import { describe, expect, it } from 'vitest'

import { resolveTransferStatusRoute } from '@/pages/ward/transfer/-utils/transfer-status-route'

describe('resolveTransferStatusRoute', () => {
  it('승인 대기 상태에서는 현재 대기 화면을 유지한다', () => {
    expect(
      resolveTransferStatusRoute('HELD', 74, 'ward-transfer-held'),
    ).toBeNull()
    expect(
      resolveTransferStatusRoute('HELD', 74, 'ward-transfer-restricted'),
    ).toBeNull()
  })

  it('승인 완료와 거절 상태를 최종 결과 화면으로 연결한다', () => {
    expect(
      resolveTransferStatusRoute('COMPLETED', 74, 'ward-transfer-held'),
    ).toEqual({
      name: 'ward-transfer-complete',
      params: { transactionId: 74 },
      replace: true,
    })
    expect(
      resolveTransferStatusRoute('REJECTED', 74, 'ward-transfer-held'),
    ).toEqual({
      name: 'ward-transfer-rejected',
      params: { transactionId: 74 },
      replace: true,
    })
  })

  it.each([
    ['CANCELED', 'ward-transfer-canceled'],
    ['EXPIRED', 'ward-transfer-expired'],
    ['FAILED', 'ward-transfer-failed'],
  ] as const)('%s 상태를 결과 화면으로 연결한다', (status, routeName) => {
    expect(
      resolveTransferStatusRoute(status, 74, 'ward-transfer-held'),
    ).toEqual({
      name: routeName,
      params: { transactionId: 74 },
      replace: true,
    })
  })
})
