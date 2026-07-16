import { apiClient } from '@/api/client'
import {
  deleteUserResponseSchema,
  userResponseSchema,
  usersResponseSchema,
  type CreateUserRequest,
  type UpdateUserRequest,
  type User,
} from '@/schemas/user.schema'

export async function getUsers(): Promise<User[]> {
  const response = await apiClient.get('/users', usersResponseSchema)
  return response.data
}

export async function getUser(id: number): Promise<User> {
  const response = await apiClient.get(`/users/${id}`, userResponseSchema)
  return response.data
}

export async function createUser(body: CreateUserRequest): Promise<User> {
  const response = await apiClient.post('/users', userResponseSchema, body)
  return response.data
}

export async function updateUser(
  id: number,
  body: UpdateUserRequest,
): Promise<User> {
  const response = await apiClient.put(`/users/${id}`, userResponseSchema, body)
  return response.data
}

export async function deleteUser(id: number): Promise<void> {
  await apiClient.delete(`/users/${id}`, deleteUserResponseSchema)
}
