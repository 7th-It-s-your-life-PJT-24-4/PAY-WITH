<script setup lang="ts">
import { Sparkles } from '@lucide/vue'
import { Button, Progress } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import backIconUrl from '@/assets/icons/transaction-detail-back.svg'
import headerSpacerIconUrl from '@/assets/icons/transaction-detail-header-spacer.svg'
import { guardTransactionDetailOptions } from '@/lib/query/guard/transaction'
import { parsePositiveRouteId } from '@/pages/guard/-utils/guard-route'
import {
  formatApprovalDateTime,
  formatApprovalMoney,
} from '@/pages/guard/approval-requests/-utils/approval-format'
import { useGuardStore } from '@/stores/guard.store'

type ApprovalResultStatus = 'approved' | 'rejected' | 'canceled' | 'expired'

const riskPresentation = {
  SAFE: {
    title: '안심할 수 있는 거래에요',
    label: '안전',
    cardClass: 'bg-[#f1fff2]',
    textClass: 'text-success',
    color: '#2fa737',
  },
  CAUTION: {
    title: '주의가 필요한 거래에요',
    label: '주의',
    cardClass: 'bg-[#fff7ef]',
    textClass: 'text-warning',
    color: '#ff9f3f',
  },
  DANGER: {
    title: '이상 거래가 발생했어요',
    label: '위험',
    cardClass: 'bg-[#fff0f0]',
    textClass: 'text-error',
    color: '#ff6161',
  },
} as const

const resultTitle: Record<ApprovalResultStatus, string> = {
  approved: '승인된 이상 거래에요',
  rejected: '거절된 이상 거래에요',
  canceled: '취소된 이상 거래에요',
  expired: '만료된 이상 거래에요',
}

const riskReasonLabels: Record<string, string> = {
  BL_FRAUD_ACCOUNT: '사기 신고 이력이 있는 계좌예요.',
  BL_REJECTED_RECIPIENT: '보호자가 이전에 거절한 수취인 계좌예요.',
  DIVISION_TRANSFER: '짧은 시간에 나누어 송금한 패턴이 감지됐어요.',
  HIGH_AMOUNT_L1: '평소보다 큰 금액의 송금이에요.',
  HIGH_AMOUNT_L2: '고액 송금 패턴이 감지됐어요.',
  HIGH_AMOUNT_L3: '매우 큰 금액의 송금이에요.',
  NEW_RECIPIENT: '처음 송금하는 수취인이에요.',
  NIGHT_TIME_DEEP: '심야 시간대의 송금이에요.',
  NIGHT_TIME_LATE: '늦은 시간대의 송금이에요.',
  PENDING_APPROVAL_EXISTS: '승인 대기 중에 추가로 요청한 송금이에요.',
  REPEATED: '짧은 시간에 반복된 송금이에요.',
  SAFE_ACCOUNT_CHECK: '등록한 안전계좌로 보내는 송금이에요.',
  SUSPICIOUS_MEMO: '메모에서 위험 키워드가 감지됐어요.',
  PAY_SPLIT_PAYMENT: '짧은 시간 안에 여러 번 나누어 결제했어요.',
  PAY_HIGH_AMOUNT_L3: '평소보다 매우 큰 금액의 결제예요.',
  PAY_HIGH_AMOUNT_L2: '평소보다 큰 금액의 결제예요.',
  PAY_HIGH_AMOUNT_L1: '일반적인 생활 결제보다 큰 금액이에요.',
  PAY_RISKY_CATEGORY: '환금성이 높은 위험 업종에서 결제했어요.',
  PAY_PENDING_APPROVAL: '승인 대기 중인 송금이 있는 상태에서 결제했어요.',
  PAY_GIFT_CARD_AMOUNT: '상품권 의심 단위 금액으로 결제했어요.',
  PAY_NIGHT_DEEP: '자정 이후 늦은 시간에 결제했어요.',
  PAY_NIGHT_LATE: '늦은 밤 시간대에 결제했어요.',
  PAY_IMPOSSIBLE_TRAVEL: '물리적으로 이동하기 어려운 위치에서 결제했어요.',
}

