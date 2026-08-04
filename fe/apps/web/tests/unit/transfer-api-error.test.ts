import { describe, expect, it } from 'vitest'

import { getTransferFailureAction } from '@/pages/ward/transfer/-utils/transfer-api-error'

describe('getTransferFailureAction', () => {
  it.each([
    [400, '송금 비밀번호가 올바르지 않습니다.', 'retry-pin'],
    [404, '해당계좌를 찾을 수 없습니다.', 'edit-account'],
    [422, '송금 가능한 잔액이 부족합니다.', 'charge'],
    [409, '이전 요청이 처리 중입니다.', 'check-status'],
    [409, '동일한 키로 다른 내용의 요청이 감지되었습니다.', 'restart-transfer'],
  ] as const)('%s 응답을 %s 행동으로 분류한다', (status, message, action) => {
    expect(getTransferFailureAction({ status, code: null, message })).toBe(
      action,
    )
  })

  it('처리 중 실패는 단순 처리 중보다 먼저 판별해 재시도를 막는다', () => {
    expect(
      getTransferFailureAction({
        status: 409,
        code: null,
        message:
          '이전 요청이 처리 중 실패했습니다. 잔액을 확인 후 고객센터로 문의해주세요.',
      }),
    ).toBe('go-home')
  })

  it.each([500, null] as const)(
    '%s 오류에서는 안전하게 홈 이동만 허용한다',
    (status) => {
      expect(
        getTransferFailureAction({
          status,
          code: null,
          message: '송금 처리 결과를 확인할 수 없습니다.',
        }),
      ).toBe('go-home')
    },
  )

  it('BE 오류 코드가 제공되면 메시지보다 코드를 우선한다', () => {
    expect(
      getTransferFailureAction({
        status: 400,
        code: 'TRANSFER_INSUFFICIENT_BALANCE',
        message: '요청 실패',
      }),
    ).toBe('charge')
  })
})
