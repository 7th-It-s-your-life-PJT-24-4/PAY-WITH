import { apiClient } from '@/api/client'
import {
  recipientInquiryResponseSchema,
  transferResultResponseSchema,
  type CreateTransferRequest,
  type RecipientInquiry,
  type RecipientInquiryRequest,
  type TransferResult,
} from '@/schemas/transfer.schema'

export async function inquireTransferRecipient(
  request: RecipientInquiryRequest,
): Promise<RecipientInquiry> {
  const response = await apiClient.post(
    '/ward/transfers/recipient',
    recipientInquiryResponseSchema,
    request,
  )
  const { recipientName, ...recipient } = response.data
  return { ...recipient, holderName: recipientName }
}

export async function createTransfer(
  request: CreateTransferRequest,
  idempotencyKey: string,
): Promise<TransferResult> {
  const response = await apiClient.post(
    '/ward/transfers',
    transferResultResponseSchema,
    request,
    { 'Idempotency-Key': idempotencyKey },
  )
  return response.data
}
