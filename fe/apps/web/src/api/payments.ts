import { apiClient } from '@/api/client'
import {
  executedPaymentResponseSchema,
  paymentCancelResponseSchema,
  paymentQrSessionResponseSchema,
  paymentStatusResponseSchema,
  type CreateWardPaymentRequest,
  type ExecutedPayment,
  type ExecutePaymentRequest,
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

/** 가맹점 스캐너의 결제 실행 — 60초 1회용 qrToken 이 자격증명이라 인증 없이 호출한다 */
export function executePayment(
  request: ExecutePaymentRequest,
): Promise<ExecutedPayment> {
  return apiClient
    .post('/payments/execute', executedPaymentResponseSchema, request)
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
