import { useMutation } from '@tanstack/vue-query'

import { createTransfer } from '@/api/transfers'
import type { CreateTransferRequest } from '@/schemas/transfer.schema'

interface CreateTransferVariables {
  request: CreateTransferRequest
  idempotencyKey: string
}

export function useCreateTransferMutation() {
  return useMutation({
    mutationFn: ({ request, idempotencyKey }: CreateTransferVariables) =>
      createTransfer(request, idempotencyKey),
  })
}
