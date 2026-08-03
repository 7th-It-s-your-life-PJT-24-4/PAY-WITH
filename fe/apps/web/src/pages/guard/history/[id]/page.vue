<script setup lang="ts">
import { Button, ConfirmModal, Progress } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import backIconUrl from '@/assets/icons/transaction-detail-back.svg'
import headerSpacerIconUrl from '@/assets/icons/transaction-detail-header-spacer.svg'
import {
  getMockGuardTransaction,
  getMockGuardTransactionDetail,
  type GuardTransaction,
} from '@/mocks/guard-home.mock'

type RiskPresentation = {
  title: string
  label: string
  cardClass: string
  colorClass: string
  colorValue: string
  primaryActionLabel?: string
  secondaryActionLabel?: string
}

type TransactionDecision = 'approved' | 'rejected'

const riskPresentations: Record<GuardTransaction['status'], RiskPresentation> =
  {
    danger: {
      title: '이상 거래가 발생했어요',
      label: '위험',
      cardClass: 'bg-[#fff0f0]',
      colorClass: 'text-error',
      colorValue: '#ff6161',
      primaryActionLabel: '승인하기',
      secondaryActionLabel: '거절하기',
    },
    warning: {
      title: '주의가 필요한 거래에요',
      label: '주의',
      cardClass: 'bg-[#fff7ef]',
      colorClass: 'text-warning',
      colorValue: '#ff9f3f',
      primaryActionLabel: '연락하기',
    },
    safe: {
      title: '안심할 수 있는 거래에요',
      label: '안전',
      cardClass: 'bg-[#f1fff2]',
      colorClass: 'text-success',
      colorValue: '#2fa737',
    },
  }

const route = useRoute()
const router = useRouter()
const isDecisionConfirmOpen = ref(false)
const pendingDecision = ref<TransactionDecision>('approved')
const transactionId = computed(() => String(route.params.transactionId))
const transaction = computed(() => getMockGuardTransaction(transactionId.value))
const detail = computed(() =>
  getMockGuardTransactionDetail(transactionId.value),
)
const completedDecision = computed<TransactionDecision | null>(() => {
  if (route.query.decision === 'approved') return 'approved'
  if (route.query.decision === 'rejected') return 'rejected'
  return null
})
const presentation = computed(() => {
  if (!transaction.value) return null

  const basePresentation = riskPresentations[transaction.value.status]
  if (!completedDecision.value) return basePresentation

  return {
    ...basePresentation,
    title:
      completedDecision.value === 'approved'
        ? '승인된 이상 거래에요'
        : '거절된 이상 거래에요',
    colorClass:
      completedDecision.value === 'approved'
        ? 'text-primary-500'
        : 'text-error',
    primaryActionLabel: undefined,
    secondaryActionLabel: undefined,
  }
})
const detailRows = computed(() => {
  if (!detail.value) return []

  return [
    {
      label: '거래금액',
      value: `${new Intl.NumberFormat('ko-KR').format(detail.value.amount)}원`,
    },
    { label: '사용처', value: detail.value.merchantName },
    { label: '출금처', value: detail.value.withdrawalAccountLabel },
    { label: '이체일시', value: detail.value.occurredAt },
  ]
})

function goBack() {
  router.replace({ name: 'guard-history' })
}

function openDecisionConfirm(decision: TransactionDecision) {
  pendingDecision.value = decision
  isDecisionConfirmOpen.value = true
}

function confirmDecision() {
  isDecisionConfirmOpen.value = false
  router.push({
    name: 'guard-transaction-decision-complete',
    params: { transactionId: transactionId.value },
    query: { decision: pendingDecision.value },
  })
}
</script>

