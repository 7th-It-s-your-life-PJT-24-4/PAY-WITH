import { apiClient } from '@/api/client'
import {
  paymentCancelResponseSchema,
  paymentQrSessionResponseSchema,
  paymentStatusResponseSchema,
  type CreateWardPaymentRequest,
  type PaymentCancel,
  type PaymentQrSession,
  type PaymentStatus,
} from '@/schemas/payment.schema'

export function createWardPayment(
  request: CreateWardPaymentRequest,
): Promise<PaymentQrSession> {
  return apiClient
    .post('/ward/payments', paymentQrSessionResponseSchema, request)
    .then(({ data }) => data)
}

export function getWardPaymentStatus(
  paymentId: number,
): Promise<PaymentStatus> {
  return apiClient
    .get(`/ward/payments/${paymentId}`, paymentStatusResponseSchema)
    .then(({ data }) => data)
}

export function cancelWardPayment(paymentId: number): Promise<PaymentCancel> {
  return apiClient
    .post(
      `/ward/payments/${paymentId}/cancel`,
      paymentCancelResponseSchema,
      undefined,
    )
    .then(({ data }) => data)
}
