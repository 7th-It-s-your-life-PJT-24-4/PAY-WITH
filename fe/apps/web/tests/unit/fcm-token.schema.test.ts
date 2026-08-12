import { describe, expect, it } from 'vitest'

import {
  fcmTokenRequestSchema,
  fcmTokenResponseSchema,
} from '@/schemas/fcm-token.schema'

describe('FCM 토큰 스키마', () => {
  it('BE 계약과 동일하게 255자 토큰과 null 응답을 허용한다', () => {
    const token = 'a'.repeat(255)
    expect(fcmTokenRequestSchema.parse({ fcmToken: token })).toEqual({
      fcmToken: token,
    })
    expect(
      fcmTokenResponseSchema.parse({
        success: true,
        data: null,
        message: null,
      }),
    ).toEqual({ success: true, data: null, message: null })
  })

  it('빈 토큰과 255자를 넘는 토큰을 거부한다', () => {
    expect(fcmTokenRequestSchema.safeParse({ fcmToken: '   ' }).success).toBe(
      false,
    )
    expect(
      fcmTokenRequestSchema.safeParse({ fcmToken: 'a'.repeat(256) }).success,
    ).toBe(false)
  })
})
