<script setup lang="ts">
import { Sparkles } from '@lucide/vue'
import { Progress } from '@pay-with/ui'
import { computed } from 'vue'

import {
  getGuardTransactionTitlePresentation,
  maskGuardAccountNumber,
  type GuardTransactionDetailView,
} from '@/pages/guard/-utils/guard-transaction-detail'

const props = defineProps<{
  detail: GuardTransactionDetailView
}>()

const moneyFormatter = new Intl.NumberFormat('ko-KR')
const dateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

type DetailRow =
  | {
      kind: 'text'
      label: string
      value: string
    }
  | {
      kind: 'account'
      label: string
      testIdPrefix: 'charge' | 'recipient'
      holderName: string | null
      bankName: string | null
      accountNo: string | null
    }

const titlePresentation = computed(() =>
  getGuardTransactionTitlePresentation(props.detail),
)
const isCharge = computed(() => props.detail.type === 'CHARGE')
const isFailed = computed(() => props.detail.status === 'FAILED')
const failureLabel = computed(() => {
  if (props.detail.type === 'CHARGE') return '충전 실패'
  if (props.detail.type === 'PAYMENT') return '결제 실패'
  return '송금 실패'
})
const failureDescription = computed(() => {
  if (props.detail.type === 'CHARGE') return '실제 충전은 완료되지 않았어요.'
  if (props.detail.type === 'PAYMENT') return '실제 결제는 완료되지 않았어요.'
  return '승인된 거래지만 실제 송금은 완료되지 않았어요.'
})
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
  if (props.detail.riskLevel === null) {
    return {
      label: '미분류',
      color: '#6b7280',
      textClass: 'text-gray-700',
      cardClass: 'bg-gray-100',
    }
  }
  return {
    label: '위험',
    color: '#ff6161',
    textClass: 'text-error',
    cardClass: 'bg-[#fff0f0]',
  }
})

function formatDateTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : dateTimeFormatter.format(date)
}

function formatHolderName(value: string | null) {
  if (!value) return '-'
  return value.endsWith('님') ? value : `${value}님`
}

const detailRows = computed<DetailRow[]>(() => {
  const amountRow: DetailRow = {
    kind: 'text',
    label: '거래금액',
    value: `${moneyFormatter.format(props.detail.amount)}원`,
  }
  const occurredAtRow: DetailRow = {
    kind: 'text',
    label:
      props.detail.type === 'CHARGE'
        ? '충전일시'
        : props.detail.type === 'PAYMENT'
          ? '결제일시'
          : '이체일시',
    value: formatDateTime(props.detail.occurredAt),
  }

  if (props.detail.type === 'CHARGE') {
    return [
      amountRow,
      {
        kind: 'account',
        label: '충전',
        testIdPrefix: 'charge',
        holderName: formatHolderName(props.detail.counterpartyName),
        bankName: props.detail.bankName,
        accountNo: props.detail.accountNo,
      },
      occurredAtRow,
    ]
  }

  if (props.detail.type === 'PAYMENT') {
    return [
      amountRow,
      {
        kind: 'text',
        label: '사용처',
        value: props.detail.counterpartyName ?? '-',
      },
      occurredAtRow,
    ]
  }

  return [
    amountRow,
    {
      kind: 'text',
      label: '받는 분',
      value: props.detail.counterpartyName ?? '-',
    },
    {
      kind: 'account',
      label: '받는 계좌',
      testIdPrefix: 'recipient',
      holderName: null,
      bankName: props.detail.bankName,
      accountNo: props.detail.accountNo
        ? maskGuardAccountNumber(props.detail.accountNo)
        : null,
    },
    occurredAtRow,
  ]
})
</script>

