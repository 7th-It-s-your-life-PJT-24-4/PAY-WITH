import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const userSchema = z.object({
  id: z.number(),
  name: z.string().min(1),
  email: z.email(),
  createdAt: z.string(),
  updatedAt: z.string(),
})

export const usersSchema = z.array(userSchema)
export const userResponseSchema = apiResponseSchema(userSchema)
export const usersResponseSchema = apiResponseSchema(usersSchema)
export const deleteUserResponseSchema = apiResponseSchema(z.null())

export const createUserRequestSchema = z.object({
  email: z.email(),
  password: z.string().min(1),
  name: z.string().min(1),
})

export const updateUserRequestSchema = z.object({
  name: z.string().min(1),
})

export type User = z.infer<typeof userSchema>
export type CreateUserRequest = z.infer<typeof createUserRequestSchema>
export type UpdateUserRequest = z.infer<typeof updateUserRequestSchema>
