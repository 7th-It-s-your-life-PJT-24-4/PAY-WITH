import { z } from 'zod'

import { phoneNumberSchema } from '@/schemas/auth.schema'
import { apiResponseSchema } from '@/schemas/api-response.schema'
import {
  birthDateSchema,
  loginPasswordSchema,
  nameSchema,
  paymentPasswordSchema,
} from '@/schemas/sign-up.schema'

export const userSchema = z.object({
  id: z.number(),
  name: z.string().min(1),
  phone: phoneNumberSchema,
  role: z.enum(['WARD', 'GUARD']),
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
})

export const usersSchema = z.array(userSchema)
export const userResponseSchema = apiResponseSchema(userSchema)
export const usersResponseSchema = apiResponseSchema(usersSchema)
export const deleteUserResponseSchema = apiResponseSchema(z.null())

export const createUserRequestSchema = z.object({
  role: z.enum(['WARD', 'GUARD']),
  phone: phoneNumberSchema,
  password: loginPasswordSchema,
  name: nameSchema,
  birthDate: birthDateSchema,
  gender: z.enum(['남', '여']),
  paymentPassword: paymentPasswordSchema,
  verificationToken: z.string().min(1),
})

export const updateUserRequestSchema = z.object({
  name: z.string().min(1),
})

export type User = z.infer<typeof userSchema>
export type CreateUserRequest = z.infer<typeof createUserRequestSchema>
export type UpdateUserRequest = z.infer<typeof updateUserRequestSchema>
