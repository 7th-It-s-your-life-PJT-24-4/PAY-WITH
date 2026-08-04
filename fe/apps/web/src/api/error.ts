import { HTTPError } from 'ky'
import { z, ZodError } from 'zod'

const apiErrorBodySchema = z.object({
  code: z.string().optional(),
  message: z.string().min(1),
})

export function getApiErrorCode(error: unknown): string | null {
  if (!(error instanceof HTTPError)) return null

  const result = apiErrorBodySchema.safeParse(error.data)
  return result.success ? (result.data.code ?? null) : null
}

export async function getApiErrorMessage(
  error: unknown,
  fallback: string,
): Promise<string> {
  if (error instanceof ZodError) return fallback
  if (!(error instanceof HTTPError)) return fallback

  const result = apiErrorBodySchema.safeParse(error.data)
  return result.success ? result.data.message : fallback
}
