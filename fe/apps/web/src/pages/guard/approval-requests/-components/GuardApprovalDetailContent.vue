<script setup lang="ts">
import { Sparkles } from '@lucide/vue'
import { Progress } from '@pay-with/ui'
import { computed } from 'vue'

import {
  formatApprovalDateTime,
  formatApprovalMoney,
  maskApprovalAccountNumber,
} from '@/pages/guard/approval-requests/-utils/approval-format'
import type { ApprovalRequestDetail } from '@/schemas/approval.schema'

type DecisionState = 'pending' | 'approved' | 'rejected'

const props = withDefaults(
  defineProps<{
    detail: ApprovalRequestDetail
    state?: DecisionState
    transferFailureReason?: string | null
  }>(),
  {
    state: 'pending',
    transferFailureReason: null,
  },
)

const title = computed(() => {
  if (props.state === 'approved') return '승인된 이상 거래에요'
  if (props.state === 'rejected') return '거절된 이상 거래에요'
  return '이상 거래가 발생했어요'
})

const titleColorClass = computed(() =>
  props.state === 'approved' ? 'text-primary-500' : 'text-error',
)

const riskPresentation = computed(() => {
  if (props.detail.riskLevel === 'SAFE') {
    return {
      label: '안전',
      color: '#2fa737',
      textClass: 'text-success',
      cardClass: 'bg-[#f1fff2]',
    }
  }
  if (props.detail.riskLevel === 'CAUTION') {
    return {
      label: '주의',
      color: '#ff9f3f',
      textClass: 'text-warning',
      cardClass: 'bg-[#fff7ef]',
    }
  }
  return {
    label: '위험',
    color: '#ff6161',
    textClass: 'text-error',
    cardClass: 'bg-[#fff0f0]',
  }
})

const detailRows = computed(() => [
  { label: '거래금액', value: formatApprovalMoney(props.detail.amount) },
  { label: '사용처', value: props.detail.holderName ?? '-' },
  {
    label: '출금처',
    value: `${props.detail.bankName ?? '은행 정보 없음'} ${maskApprovalAccountNumber(props.detail.accountNo)}`,
  },
  {
    label: '이체일시',
    value: formatApprovalDateTime(props.detail.requestedAt),
  },
])

const riskScore = computed(() => props.detail.totalScore ?? 0)
</script>

<template>
  <section class="px-mobile-gutter pt-8" aria-labelledby="approval-risk-title">
    <h2
      id="approval-risk-title"
      class="text-[28px] font-bold leading-[1.2] tracking-[-0.56px]"
      :class="titleColorClass"
    >
      {{ title }}
    </h2>

    <dl class="mt-10 grid gap-10">
      <div
        v-for="row in detailRows"
        :key="row.label"
        class="flex min-h-6 items-center justify-between gap-md text-[20px] leading-[1.2] tracking-[-0.4px]"
      >
        <dt class="shrink-0 font-medium text-gray-700">{{ row.label }}</dt>
        <dd class="truncate text-right font-semibold text-black">
          {{ row.value }}
        </dd>
      </div>
    </dl>

    <section
      class="relative mt-10 rounded-[12px] px-xl pt-lg pb-lg"
      :class="riskPresentation.cardClass"
      aria-labelledby="approval-analysis-title"
    >
      <span
        class="absolute top-sm right-sm flex h-[30px] min-w-[60px] items-center justify-center rounded-full px-sm text-[16px] font-bold leading-[1.2] tracking-[-0.32px] text-white"
        :style="{ backgroundColor: riskPresentation.color }"
      >
        {{ riskPresentation.label }}
      </span>
      <h3
        id="approval-analysis-title"
        class="text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-black"
      >
        이상거래 의심도
      </h3>
      <p
        class="mt-xxs text-[40px] font-semibold leading-[1.2] tracking-[-0.8px]"
        :class="riskPresentation.textClass"
      >
        {{ riskScore }}점
      </p>

      <Progress
        class="mt-md w-full"
        :indicator-color="riskPresentation.color"
        label="이상거래 의심도 게이지"
        :value="riskScore"
      />

      <h3
        class="mt-xl flex items-center gap-xxs text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-black"
      >
        <Sparkles class="size-5" :stroke-width="2" aria-hidden="true" />
        AI 요약
      </h3>
      <ul
        v-if="detail.ruleHits.length"
        class="mt-sm list-disc pl-xl text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-black"
      >
        <li v-for="rule in detail.ruleHits" :key="rule.ruleCode">
          {{ rule.description }}
        </li>
      </ul>
      <p
        v-else
        class="mt-sm text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-gray-700"
      >
        세부 분석 결과가 없어요.
      </p>
    </section>

    <p
      v-if="state === 'approved' && transferFailureReason"
      class="mt-md rounded-[12px] bg-[#fff0f0] px-lg py-md text-[14px] font-medium leading-5 text-error"
      role="alert"
    >
      거래 승인은 완료됐지만 송금 처리에 실패했어요.<br />
      {{ transferFailureReason }}
    </p>
  </section>
</template>
