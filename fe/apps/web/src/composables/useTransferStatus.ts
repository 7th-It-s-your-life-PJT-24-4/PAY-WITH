import { computed, onMounted, ref, toValue } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MaybeRefOrGetter } from 'vue'

import { resolveTransferStatusRoute } from '@/pages/ward/transfer/-utils/transfer-status-route'
import { useTransferStore } from '@/stores/transfer.store'
import type { TransferDetail } from '@/types/transfer'

export function useTransferStatus(transactionId: MaybeRefOrGetter<number>) {
  const route = useRoute()
  const router = useRouter()
  const transferStore = useTransferStore()
  const isLoading = ref(false)
  const isCancelling = ref(false)
  const errorMessage = ref('')

  const transferDetail = computed(() => {
    const detail = transferStore.transferDetail
    return detail?.transactionId === toValue(transactionId) ? detail : null
  })
  const canCancel = computed<boolean>(() => false)

  async function syncRoute() {
    const detail = transferDetail.value
    if (!detail) return
    const target = resolveTransferStatusRoute(
      detail.status,
      detail.transactionId,
      route.name,
    )
    if (target) await router.replace(target)
  }

  async function refresh() {
    if (transferDetail.value) return transferDetail.value

    isLoading.value = true
    errorMessage.value = ''
    try {
      const detail = transferStore.restoreTransferDetail(toValue(transactionId))
      if (!detail)
        throw new Error('송금 상태 조회 API가 아직 제공되지 않습니다.')
      await syncRoute()
      return detail
    } catch (error) {
      errorMessage.value =
        error instanceof Error
          ? error.message
          : '송금 상태를 확인하지 못했습니다.'
      return null
    } finally {
      isLoading.value = false
    }
  }

  async function cancel(): Promise<TransferDetail | null> {
    if (isCancelling.value) return null
    if (!canCancel.value) {
      errorMessage.value =
        '송금 취소 기능은 준비 중입니다. 보호자에게 연락해 주세요.'
      return null
    }
    isCancelling.value = true
    errorMessage.value = ''
    isCancelling.value = false
    return null
  }

  onMounted(async () => {
    await refresh()
  })

  return {
    transferDetail,
    canCancel,
    isLoading,
    isCancelling,
    errorMessage,
    refresh,
    cancel,
  }
}
