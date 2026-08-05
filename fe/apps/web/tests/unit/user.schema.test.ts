import { describe, expect, it } from 'vitest'

import {
  createUserRequestSchema,
  updateUserRequestSchema,
  usersResponseSchema,
} from '@/schemas/user.schema'

describe('usersSchema', () => {
  it('validates backend ApiResponse user lists', () => {
    const response = usersResponseSchema.parse({
      success: true,
      data: [
        {
          id: 1,
          name: 'Ada Lovelace',
          phone: '010-1234-5678',
          role: 'WARD',
          createdAt: '2026-07-16T10:00:00',
          updatedAt: '2026-07-16T10:00:00',
        },
      ],
      message: null,
    })

    expect(response.data[0]?.name).toBe('Ada Lovelace')
    expect(response.data[0]?.phone).toBe('01012345678')
  })

  it('matches the backend sign-up request fields', () => {
    const request = createUserRequestSchema.parse({
      role: 'GUARD',
      phone: '010-1234-5678',
      password: 'password123',
      name: '홍길동',
      birthDate: '19900101',
      gender: '남',
      paymentPassword: '123456',
      verificationToken: 'verification-token',
    })

    expect(request).toMatchObject({
      role: 'GUARD',
      phone: '01012345678',
      verificationToken: 'verification-token',
    })
  })

  it('validates editable profile fields', () => {
    expect(
      updateUserRequestSchema.parse({ name: '김보호', avatarId: 4 }),
    ).toEqual({ name: '김보호', avatarId: 4 })
    expect(() =>
      updateUserRequestSchema.parse({ name: '김보호', avatarId: 7 }),
    ).toThrow()
  })
})
