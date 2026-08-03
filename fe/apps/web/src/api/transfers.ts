import { apiClient } from '@/api/client'
import {
  recipientHistoryResponseSchema,
  recipientInquiryResponseSchema,
  transferResultResponseSchema,
  type CreateTransferRequest,
  type RecipientHistoryItem,
  type RecipientHistoryParams,
  type RecipientInquiry,
  type RecipientInquiryRequest,
  type TransferResult,
} from '@/schemas/transfer.schema'

export async function getTransferRecipients(
  params: RecipientHistoryParams = {},
): Promise<RecipientHistoryItem[]> {
  const searchParams = new URLSearchParams()
  if (params.keyword) searchParams.set('keyword', params.keyword)
  if (params.sort) searchParams.set('sort', params.sort)
  if (params.size) searchParams.set('size', String(params.size))
  const query = searchParams.toString()
  const response = await apiClient.get(
    `/ward/transfers/recipient${query ? `?${query}` : ''}`,
    recipientHistoryResponseSchema,
  )
  return response.data.recipients
}

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
