<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { guardTransactionDetailOptions } from '@/lib/query/guard/transaction'
import GuardTransactionDetailContent from '@/pages/guard/-components/GuardTransactionDetailContent.vue'
import { parsePositiveRouteId } from '@/pages/guard/-utils/guard-route'
import { createGuardTransactionDetailView } from '@/pages/guard/-utils/guard-transaction-detail'
import GuardApprovalHeader from '@/pages/guard/approval-requests/-components/GuardApprovalHeader.vue'
import { useGuardStore } from '@/stores/guard.store'

type ApprovalHistoryFilter = 'approved' | 'rejected' | 'canceled' | 'expired'

const route = useRoute()
const router = useRouter()
const guardStore = useGuardStore()
const transactionId = computed(() => parsePositiveRouteId(route.params.id))
const routeWardId = computed(() => parsePositiveRouteId(route.query.wardId))
const wardId = computed(() => routeWardId.value ?? guardStore.activeWardId)
const approvalStatus = computed<ApprovalHistoryFilter | null>(() => {
  const status = route.query.approvalStatus
  return status === 'approved' ||
    status === 'rejected' ||
    status === 'canceled' ||
    status === 'expired'
    ? status
    : null
})
const routeFailureReason = computed(() =>
  typeof route.query.failureReason === 'string'
    ? route.query.failureReason
    : null,
)
const transactionQuery = useQuery(
  guardTransactionDetailOptions(wardId, transactionId),
)
const detail = computed(() => transactionQuery.data.value ?? null)
const detailContent = computed(() =>
  detail.value
    ? createGuardTransactionDetailView(detail.value, routeFailureReason.value)
    : null,
)
const isFailed = computed(() => detail.value?.status === 'FAILED')
const isFailedTransfer = computed(
  () => isFailed.value && detail.value?.type === 'TRANSFER',
)
const isApprovalSource = computed(() => route.query.source === 'approval')
const hasValidParams = computed(
  () => transactionId.value !== null && wardId.value !== null,
)

function goBack() {
  if (isApprovalSource.value) {
    router.replace({
      name: 'guard-approval-requests',
      query: {
        wardId: wardId.value ?? undefined,
        status: approvalStatus.value ?? undefined,
      },
    })
    return
  }

  router.replace({
    name: 'guard-history',
    query: { wardId: wardId.value ?? undefined },
  })
}

function chargeWardWallet() {
  router.push({
    name: 'guard-charge',
    query: { wardId: wardId.value ?? undefined },
  })
}

function contactWard() {
  window.location.href = 'tel:'
}
</script>

<template>
  <main
    class="min-h-screen bg-white"
    :class="isFailedTransfer ? 'pb-[220px]' : 'pb-28'"
  >
    <GuardApprovalHeader
      title="거래 상세"
      :back-label="
        isApprovalSource
          ? '이상 거래 목록으로 돌아가기'
          : '거래 내역으로 돌아가기'
      "
      @back="goBack"
    />

    <section
      v-if="!hasValidParams"
      class="flex min-h-[560px] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-700">
        올바르지 않은 거래 정보예요.
      </p>
      <Button
        class="mt-lg w-full"
        label="목록으로 돌아가기"
        variant="guard-cta"
        size="guard-cta"
        @click="goBack"
      />
    </section>

    <section
      v-else-if="transactionQuery.isPending.value"
      class="flex min-h-[560px] items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-500"
      aria-busy="true"
    >
      거래 상세를 불러오는 중이에요.
    </section>

    <section
      v-else-if="transactionQuery.isError.value || !detailContent"
      class="flex min-h-[560px] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-700">
        거래 상세를 불러오지 못했어요.
      </p>
      <button
        class="mt-md min-h-11 px-md text-[16px] font-semibold text-primary-500"
        type="button"
        @click="transactionQuery.refetch()"
      >
        다시 시도
      </button>
    </section>

    <GuardTransactionDetailContent v-else :detail="detailContent" />

    <div
      v-if="detail"
      class="fixed inset-x-0 bottom-[calc(20px+env(safe-area-inset-bottom))] z-30 mx-auto grid w-full max-w-[390px] gap-sm px-mobile-gutter"
    >
      <template v-if="isFailedTransfer">
        <Button
          class="w-full"
          label="지갑 충전해주기"
          variant="guard-cta"
          size="guard-cta"
          @click="chargeWardWallet"
        />
        <Button
          class="w-full"
          label="피보호자에게 연락하기"
          variant="outline-primary"
          size="guard-cta"
          @click="contactWard"
        />
        <Button
          v-if="isApprovalSource"
          class="w-full"
          label="이상 거래 목록으로"
          variant="outline-primary"
          size="guard-cta"
          @click="goBack"
        />
      </template>

      <Button
        v-else-if="isApprovalSource"
        class="w-full"
        label="이상 거래 목록으로"
        :variant="detail.status === 'COMPLETED' ? 'guard-cta' : 'danger'"
        size="guard-cta"
        @click="goBack"
      />
      <Button
        v-else
        class="w-full shadow-[0_10px_15px_-3px_rgb(0_0_0/10%),0_4px_6px_-4px_rgb(0_0_0/10%)]"
        label="연락하기"
        variant="guard-cta"
        size="guard-cta"
        @click="contactWard"
      />
    </div>
  </main>
</template>
