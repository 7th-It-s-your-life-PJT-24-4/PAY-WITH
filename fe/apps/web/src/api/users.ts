import { apiClient } from '@/api/client'
import { usersSchema, type User } from '@/schemas/user.schema'

export function getUsers(): Promise<User[]> {
  return apiClient.get('/users', usersSchema)
}
