import { useQuery } from '@tanstack/vue-query'
import { computed, ref, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { wardTransactionDetailOptions } from '@/lib/query/transaction-history'
import { resolveTransferStatusRoute } from '@/pages/ward/transfer/-utils/transfer-status-route'
import { useTransferStore } from '@/stores/transfer.store'
import type { TransferDetail, TransferStatus } from '@/types/transfer'

export function useTransferStatus(transactionId: MaybeRefOrGetter<number>) {
  const route = useRoute()
  const router = useRouter()
  const transferStore = useTransferStore()
  const isCancelling = ref(false)
  const errorMessage = ref('')

  const resolvedId = computed(() => toValue(transactionId))
  const transactionQuery = useQuery({
    ...wardTransactionDetailOptions(resolvedId),
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'HELD' ? 3000 : false
    },
  })

  const transferDetail = computed<TransferDetail | null>(() => {
    const queryData = transactionQuery.data.value
    if (queryData) {
      return {
        transactionId: queryData.transactionId,
        status: queryData.status as TransferStatus,
        holderName: queryData.transfer?.holderName ?? queryData.title,
        bankCode: '',
        bankName: queryData.transfer?.bankName ?? queryData.methodLabel,
        accountNo: queryData.transfer?.accountNo ?? '',
        amount: queryData.amount,
        memo: queryData.memo,
        requestedAt: queryData.occurredAt,
        expiredAt: null,
        respondedAt: null,
        completedAt: null,
        balanceAfter: queryData.balanceAfter,
        riskAnalysis: null,
        failureCode: null,
        failureMessage: null,
      }
    }
    const detail = transferStore.transferDetail
    return detail?.transactionId === resolvedId.value ? detail : null
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

  watch(
    () => transactionQuery.data.value,
    async (queryData) => {
      if (queryData) {
        const detail: TransferDetail = {
          transactionId: queryData.transactionId,
          status: queryData.status as TransferStatus,
          holderName: queryData.transfer?.holderName ?? queryData.title,
          bankCode: '',
          bankName: queryData.transfer?.bankName ?? queryData.methodLabel,
          accountNo: queryData.transfer?.accountNo ?? '',
          amount: queryData.amount,
          memo: queryData.memo,
          requestedAt: queryData.occurredAt,
          expiredAt: null,
          respondedAt: null,
          completedAt: null,
          balanceAfter: queryData.balanceAfter,
          riskAnalysis: null,
          failureCode: null,
          failureMessage: null,
        }
        transferStore.setTransferDetail(detail)
        await syncRoute()
      }
    },
    { immediate: true },
  )

  async function refresh() {
    await transactionQuery.refetch()
    await syncRoute()
    return transferDetail.value
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

  return {
    transferDetail,
    canCancel,
    isLoading: computed(() => transactionQuery.isFetching.value),
    isCancelling,
    errorMessage,
    refresh,
    cancel,
  }
}
