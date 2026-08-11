<script setup lang="ts">
import { AlertTriangle, Check, X } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import GuardApprovalHeader from '@/pages/guard/approval-requests/-components/GuardApprovalHeader.vue'

const route = useRoute()
const router = useRouter()
const transactionId = computed(() => {
  const value = Number(route.query.transactionId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const wardId = computed(() => {
  const value = Number(route.query.wardId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const decision = computed(() => {
  if (route.query.decision === 'approved') return 'approved'
  if (route.query.decision === 'rejected') return 'rejected'
  return null
})
const transferStatus = computed(() => {
  if (route.query.transferStatus === 'completed') return 'completed'
  if (route.query.transferStatus === 'failed') return 'failed'
  return null
})
const failureReason = computed(() =>
  typeof route.query.failureReason === 'string'
    ? route.query.failureReason
    : null,
)
const hasResult = computed(
  () =>
    transactionId.value !== null &&
    wardId.value !== null &&
    decision.value !== null &&
    (decision.value === 'rejected' || transferStatus.value !== null),
)
const isApproved = computed(() => decision.value === 'approved')
const isTransferFailed = computed(
  () => isApproved.value && transferStatus.value === 'failed',
)

function goToList() {
  router.replace({
    name: 'guard-approval-requests',
    query: {
      wardId: wardId.value ?? undefined,
      status: decision.value ?? undefined,
    },
  })
}

function confirm() {
  if (!hasResult.value) {
    goToList()
    return
  }

  router.replace({
    name: 'guard-transaction-detail',
    params: { id: transactionId.value! },
    query: {
      wardId: wardId.value!,
      approvalStatus: decision.value!,
      source: 'approval',
      failureReason: isTransferFailed.value
        ? (failureReason.value ?? undefined)
        : undefined,
    },
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
  <main class="flex min-h-screen flex-col bg-white">
    <GuardApprovalHeader
      title=""
      back-label="이상 거래 목록으로 돌아가기"
      @back="goToList"
    />

    <section
      v-if="hasResult"
      class="flex flex-1 flex-col items-center pt-[100px] text-center"
      aria-labelledby="approval-decision-complete-title"
    >
      <span
        class="flex size-20 items-center justify-center rounded-full text-white"
        :class="
          isTransferFailed
            ? 'bg-warning'
            : isApproved
              ? 'bg-primary-500'
              : 'bg-error'
        "
        aria-hidden="true"
      >
        <AlertTriangle
          v-if="isTransferFailed"
          class="size-11"
          :stroke-width="3"
        />
        <Check v-else-if="isApproved" class="size-12" :stroke-width="3.5" />
        <X v-else class="size-12" :stroke-width="3.5" />
      </span>
      <h1
        id="approval-decision-complete-title"
        class="mt-lg text-[28px] font-semibold leading-[1.2] tracking-[-0.56px] text-black"
      >
        <template v-if="isTransferFailed">
          거래를 승인했지만<br />송금에 실패했어요
        </template>
        <template v-else-if="isApproved">
          이상 거래를 승인하고<br />송금을 완료했어요
        </template>
        <template v-else> 이상 거래를 거절했어요 </template>
      </h1>

      <div
        v-if="isTransferFailed"
        class="mx-mobile-gutter mt-xl self-stretch rounded-[12px] bg-[#fff7ef] px-lg py-md text-left"
        role="alert"
      >
        <p class="text-[15px] font-bold text-warning">송금 실패 사유</p>
        <p class="mt-xs text-[14px] font-medium leading-5 text-gray-700">
          {{ failureReason ?? '피보호자의 잔액과 거래 상태를 확인해 주세요.' }}
        </p>
        <p class="mt-sm text-[13px] font-medium leading-5 text-gray-600">
          잔액을 충전한 뒤 피보호자가 다시 송금해야 해요.
        </p>
      </div>

      <p
        v-else-if="isApproved"
        class="mt-md text-[15px] font-medium text-gray-600"
      >
        정상적으로 송금 처리됐어요.
      </p>
    </section>

    <section
      v-else
      class="flex flex-1 items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-700"
      role="alert"
    >
      처리 결과를 확인할 수 없어요.
    </section>

    <div
      class="grid gap-sm px-mobile-gutter pb-[calc(20px+env(safe-area-inset-bottom))]"
    >
      <Button
        v-if="isTransferFailed"
        class="w-full"
        label="잔액 충전해주기"
        variant="guard-cta"
        size="guard-cta"
        @click="chargeWardWallet"
      />
      <Button
        v-if="isTransferFailed"
        class="w-full"
        label="피보호자에게 연락하기"
        variant="outline-primary"
        size="guard-cta"
        @click="contactWard"
      />
      <Button
        class="w-full"
        :label="isTransferFailed ? '거래 상세 확인' : '확인'"
        :variant="
          isTransferFailed
            ? 'outline-primary'
            : isApproved || !hasResult
              ? 'guard-cta'
              : 'danger'
        "
        size="guard-cta"
        @click="confirm"
      />
    </div>
  </main>
</template>
