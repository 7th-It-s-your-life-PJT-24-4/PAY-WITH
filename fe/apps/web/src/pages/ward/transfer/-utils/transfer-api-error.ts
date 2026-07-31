import { isHTTPError } from 'ky'
import { z } from 'zod'

const apiErrorSchema = z.object({
  success: z.literal(false),
  data: z.unknown().nullable(),
  message: z.string(),
  code: z.string().nullable().optional(),
})

export async function getTransferApiError(
  error: unknown,
  fallbackMessage: string,
) {
  if (!isHTTPError(error))
    return { status: null, code: null, message: fallbackMessage }

  const parsed = apiErrorSchema.safeParse(error.data)
  return {
    status: error.response.status,
    code: parsed.success ? (parsed.data.code ?? null) : null,
    message: parsed.success ? parsed.data.message : fallbackMessage,
  }
}
