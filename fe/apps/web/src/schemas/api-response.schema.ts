import { z, type ZodType } from 'zod'

export function apiResponseSchema<T>(dataSchema: ZodType<T>) {
  return z.object({
    success: z.literal(true),
    data: dataSchema,
    message: z.string().nullable(),
  })
}
