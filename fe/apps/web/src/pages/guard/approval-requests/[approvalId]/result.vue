<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import GuardApprovalDetailContent from '@/pages/guard/approval-requests/-components/GuardApprovalDetailContent.vue'
import GuardApprovalHeader from '@/pages/guard/approval-requests/-components/GuardApprovalHeader.vue'
import { getApprovalDecisionSnapshot } from '@/pages/guard/approval-requests/-utils/approval-decision-snapshot'

const route = useRoute()
const router = useRouter()
const approvalId = computed(() => {
  const value = Number(route.params.approvalId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const snapshot = computed(() =>
  approvalId.value ? getApprovalDecisionSnapshot(approvalId.value) : null,
)
const state = computed(() =>
  snapshot.value?.decision.status === 'APPROVED' ? 'approved' : 'rejected',
)
const transferFailureReason = computed(() => {
  const transfer = snapshot.value?.decision.transfer
  return transfer?.status === 'FAILED' ? transfer.failureReason : null
})

function goToList() {
  router.replace({ name: 'guard-approval-requests' })
}
</script>

<template>
  <main class="min-h-screen bg-white pb-10">
    <GuardApprovalHeader @back="goToList" />

    <GuardApprovalDetailContent
      v-if="snapshot"
      :detail="snapshot.detail"
      :state="state"
      :transfer-failure-reason="transferFailureReason"
    />

    <section
      v-else
      class="flex min-h-[560px] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-700">
        처리된 거래 정보를 확인할 수 없어요.
      </p>
      <Button
        class="mt-lg w-full"
        label="목록으로 돌아가기"
        variant="guard-cta"
        size="guard-cta"
        @click="goToList"
      />
    </section>
  </main>
</template>
