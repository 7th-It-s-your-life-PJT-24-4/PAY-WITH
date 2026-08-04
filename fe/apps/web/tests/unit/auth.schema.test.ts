import { describe, expect, it } from 'vitest'

import {
  loginRequestSchema,
  phoneCodeRequestSchema,
  phoneVerifyRequestSchema,
} from '@/schemas/auth.schema'

describe('auth schemas', () => {
  it('normalizes a hyphenated phone number for login', () => {
    const request = loginRequestSchema.parse({
      phone: '010-1234-5678',
      password: 'password123',
    })

    expect(request.phone).toBe('01012345678')
  })

  it('validates the phone verification request contract', () => {
    expect(
      phoneCodeRequestSchema.parse({
        phone: '010-1234-5678',
        purpose: 'SIGNUP',
      }),
    ).toMatchObject({ phone: '01012345678', purpose: 'SIGNUP' })

    expect(() =>
      phoneVerifyRequestSchema.parse({
        phone: '01012345678',
        code: '12345',
      }),
    ).toThrow()
  })
})
