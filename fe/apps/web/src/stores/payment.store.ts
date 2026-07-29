import { ref } from 'vue'
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
  completedPayment: CompletedPayment | null
}

export const usePaymentStore = defineStore('payment', () => {
  const qrSession = ref<PaymentQrSession | null>(null)
  const completedPayment = ref<CompletedPayment | null>(null)

  function persist() {
    if (typeof sessionStorage === 'undefined') return
    const state: StoredPaymentState = {
      qrSession: qrSession.value,
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
      const completedResult = completedPaymentSchema.safeParse(
        stored.completedPayment,
      )
      qrSession.value = qrResult.success ? qrResult.data : null
      completedPayment.value = completedResult.success
        ? completedResult.data
        : null
    } catch {
      clear()
    }
  }

  function saveQrSession(value: PaymentQrSession) {
    qrSession.value = value
    completedPayment.value = null
    persist()
  }

  function saveCompletedPayment(value: CompletedPayment) {
    completedPayment.value = value
    qrSession.value = null
    persist()
  }

  function clearQrSession() {
    qrSession.value = null
    persist()
  }

  function clear() {
    qrSession.value = null
    completedPayment.value = null
    if (typeof sessionStorage !== 'undefined')
      sessionStorage.removeItem(sessionKey)
  }

  return {
    qrSession,
    completedPayment,
    restore,
    saveQrSession,
    saveCompletedPayment,
    clearQrSession,
    clear,
  }
})
