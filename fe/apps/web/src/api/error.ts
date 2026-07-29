import { HTTPError } from 'ky'

interface ApiErrorBody {
  message?: unknown
}

export async function getApiErrorMessage(
  error: unknown,
  fallback: string,
): Promise<string> {
  if (!(error instanceof HTTPError))
    return error instanceof Error ? error.message : fallback

  try {
    const body = (await error.response.clone().json()) as ApiErrorBody
    return typeof body.message === 'string' && body.message.length > 0
      ? body.message
      : fallback
  } catch {
    return fallback
  }
}