<template>
  <section
    class="px-mobile-gutter pt-8"
    :aria-labelledby="
      isCharge && !isFailed ? undefined : 'guard-transaction-status-title'
    "
  >
    <h2
      v-if="!isCharge || isFailed"
      id="guard-transaction-status-title"
      class="text-[26px] font-bold leading-[1.2] tracking-[-0.52px]"
      :class="titlePresentation.titleClass"
    >
      {{ titlePresentation.title }}
    </h2>

    <div
      v-if="isFailed"
      class="mt-md rounded-[12px] bg-[#fff0f0] px-lg py-md text-error"
      role="alert"
    >
      <p class="text-[16px] font-bold">{{ failureLabel }}</p>
      <p class="mt-xs text-[14px] font-medium leading-5">
        {{ failureDescription }}
      </p>
    </div>

    <dl
      class="grid min-w-0 grid-cols-[minmax(0,1fr)] gap-10"
      :class="isCharge ? '' : 'mt-10'"
    >
      <div
        v-for="row in detailRows"
        :key="row.label"
        class="flex min-h-6 min-w-0 items-center justify-between gap-md overflow-hidden text-[18px] leading-[1.2] tracking-[-0.36px]"
      >
        <dt class="shrink-0 font-medium text-gray-700">{{ row.label }}</dt>
        <dd
          v-if="row.kind === 'account'"
          class="flex min-w-0 flex-1 items-center justify-end gap-xxs overflow-hidden text-right font-semibold text-black"
        >
          <span
            v-if="row.holderName"
            :data-testid="`${row.testIdPrefix}-holder-name`"
            class="shrink-0 whitespace-nowrap"
          >
            {{ row.holderName }}
          </span>
          <span
            v-if="row.bankName"
            :data-testid="`${row.testIdPrefix}-bank-name`"
            class="shrink-0 whitespace-nowrap"
          >
            {{ row.bankName }}
          </span>
          <span
            v-if="row.accountNo"
            :data-testid="`${row.testIdPrefix}-account-number`"
            class="min-w-0 truncate"
          >
            {{ row.accountNo }}
          </span>
          <span
            v-if="!row.holderName && !row.bankName && !row.accountNo"
            class="shrink-0 whitespace-nowrap"
          >
            -
          </span>
        </dd>
        <dd
          v-else
          class="min-w-0 flex-1 truncate text-right font-semibold text-black"
        >
          {{ row.value }}
        </dd>
      </div>
    </dl>

    <section
      v-if="!isCharge"
      class="relative mt-10 rounded-[12px] px-xl pt-lg pb-lg"
      :class="riskPresentation.cardClass"
      aria-labelledby="guard-transaction-analysis-title"
    >
      <span
        class="absolute top-sm right-sm flex h-[30px] min-w-[60px] items-center justify-center rounded-full px-sm text-[16px] font-bold leading-[1.2] tracking-[-0.32px] text-white"
        :style="{ backgroundColor: riskPresentation.color }"
      >
        {{ riskPresentation.label }}
      </span>
      <h3
        id="guard-transaction-analysis-title"
        class="text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-black"
      >
        이상거래 의심도
      </h3>
      <p
        class="mt-xxs text-[40px] font-semibold leading-[1.2] tracking-[-0.8px]"
        :class="riskPresentation.textClass"
      >
        {{ detail.riskScore }}점
      </p>

      <Progress
        class="mt-md w-full"
        :indicator-color="riskPresentation.color"
        label="이상거래 의심도 게이지"
        :value="detail.riskScore"
      />

      <h3
        class="mt-xl flex items-center gap-xxs text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-black"
      >
        <Sparkles class="size-5" :stroke-width="2" aria-hidden="true" />
        AI 분석 결과
      </h3>
      <p
        v-if="detail.summary"
        class="mt-sm text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-black"
      >
        {{ detail.summary }}
      </p>
      <ul
        v-if="detail.reasons.length"
        class="mt-sm list-disc pl-xl text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-black"
      >
        <li v-for="reason in detail.reasons" :key="reason">{{ reason }}</li>
      </ul>
      <p
        v-else-if="!detail.summary"
        class="mt-sm text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-gray-700"
      >
        세부 분석 결과가 없어요.
      </p>
    </section>

    <section
      v-if="isFailed"
      class="mt-md rounded-[12px] border border-error/20 bg-white px-lg py-md"
      aria-labelledby="guard-transfer-failure-reason-title"
    >
      <h3
        id="guard-transfer-failure-reason-title"
        class="text-[16px] font-bold text-error"
      >
        실패 사유
      </h3>
      <p class="mt-xs text-[14px] font-medium leading-5 text-gray-700">
        {{ detail.failureReason }}
      </p>
    </section>
  </section>
</template>
