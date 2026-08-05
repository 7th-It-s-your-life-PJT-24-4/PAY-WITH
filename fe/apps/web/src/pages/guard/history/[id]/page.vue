<script setup lang="ts">
import { Button, Progress } from '@pay-with/ui'
import { queryOptions, useQuery } from '@tanstack/vue-query'
import { computed, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getUser } from '@/api/users'
import backIconUrl from '@/assets/icons/transaction-detail-back.svg'
import headerSpacerIconUrl from '@/assets/icons/transaction-detail-header-spacer.svg'
import { guardTransactionDetailOptions } from '@/lib/query/guard/transactions'
import type { TransactionRiskLevel } from '@/schemas/guard-transaction.schema'

type RiskPresentation = {
  title: string
  label: string
  cardClass: string
  colorClass: string
  colorValue: string
}

const riskPresentations: Record<TransactionRiskLevel, RiskPresentation> = {
  DANGER: {
    title: '이상 거래가 발생했어요',
    label: '위험',
    cardClass: 'bg-[#fff0f0]',
    colorClass: 'text-error',
    colorValue: '#ff6161',
  },
  CAUTION: {
    title: '주의가 필요한 거래에요',
    label: '주의',
    cardClass: 'bg-[#fff7ef]',
    colorClass: 'text-warning',
    colorValue: '#ff9f3f',
  },
  SAFE: {
    title: '안심할 수 있는 거래에요',
    label: '안전',
    cardClass: 'bg-[#f1fff2]',
    colorClass: 'text-success',
    colorValue: '#2fa737',
  },
}

const route = useRoute()
const router = useRouter()
const transactionId = computed(() =>
  parsePositiveInteger(route.params.transactionId),
)
const wardId = computed(() => parsePositiveInteger(route.query.wardId))
const hasValidIds = computed(() => transactionId.value > 0 && wardId.value > 0)

const transactionQuery = useQuery(
  guardTransactionDetailOptions(wardId, transactionId),
)
const wardUserQuery = useQuery(
  queryOptions({
    queryKey: computed(() => ['users', wardId.value] as const),
    queryFn: () => getUser(wardId.value),
    enabled: computed(() => wardId.value > 0),
    staleTime: 5 * 60_000,
  }),
)

const detail = computed(() => transactionQuery.data.value ?? null)
const riskLevel = computed<TransactionRiskLevel>(
  () => detail.value?.riskLevel ?? 'SAFE',
)
const presentation = computed(() => riskPresentations[riskLevel.value])
const riskScore = computed(() => detail.value?.riskAnalysis?.riskScore ?? 0)
const detailRows = computed(() => {
  if (!detail.value) return []

  return [
    {
      label: '거래금액',
      value: `${new Intl.NumberFormat('ko-KR').format(detail.value.amount)}원`,
    },
    {
      label: '사용처',
      value: detail.value.counterpartyName ?? '원머니',
    },
    {
      label: '출금처',
      value: formatWithdrawalAccount(
        detail.value.bankName,
        detail.value.accountNo,
      ),
    },
    { label: '이체일시', value: formatOccurredAt(detail.value.occurredAt) },
  ]
})
const analysisResults = computed(() => {
  const analysis = detail.value?.riskAnalysis
  if (!analysis) return []

  return Array.from(
    new Set(
      [
        analysis.summary,
        ...analysis.reasons.map(({ description }) => description),
      ]
        .filter((result): result is string => Boolean(result?.trim()))
        .map((result) => result.trim()),
    ),
  ).slice(0, 3)
})

watchEffect(() => {
  if (!hasValidIds.value) {
    void router.replace({ name: 'guard-history' })
  }
})

function parsePositiveInteger(value: unknown): number {
  const normalized = Array.isArray(value) ? value[0] : value
  if (typeof normalized !== 'string' || !/^\d+$/.test(normalized)) return 0

  const parsed = Number(normalized)
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : 0
}

function formatWithdrawalAccount(
  bankName: string | null,
  accountNo: string | null,
) {
  if (!bankName && !accountNo) return '원머니'

  const lastFourDigits = accountNo?.replace(/\D/g, '').slice(-4) ?? ''
  return [bankName, lastFourDigits].filter(Boolean).join(' ')
}

function formatOccurredAt(value: string) {
  const occurredAt = new Date(value)
  if (Number.isNaN(occurredAt.getTime())) return value

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(occurredAt)
}

function goBack() {
  if (globalThis.history.length > 1) {
    router.back()
    return
  }

  router.replace({ name: 'guard-history' })
}

function contactWard() {
  const phone = wardUserQuery.data.value?.phone
  if (!phone) return
  globalThis.location.href = `tel:${phone}`
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
      v-if="transactionQuery.isPending.value"
      class="flex min-h-[calc(100dvh-44px)] items-center justify-center px-mobile-gutter text-[16px] font-medium text-gray-500"
    >
      거래 상세를 불러오는 중이에요.
    </section>
    <section
      v-else-if="transactionQuery.isError.value || !detail"
      class="flex min-h-[calc(100dvh-44px)] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-500">
        거래 상세를 불러오지 못했어요.
      </p>
      <button
        class="mt-md text-[14px] font-semibold text-primary-500"
        type="button"
        @click="goBack"
      >
        거래 내역으로 돌아가기
      </button>
    </section>

    <template v-else>
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

        <dl class="mt-10">
          <div
            v-for="row in detailRows"
            :key="row.label"
            class="flex h-16 items-center justify-between gap-md text-[20px] leading-[1.2] tracking-[-0.4px]"
          >
            <dt class="font-medium text-gray-700">{{ row.label }}</dt>
            <dd
              class="max-w-[220px] truncate text-right font-semibold text-black"
            >
              {{ row.value }}
            </dd>
          </div>
        </dl>

        <section
          class="relative rounded-[12px] px-xl pt-lg pb-lg"
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
            {{ riskScore }}점
          </p>

          <Progress
            class="mt-md w-full"
            :indicator-color="presentation.colorValue"
            label="이상거래 의심도 게이지"
            :value="riskScore"
          />

          <template v-if="riskLevel === 'DANGER' && analysisResults.length > 0">
            <h3
              class="mt-xl text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-black"
            >
              AI 분석 결과
            </h3>
            <ul
              class="mt-sm list-disc pl-xl text-[16px] font-medium leading-[1.32] tracking-[-0.32px] text-black"
            >
              <li v-for="result in analysisResults" :key="result">
                {{ result }}
              </li>
            </ul>
          </template>
        </section>
      </section>

      <div
        class="fixed inset-x-0 bottom-[calc(44px+env(safe-area-inset-bottom))] z-30 mx-auto w-full max-w-[390px] px-mobile-gutter"
      >
        <Button
          class="w-full"
          label="연락하기"
          variant="guard-cta"
          size="guard-cta"
          @click="contactWard"
        />
      </div>
    </template>
  </main>
</template>
