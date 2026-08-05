import { apiClient } from '@/api/client'
import {
  createUserRequestSchema,
  deleteUserResponseSchema,
  updateUserRequestSchema,
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
  const request = createUserRequestSchema.parse(body)
  const response = await apiClient.post('/users', userResponseSchema, request)
  return response.data
}

export async function updateUser(
  id: number,
  body: UpdateUserRequest,
): Promise<User> {
  const request = updateUserRequestSchema.parse(body)
  const response = await apiClient.put(
    `/users/${id}`,
    userResponseSchema,
    request,
  )
  return response.data
}

export async function deleteUser(id: number): Promise<void> {
  await apiClient.delete(`/users/${id}`, deleteUserResponseSchema)
}
