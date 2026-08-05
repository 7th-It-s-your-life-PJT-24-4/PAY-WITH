import { HTTPError } from 'ky'
import { describe, expect, it } from 'vitest'
import { z } from 'zod'

import {
  getApiErrorCode,
  getApiErrorMessage,
  isUnauthorizedApiError,
} from '@/api/error'

function createHttpError(data: unknown, status = 422) {
  const request = new Request('http://localhost/api/ward/charges')
  const response = new Response(null, { status })
  const error = new HTTPError(response, request, {} as never)
  error.data = data
  return error
}

describe('getApiErrorMessage', () => {
  it('검증된 API 오류 메시지를 반환한다', async () => {
    const error = createHttpError({
      code: 'ACCOUNT_002',
      message: '출금 계좌의 잔액이 부족합니다.',
    })

    await expect(getApiErrorMessage(error, '요청 실패')).resolves.toBe(
      '출금 계좌의 잔액이 부족합니다.',
    )
  })

  it('Zod 및 일반 오류의 내부 메시지를 노출하지 않는다', async () => {
    const zodError = z.number().safeParse('invalid').error

    await expect(getApiErrorMessage(zodError, '요청 실패')).resolves.toBe(
      '요청 실패',
    )
    await expect(
      getApiErrorMessage(new Error('내부 네트워크 오류'), '요청 실패'),
    ).resolves.toBe('요청 실패')
  })

  it('검증된 API 오류 코드를 반환한다', () => {
    const error = createHttpError({
      code: 'PAIRING_004',
      message: '인증 코드 입력 횟수를 초과했습니다.',
    })

    expect(getApiErrorCode(error)).toBe('PAIRING_004')
    expect(getApiErrorCode(new Error('내부 오류'))).toBeNull()
  })

  it('401 응답만 인증 거절로 판정한다', () => {
    expect(isUnauthorizedApiError(createHttpError(null, 401))).toBe(true)
    expect(isUnauthorizedApiError(createHttpError(null, 500))).toBe(false)
    expect(isUnauthorizedApiError(new TypeError('Failed to fetch'))).toBe(false)
  })
})
