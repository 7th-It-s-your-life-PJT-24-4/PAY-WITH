import { describe, expect, it } from 'vitest'

import { usersResponseSchema } from '@/schemas/user.schema'

describe('usersSchema', () => {
  it('validates backend ApiResponse user lists', () => {
    const response = usersResponseSchema.parse({
      success: true,
      data: [
        {
          id: 1,
          name: 'Ada Lovelace',
          email: 'ada@example.com',
          createdAt: '2026-07-16T10:00:00',
          updatedAt: '2026-07-16T10:00:00',
        },
      ],
      message: null,
    })

    expect(response.data[0]?.name).toBe('Ada Lovelace')
  })
})