const route = useRoute()
const router = useRouter()
const guardStore = useGuardStore()
const transactionId = computed(() => parsePositiveRouteId(route.params.id))
const routeWardId = computed(() => parsePositiveRouteId(route.query.wardId))
const wardId = computed(() => routeWardId.value ?? guardStore.activeWardId)
const approvalStatus = computed<ApprovalResultStatus | null>(() => {
  const status = route.query.status
  return status === 'approved' ||
    status === 'rejected' ||
    status === 'canceled' ||
    status === 'expired'
    ? status
    : null
})
const transactionQuery = useQuery(
  guardTransactionDetailOptions(wardId, transactionId),
)
const detail = computed(() => transactionQuery.data.value ?? null)
const isCharge = computed(() => detail.value?.type === 'CHARGE')
const riskLevel = computed(() => detail.value?.riskLevel ?? 'SAFE')
const presentation = computed(() => riskPresentation[riskLevel.value])
const title = computed(
  () =>
    (approvalStatus.value && resultTitle[approvalStatus.value]) ??
    presentation.value.title,
)
const titleClass = computed(() =>
  approvalStatus.value === 'approved'
    ? 'text-primary-500'
    : approvalStatus.value
      ? 'text-error'
      : presentation.value.textClass,
)
const riskScore = computed(() => detail.value?.riskAnalysis?.riskScore ?? 0)
const riskReasons = computed(() =>
  (detail.value?.riskAnalysis?.reasons ?? []).map(
    (reason) => riskReasonLabels[reason] ?? reason,
  ),
)
const detailRows = computed(() => {
  if (!detail.value) return []

  const accountDigits = detail.value.accountNo?.replaceAll(/\D/g, '') ?? ''
  const amountRow = {
    label: '거래금액',
    value: formatApprovalMoney(detail.value.amount),
  }
  const occurredAtRow = {
    label: '이체일시',
    value: formatApprovalDateTime(detail.value.occurredAt),
  }

  if (detail.value.type === 'CHARGE') {
    const chargeAccount = [detail.value.bankName, accountDigits.slice(0, 4)]
      .filter(Boolean)
      .join('')
    const chargeDescription = chargeAccount
      ? `${detail.value.counterpartyName ?? '-'}(${chargeAccount})`
      : (detail.value.counterpartyName ?? '-')

    return [
      amountRow,
      { label: '충전', value: chargeDescription },
      occurredAtRow,
    ]
  }

  const accountSuffix = accountDigits ? accountDigits.slice(-4) : ''
  const withdrawalAccount = [detail.value.bankName, accountSuffix]
    .filter(Boolean)
    .join(' ')

  return [
    amountRow,
    { label: '사용처', value: detail.value.counterpartyName ?? '-' },
    {
      label: '출금처',
      value: withdrawalAccount || '-',
    },
    occurredAtRow,
  ]
})
const hasValidParams = computed(
  () => transactionId.value !== null && wardId.value !== null,
)

function goBack() {
  if (route.query.source === 'approval') {
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
</script>

<template>
  <main class="min-h-screen bg-white pb-28">
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
      v-else-if="transactionQuery.isError.value || !detail"
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

    <section
      v-else
      class="px-mobile-gutter pt-8"
      :aria-labelledby="isCharge ? undefined : 'transaction-risk-title'"
    >
      <h2
        v-if="!isCharge"
        id="transaction-risk-title"
        class="text-[28px] font-bold leading-[1.2] tracking-[-0.56px]"
        :class="titleClass"
      >
        {{ title }}
      </h2>

      <dl class="grid gap-10" :class="{ 'mt-10': !isCharge }">
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
        v-if="!isCharge"
        class="relative mt-10 rounded-[12px] px-xl pt-lg pb-lg"
        :class="presentation.cardClass"
        aria-labelledby="risk-analysis-title"
      >
        <span
          class="absolute top-sm right-sm flex h-[30px] min-w-[60px] items-center justify-center rounded-full px-sm text-[16px] font-bold leading-[1.2] tracking-[-0.32px] text-white"
          :style="{ backgroundColor: presentation.color }"
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
          :class="presentation.textClass"
        >
          {{ riskScore }}점
        </p>

        <Progress
          class="mt-md w-full"
          :indicator-color="presentation.color"
          label="이상거래 의심도 게이지"
          :value="riskScore"
        />

        <template v-if="riskLevel === 'DANGER'">
          <h3
            class="mt-xl flex items-center gap-xxs text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-black"
          >
            <Sparkles class="size-5" :stroke-width="2" aria-hidden="true" />
            AI 분석 결과
          </h3>
          <p
            v-if="detail.riskAnalysis?.summary"
            class="mt-sm text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-black"
          >
            {{ detail.riskAnalysis.summary }}
          </p>
          <ul
            v-if="riskReasons.length"
            class="mt-sm list-disc pl-xl text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-black"
          >
            <li v-for="reason in riskReasons" :key="reason">{{ reason }}</li>
          </ul>
          <p
            v-else-if="!detail.riskAnalysis?.summary"
            class="mt-sm text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-gray-700"
          >
            세부 분석 결과가 없어요.
          </p>
        </template>
      </section>
    </section>

    <div
      v-if="detail"
      class="fixed inset-x-0 bottom-[calc(20px+env(safe-area-inset-bottom))] z-30 mx-auto w-full max-w-[390px] px-mobile-gutter"
    >
      <Button
        v-if="route.query.source === 'approval'"
        class="w-full"
        label="이상 거래 목록으로"
        :variant="approvalStatus === 'approved' ? 'guard-cta' : 'danger'"
        size="guard-cta"
        @click="goBack"
      />
      <Button
        v-else
        class="w-full shadow-[0_10px_15px_-3px_rgb(0_0_0/10%),0_4px_6px_-4px_rgb(0_0_0/10%)]"
        label="연락하기"
        variant="guard-cta"
        size="guard-cta"
      />
    </div>
  </main>
</template>
