import { describe, expect, it } from 'vitest'

import { usersSchema } from '@/schemas/user.schema'

describe('usersSchema', () => {
  it('validates API user responses', () => {
    const users = usersSchema.parse([
      {
        id: 1,
        name: 'Ada Lovelace',
        email: 'ada@example.com',
      },
    ])

    expect(users[0]?.name).toBe('Ada Lovelace')
  })
})
