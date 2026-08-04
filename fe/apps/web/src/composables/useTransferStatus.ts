import { computed, onBeforeUnmount, onMounted, ref, toValue } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MaybeRefOrGetter } from 'vue'

import {
  cancelMockTransfer,
  getMockTransferDetail,
} from '@/mocks/transfer.mock'
import { resolveTransferStatusRoute } from '@/pages/ward/transfer/-utils/transfer-status-route'
import { useTransferStore } from '@/stores/transfer.store'

const pollingInterval = 3_000

// TODO(transfer-detail-api): 피보호자 거래 상세 조회·취소 API가 제공되면
// mock 조회와 수동 polling을 TanStack Query 기반 실제 연동으로 교체한다.

export function useTransferStatus(
  transactionId: MaybeRefOrGetter<number>,
  options: { pollWhileHeld?: boolean } = {},
) {
  const route = useRoute()
  const router = useRouter()
  const transferStore = useTransferStore()
  const isLoading = ref(false)
  const isCancelling = ref(false)
  const errorMessage = ref('')
  let pollingTimer: ReturnType<typeof globalThis.setTimeout> | undefined
  let disposed = false

  const transferDetail = computed(() => {
    const detail = transferStore.transferDetail
    return detail?.transactionId === toValue(transactionId) ? detail : null
  })
  const canCancel = computed(() => transferStore.transferDetailSource !== 'api')

  function stopPolling() {
    if (pollingTimer !== undefined) globalThis.clearTimeout(pollingTimer)
    pollingTimer = undefined
  }

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

  function schedulePolling() {
    stopPolling()
    if (
      disposed ||
      !options.pollWhileHeld ||
      transferStore.transferDetailSource === 'api' ||
      transferDetail.value?.status !== 'HELD'
    )
      return

    pollingTimer = globalThis.setTimeout(async () => {
      await refresh()
      schedulePolling()
    }, pollingInterval)
  }

  async function refresh() {
    if (transferStore.transferDetailSource === 'api' && transferDetail.value)
      return transferDetail.value

    isLoading.value = true
    errorMessage.value = ''
    try {
      const detail = await getMockTransferDetail(toValue(transactionId))
      transferStore.setTransferDetail(detail)
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

  async function cancel() {
    if (isCancelling.value) return null
    if (!canCancel.value) {
      errorMessage.value =
        '송금 취소 기능은 준비 중입니다. 보호자에게 연락해 주세요.'
      return null
    }
    isCancelling.value = true
    errorMessage.value = ''
    stopPolling()
    try {
      await cancelMockTransfer(toValue(transactionId))
      return await refresh()
    } catch (error) {
      if (error instanceof Error && error.name === 'TRANSFER_008')
        return await refresh()
      else {
        errorMessage.value =
          error instanceof Error ? error.message : '거래를 취소하지 못했습니다.'
        return null
      }
    } finally {
      isCancelling.value = false
      schedulePolling()
    }
  }

  async function handleVisibilityChange() {
    if (document.visibilityState === 'hidden') {
      stopPolling()
      return
    }
    await refresh()
    schedulePolling()
  }

  onMounted(async () => {
    await refresh()
    schedulePolling()
    document.addEventListener('visibilitychange', handleVisibilityChange)
  })

  onBeforeUnmount(() => {
    disposed = true
    stopPolling()
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  })

  return {
    transferDetail,
    canCancel,
    isLoading,
    isCancelling,
    errorMessage,
    refresh,
    cancel,
    stopPolling,
  }
}
