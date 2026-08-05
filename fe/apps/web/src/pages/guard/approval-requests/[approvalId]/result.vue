<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { HTTPError } from 'ky'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { guardApprovalResultOptions } from '@/lib/query/guard/approval'
import GuardApprovalDetailContent from '@/pages/guard/approval-requests/-components/GuardApprovalDetailContent.vue'
import GuardApprovalHeader from '@/pages/guard/approval-requests/-components/GuardApprovalHeader.vue'

type DecisionState = 'approved' | 'rejected' | 'canceled' | 'expired'

const route = useRoute()
const router = useRouter()
const approvalId = computed(() => {
  const value = Number(route.params.approvalId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const resultQuery = useQuery(guardApprovalResultOptions(approvalId))
const fallbackState = computed<DecisionState>(() => {
  const status = route.query.status
  return status === 'approved' ||
    status === 'rejected' ||
    status === 'canceled' ||
    status === 'expired'
    ? status
    : 'rejected'
})
const state = computed<DecisionState>(
  () =>
    (resultQuery.data.value?.decision.status.toLowerCase() ??
      fallbackState.value) as DecisionState,
)
const transferFailureReason = computed(() => {
  const transfer = resultQuery.data.value?.decision.transfer
  return transfer?.status === 'FAILED' ? transfer.failureReason : null
})
const resultNotFound = computed(
  () =>
    resultQuery.error.value instanceof HTTPError &&
    resultQuery.error.value.response.status === 404,
)

function goToList() {
  router.replace({
    name: 'guard-approval-requests',
    query: {
      wardId:
        resultQuery.data.value?.detail.wardId ??
        route.query.wardId ??
        undefined,
      status: state.value,
    },
  })
}
</script>

<template>
  <main class="min-h-screen bg-white pb-32">
    <GuardApprovalHeader
      back-label="이상 거래 목록으로 돌아가기"
      @back="goToList"
    />

    <section
      v-if="approvalId !== null && resultQuery.isPending.value"
      class="flex min-h-[560px] items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-500"
      aria-busy="true"
    >
      처리된 거래 정보를 불러오는 중이에요.
    </section>

    <GuardApprovalDetailContent
      v-else-if="resultQuery.data.value"
      :detail="resultQuery.data.value.detail"
      :state="state"
      :transfer-failure-reason="transferFailureReason"
    />

    <section
      v-else
      class="flex min-h-[560px] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-700">
        {{
          approvalId === null
            ? '올바르지 않은 승인 요청이에요.'
            : resultNotFound
              ? '처리된 거래 정보를 찾을 수 없어요.'
              : '처리된 거래 정보를 불러오지 못했어요.'
        }}
      </p>
      <Button
        class="mt-lg w-full"
        label="목록으로 돌아가기"
        variant="guard-cta"
        size="guard-cta"
        @click="goToList"
      />
    </section>

    <div
      v-if="resultQuery.data.value"
      class="fixed inset-x-0 bottom-[calc(20px+env(safe-area-inset-bottom))] z-30 mx-auto w-full max-w-[390px] px-mobile-gutter"
    >
      <Button
        class="w-full"
        label="이상 거래 목록으로"
        :variant="state === 'approved' ? 'guard-cta' : 'danger'"
        size="guard-cta"
        @click="goToList"
      />
    </div>
  </main>
</template>
