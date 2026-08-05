<script setup lang="ts">
import { Button, ConfirmModal } from '@pay-with/ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { HTTPError } from 'ky'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  approveApprovalRequest,
  rejectApprovalRequest,
} from '@/api/approval-requests'
import { getApiErrorMessage } from '@/api/error'
import { guardHomeKeys } from '@/lib/query/guard/home'
import {
  guardApprovalDetailOptions,
  guardApprovalKeys,
} from '@/lib/query/guard/approval'
import GuardApprovalDetailContent from '@/pages/guard/approval-requests/-components/GuardApprovalDetailContent.vue'
import GuardApprovalHeader from '@/pages/guard/approval-requests/-components/GuardApprovalHeader.vue'
import { saveApprovalDecisionSnapshot } from '@/pages/guard/approval-requests/-utils/approval-decision-snapshot'

type TransactionDecision = 'approved' | 'rejected'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const approvalId = computed(() => {
  const value = Number(route.params.approvalId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const approvalQuery = useQuery(guardApprovalDetailOptions(approvalId))
const isDecisionConfirmOpen = ref(false)
const pendingDecision = ref<TransactionDecision>('approved')
const errorMessage = ref('')

const decisionMutation = useMutation({
  mutationFn: ({
    decision,
    id,
  }: {
    decision: TransactionDecision
    id: number
  }) =>
    decision === 'approved'
      ? approveApprovalRequest(id)
      : rejectApprovalRequest(id),
})

const approvalNotFound = computed(
  () =>
    approvalQuery.error.value instanceof HTTPError &&
    (approvalQuery.error.value.response.status === 404 ||
      approvalQuery.error.value.response.status === 409),
)

function goToList() {
  router.replace({
    name: 'guard-approval-requests',
    query: approvalQuery.data.value
      ? { wardId: approvalQuery.data.value.wardId }
      : undefined,
  })
}

function openDecisionConfirm(decision: TransactionDecision) {
  pendingDecision.value = decision
  errorMessage.value = ''
  isDecisionConfirmOpen.value = true
}

async function confirmDecision() {
  const id = approvalId.value
  const detail = approvalQuery.data.value
  if (!id || !detail || decisionMutation.isPending.value) return

  try {
    const decision = await decisionMutation.mutateAsync({
      id,
      decision: pendingDecision.value,
    })
    saveApprovalDecisionSnapshot(detail, decision)
    isDecisionConfirmOpen.value = false
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: guardApprovalKeys.lists() }),
      queryClient.invalidateQueries({ queryKey: guardHomeKeys.all }),
    ])
    await router.replace({
      name: 'guard-approval-decision-complete',
      params: { approvalId: id },
    })
  } catch (error) {
    isDecisionConfirmOpen.value = false
    errorMessage.value = await getApiErrorMessage(
      error,
      '거래를 처리하지 못했어요. 다시 시도해 주세요.',
    )
  }
}
</script>

<template>
  <main class="min-h-screen bg-white pb-32">
    <GuardApprovalHeader
      title="이상 거래 상세"
      back-label="이상 거래 목록으로 돌아가기"
      @back="goToList"
    />

    <section
      v-if="approvalId === null"
      class="flex min-h-[560px] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-700">
        올바르지 않은 승인 요청이에요.
      </p>
      <Button
        class="mt-lg w-full"
        label="목록으로 돌아가기"
        variant="guard-cta"
        size="guard-cta"
        @click="goToList"
      />
    </section>

    <section
      v-else-if="approvalQuery.isPending.value"
      class="flex min-h-[560px] items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-500"
      aria-busy="true"
    >
      거래 상세를 불러오는 중이에요.
    </section>

    <section
      v-else-if="approvalNotFound"
      class="flex min-h-[560px] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-700">
        이미 처리됐거나 만료된 거래예요.
      </p>
      <Button
        class="mt-lg w-full"
        label="목록으로 돌아가기"
        variant="guard-cta"
        size="guard-cta"
        @click="goToList"
      />
    </section>

    <section
      v-else-if="approvalQuery.isError.value || !approvalQuery.data.value"
      class="flex min-h-[560px] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-700">
        거래 상세를 불러오지 못했어요.
      </p>
      <button
        class="mt-md min-h-11 px-md text-[16px] font-semibold text-primary-500"
        type="button"
        @click="approvalQuery.refetch()"
      >
        다시 시도
      </button>
    </section>

    <template v-else>
      <GuardApprovalDetailContent :detail="approvalQuery.data.value" />

      <p
        v-if="errorMessage"
        class="mx-mobile-gutter mt-md text-center text-[14px] font-medium text-error"
        role="alert"
      >
        {{ errorMessage }}
      </p>

      <div
        class="fixed inset-x-0 bottom-[calc(20px+env(safe-area-inset-bottom))] z-30 mx-auto grid w-full max-w-[390px] grid-cols-2 gap-[15px] px-mobile-gutter"
      >
        <Button
          class="w-full"
          label="승인하기"
          variant="guard-cta"
          size="guard-cta"
          @click="openDecisionConfirm('approved')"
        />
        <Button
          class="w-full"
          label="거절하기"
          variant="outline-primary"
          size="guard-cta"
          @click="openDecisionConfirm('rejected')"
        />
      </div>
    </template>
  </main>

  <ConfirmModal
    v-model:open="isDecisionConfirmOpen"
    :title="
      pendingDecision === 'approved'
        ? '거래를 승인할까요?'
        : '거래를 거절할까요?'
    "
    cancel-label="취소"
    :confirm-label="
      decisionMutation.isPending.value
        ? '처리 중'
        : pendingDecision === 'approved'
          ? '승인'
          : '거절'
    "
    :confirm-disabled="decisionMutation.isPending.value"
    confirm-variant="primary"
    @confirm="confirmDecision"
  />
</template>
