import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  completedPaymentSchema,
  paymentQrSessionSchema,
  type CompletedPayment,
  type PaymentQrSession,
} from '@/schemas/payment.schema'

const sessionKey = 'pay-with:ward-payment'

interface StoredPaymentState {
  qrSession: PaymentQrSession | null
  qrSessionReceivedAt: number | null
  completedPayment: CompletedPayment | null
}

export const usePaymentStore = defineStore('payment', () => {
  const qrSession = ref<PaymentQrSession | null>(null)
  const qrSessionReceivedAt = ref<number | null>(null)
  const completedPayment = ref<CompletedPayment | null>(null)
  const qrExpiresAt = computed(() => {
    if (!qrSession.value || qrSessionReceivedAt.value === null) return null
    return qrSessionReceivedAt.value + qrSession.value.expiresInSeconds * 1_000
  })

  function persist() {
    if (typeof sessionStorage === 'undefined') return
    const state: StoredPaymentState = {
      qrSession: qrSession.value,
      qrSessionReceivedAt: qrSessionReceivedAt.value,
      completedPayment: completedPayment.value,
    }
    sessionStorage.setItem(sessionKey, JSON.stringify(state))
  }

  function restore() {
    if (typeof sessionStorage === 'undefined') return
    try {
      const raw = sessionStorage.getItem(sessionKey)
      if (!raw) return
      const stored = JSON.parse(raw) as Partial<StoredPaymentState>
      const qrResult = paymentQrSessionSchema.safeParse(stored.qrSession)
      const receivedAt = stored.qrSessionReceivedAt
      const hasValidReceivedAt =
        typeof receivedAt === 'number' && Number.isFinite(receivedAt)
      const completedResult = completedPaymentSchema.safeParse(
        stored.completedPayment,
      )
      qrSession.value =
        qrResult.success && hasValidReceivedAt ? qrResult.data : null
      qrSessionReceivedAt.value =
        qrResult.success && hasValidReceivedAt ? receivedAt : null
      completedPayment.value = completedResult.success
        ? completedResult.data
        : null
    } catch {
      clear()
    }
  }

  function saveQrSession(value: PaymentQrSession) {
    qrSession.value = value
    qrSessionReceivedAt.value = Date.now()
    completedPayment.value = null
    persist()
  }

  function saveCompletedPayment(value: CompletedPayment) {
    completedPayment.value = value
    qrSession.value = null
    qrSessionReceivedAt.value = null
    persist()
  }

  function clearQrSession() {
    qrSession.value = null
    qrSessionReceivedAt.value = null
    persist()
  }

  function clear() {
    qrSession.value = null
    qrSessionReceivedAt.value = null
    completedPayment.value = null
    if (typeof sessionStorage !== 'undefined')
      sessionStorage.removeItem(sessionKey)
  }

  return {
    qrSession,
    qrExpiresAt,
    completedPayment,
    restore,
    saveQrSession,
    saveCompletedPayment,
    clearQrSession,
    clear,
  }
})
