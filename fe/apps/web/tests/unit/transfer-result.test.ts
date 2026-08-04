import { describe, expect, it } from 'vitest'

import { getTransferResultAction } from '@/pages/ward/transfer/-utils/transfer-result'

describe('getTransferResultAction', () => {
  it('완료된 송금에는 추가 행동을 표시하지 않는다', () => {
    expect(getTransferResultAction('COMPLETED', null)).toBeNull()
  })

  it.each(['EXPIRED', 'CANCELED'] as const)(
    '%s 송금에는 다시 송금하기를 표시한다',
    (status) => {
      expect(getTransferResultAction(status, null)).toEqual({
        label: '송금 다시하기',
        routeName: 'ward-transfer',
      })
    },
  )

  it('잔액 부족 실패에는 충전하기를 표시한다', () => {
    expect(getTransferResultAction('FAILED', 'INSUFFICIENT_BALANCE')).toEqual({
      label: '충전하기',
      routeName: 'ward-charge',
    })
  })

  it('알 수 없는 실패 코드에는 안전한 재송금 행동을 표시한다', () => {
    expect(getTransferResultAction('FAILED', 'UNKNOWN_ERROR')).toEqual({
      label: '송금 다시하기',
      routeName: 'ward-transfer',
    })
  })
})
