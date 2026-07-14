import { z } from 'zod'

export const userSchema = z.object({
  id: z.number(),
  name: z.string().min(1),
  email: z.string().email(),
})

export const usersSchema = z.array(userSchema)

export type User = z.infer<typeof userSchema>
