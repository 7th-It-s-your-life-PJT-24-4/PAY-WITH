<script setup lang="ts">
import { Check, X } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { guardApprovalResultOptions } from '@/lib/query/guard/approval'
import GuardApprovalHeader from '@/pages/guard/approval-requests/-components/GuardApprovalHeader.vue'

const route = useRoute()
const router = useRouter()
const approvalId = computed(() => {
  const value = Number(route.params.approvalId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const resultQuery = useQuery(guardApprovalResultOptions(approvalId))
const isApproved = computed(
  () => resultQuery.data.value?.decision.status === 'APPROVED',
)

function goToList() {
  router.replace({
    name: 'guard-approval-requests',
    query: resultQuery.data.value
      ? {
          wardId: resultQuery.data.value.detail.wardId,
          status: isApproved.value ? 'approved' : 'rejected',
        }
      : undefined,
  })
}

function confirm() {
  if (!approvalId.value || !resultQuery.data.value) {
    goToList()
    return
  }
  router.replace({
    name: 'guard-approval-request-result',
    params: { approvalId: approvalId.value },
  })
}
</script>

<template>
  <main class="flex min-h-screen flex-col bg-white">
    <GuardApprovalHeader
      title=""
      back-label="이상 거래 목록으로 돌아가기"
      @back="goToList"
    />

    <section
      v-if="resultQuery.data.value"
      class="flex flex-1 flex-col items-center pt-[100px] text-center"
      aria-labelledby="approval-decision-complete-title"
    >
      <span
        class="flex size-20 items-center justify-center rounded-full text-white"
        :class="isApproved ? 'bg-primary-500' : 'bg-error'"
        aria-hidden="true"
      >
        <Check v-if="isApproved" class="size-12" :stroke-width="3.5" />
        <X v-else class="size-12" :stroke-width="3.5" />
      </span>
      <h1
        id="approval-decision-complete-title"
        class="mt-lg text-[28px] font-semibold leading-[1.2] tracking-[-0.56px] text-black"
      >
        이상 거래를<br />
        {{ isApproved ? '승인했어요' : '거절했어요' }}
      </h1>
    </section>

    <section
      v-else-if="resultQuery.isPending.value"
      class="flex flex-1 items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-500"
      aria-busy="true"
    >
      처리 결과를 불러오는 중이에요.
    </section>

    <section
      v-else
      class="flex flex-1 items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-700"
      role="alert"
    >
      처리 결과를 확인할 수 없어요.
    </section>

    <div class="px-mobile-gutter pb-[calc(20px+env(safe-area-inset-bottom))]">
      <Button
        class="w-full"
        label="확인"
        :variant="
          isApproved || !resultQuery.data.value ? 'guard-cta' : 'danger'
        "
        size="guard-cta"
        @click="confirm"
      />
    </div>
  </main>
</template>