<template>
  <main v-if="detail && presentation" class="min-h-screen bg-white pb-28">
    <header class="grid h-11 grid-cols-[44px_1fr_44px] items-center">
      <button
        class="flex size-11 items-center justify-center"
        type="button"
        aria-label="거래 내역으로 돌아가기"
        @click="goBack"
      >
        <span class="flex size-6 items-center justify-center">
          <img
            class="h-[15.84px] w-[7.097px]"
            :src="backIconUrl"
            alt=""
            aria-hidden="true"
          />
        </span>
      </button>
      <h1
        class="text-center text-[20px] font-semibold leading-[1.2] tracking-[-0.4px] text-black"
      >
        거래 상세
      </h1>
      <img
        class="size-11"
        :src="headerSpacerIconUrl"
        alt=""
        aria-hidden="true"
      />
    </header>

    <section
      class="px-mobile-gutter pt-8"
      aria-labelledby="transaction-risk-title"
    >
      <h2
        id="transaction-risk-title"
        class="text-[28px] font-bold leading-[1.2] tracking-[-0.56px]"
        :class="presentation.colorClass"
      >
        {{ presentation.title }}
      </h2>

      <dl class="mt-10 grid gap-10">
        <div
          v-for="row in detailRows"
          :key="row.label"
          class="flex min-h-6 items-center justify-between gap-md text-[20px] leading-[1.2] tracking-[-0.4px]"
        >
          <dt class="font-medium text-gray-700">{{ row.label }}</dt>
          <dd class="text-right font-semibold text-black">{{ row.value }}</dd>
        </div>
      </dl>

      <section
        class="relative mt-10 h-fit rounded-[12px] px-xl pt-lg pb-lg"
        :class="presentation.cardClass"
        aria-labelledby="risk-analysis-title"
      >
        <span
          class="absolute top-sm right-sm flex h-[30px] w-[60px] items-center justify-center rounded-full text-[16px] font-bold leading-[1.2] tracking-[-0.32px] text-white"
          :style="{ backgroundColor: presentation.colorValue }"
        >
          {{ presentation.label }}
        </span>
        <h3
          id="risk-analysis-title"
          class="text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-black"
        >
          이상거래 의심도
        </h3>
        <p
          class="mt-xxs text-[40px] font-semibold leading-[1.2] tracking-[-0.8px]"
          :class="presentation.colorClass"
        >
          {{ detail.riskScore }}점
        </p>

        <Progress
          class="mt-md w-full"
          :indicator-color="presentation.colorValue"
          label="이상거래 의심도 게이지"
          :value="detail.riskScore"
        />

        <h3
          class="mt-xl text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-black"
        >
          AI 분석 결과
        </h3>
        <ul
          class="mt-sm list-disc pl-xl text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-black"
        >
          <li v-for="result in detail.analysisResults" :key="result">
            {{ result }}
          </li>
        </ul>
      </section>
    </section>

    <div
      v-if="presentation.primaryActionLabel"
      class="fixed inset-x-0 bottom-[calc(44px+env(safe-area-inset-bottom))] z-30 mx-auto w-full max-w-[390px] px-mobile-gutter"
      :class="
        presentation.secondaryActionLabel ? 'grid grid-cols-2 gap-[15px]' : ''
      "
    >
      <Button
        class="w-full"
        :label="presentation.primaryActionLabel"
        variant="guard-cta"
        size="guard-cta"
        @click="
          transaction?.status === 'danger' && openDecisionConfirm('approved')
        "
      />
      <Button
        v-if="presentation.secondaryActionLabel"
        class="w-full"
        :label="presentation.secondaryActionLabel"
        variant="outline-primary"
        size="guard-cta"
        @click="openDecisionConfirm('rejected')"
      />
    </div>
  </main>

  <ConfirmModal
    v-model:open="isDecisionConfirmOpen"
    :title="
      pendingDecision === 'approved'
        ? '거래를 승인할까요?'
        : '거래를 거절할까요?'
    "
    cancel-label="취소"
    :confirm-label="pendingDecision === 'approved' ? '승인' : '거절'"
    :confirm-variant="pendingDecision === 'approved' ? 'primary' : 'danger'"
    @confirm="confirmDecision"
  />
</template>
