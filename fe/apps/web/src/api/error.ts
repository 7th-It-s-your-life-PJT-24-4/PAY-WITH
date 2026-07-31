import { HTTPError } from 'ky'
import { z } from 'zod'

const apiErrorBodySchema = z.object({
  code: z.string().nullish(),
  message: z.string().min(1),
})

export async function getApiErrorMessage(
  error: unknown,
  fallback: string,
): Promise<string> {
  if (!(error instanceof HTTPError))
    return error instanceof Error ? error.message : fallback

  try {
    const result = apiErrorBodySchema.safeParse(
      await error.response.clone().json(),
    )
    return result.success ? result.data.message : fallback
  } catch {
    return fallback
  }
}
