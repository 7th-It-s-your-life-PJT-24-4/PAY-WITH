import { isHTTPError } from 'ky'
import { z } from 'zod'

const apiErrorSchema = z.object({
  success: z.literal(false),
  data: z.unknown().nullable(),
  message: z.string(),
  code: z.string().nullable().optional(),
})

export type TransferFailureAction =
  | 'retry-pin'
  | 'edit-account'
  | 'charge'
  | 'check-status'
  | 'restart-transfer'
  | 'go-home'

export interface TransferApiError {
  status: number | null
  code: string | null
  message: string
}

const failureActionByCode: Record<string, TransferFailureAction> = {
  TRANSFER_INVALID_PIN: 'retry-pin',
  TRANSFER_ACCOUNT_NOT_FOUND: 'edit-account',
  TRANSFER_INSUFFICIENT_BALANCE: 'charge',
  TRANSFER_IN_PROGRESS: 'check-status',
  TRANSFER_IDEMPOTENCY_CONFLICT: 'restart-transfer',
  TRANSFER_IRRECOVERABLE: 'go-home',
  TRANSFER_WALLET_NOT_FOUND: 'go-home',
  TRANSFER_FORBIDDEN: 'go-home',
}

export function getTransferFailureAction({
  status,
  code,
  message,
}: TransferApiError): TransferFailureAction {
  if (code && failureActionByCode[code]) return failureActionByCode[code]

  // TODO(transfer-error-code): BE 송금 오류 코드가 적용되면 아래 메시지 호환 분기를 제거한다.
  if (status === 400 && message.includes('비밀번호')) return 'retry-pin'
  if (status === 404 && message.includes('계좌')) return 'edit-account'
  if (status === 422) return 'charge'
  if (status === 409 && message.includes('처리 중 실패')) return 'go-home'
  if (status === 409 && message.includes('이전 요청이 처리 중'))
    return 'check-status'
  if (status === 409 && message.includes('동일한 키')) return 'restart-transfer'

  // 결과를 확신할 수 없는 서버·네트워크 오류에서는 새 송금을 허용하지 않는다.
  return 'go-home'
}

export async function getTransferApiError(
  error: unknown,
  fallbackMessage: string,
): Promise<TransferApiError> {
  if (!isHTTPError(error))
    return { status: null, code: null, message: fallbackMessage }

  const parsed = apiErrorSchema.safeParse(error.data)
  return {
    status: error.response.status,
    code: parsed.success ? (parsed.data.code ?? null) : null,
    message: parsed.success ? parsed.data.message : fallbackMessage,
  }
}
