import { HTTPError } from 'ky'
import { z } from 'zod'

const apiErrorBodySchema = z.object({
  code: z.string().optional(),
  message: z.string().min(1),
})

export async function getApiErrorMessage(
  error: unknown,
  fallback: string,
): Promise<string> {
  if (!(error instanceof HTTPError))
    return error instanceof Error ? error.message : fallback

  const result = apiErrorBodySchema.safeParse(error.data)
  return result.success ? result.data.message : fallback
}
