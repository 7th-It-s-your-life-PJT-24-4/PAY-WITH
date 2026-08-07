import { describe, expect, it } from 'vitest'

import {
  wardGuardianResponseSchema,
  wardGuardianSchema,
} from '@/schemas/ward-guardian.schema'

describe('wardGuardianSchema', () => {
  it('올바른 보호자 정보를 검증한다', () => {
    const validData = {
      name: '김보호',
      phone: '01012345678',
      avatarId: 1,
    }

    expect(wardGuardianSchema.parse(validData)).toEqual(validData)
  })

  it('avatarId가 null이거나 생략되어도 허용한다', () => {
    const dataWithoutAvatar = {
      name: '이보호',
      phone: '01098765432',
      avatarId: null,
    }

    expect(wardGuardianSchema.parse(dataWithoutAvatar)).toEqual(
      dataWithoutAvatar,
    )
  })

  it('API 응답 래퍼 구조를 올바르게 검증한다', () => {
    const apiPayload = {
      success: true,
      data: {
        name: '박보호',
        phone: '01011112222',
        avatarId: 3,
      },
      message: null,
    }

    expect(wardGuardianResponseSchema.parse(apiPayload)).toEqual(apiPayload)
  })
})
