<script setup lang="ts">
import { CircleAlert, ShieldCheck } from '@lucide/vue'
import { computed } from 'vue'

import type {
  TransactionRiskLevel,
  TransactionStatus,
} from '@/types/transaction'

const props = defineProps<{
  riskLevel: TransactionRiskLevel
  riskScore: number
  riskSummary: string | null
  riskReasons: string[]
  status: TransactionStatus
}>()

const riskLabel = computed(() => {
  if (props.status === 'BLOCKED') return '시스템 차단됨'

  return {
    SAFE: '안전',
    CAUTION: '주의',
    DANGER: '위험',
  }[props.riskLevel]
})

const theme = computed(
  () =>
    ({
      SAFE: {
        border: 'border-success',
        header: 'bg-success text-on-semantic',
        text: 'text-success',
        note: 'border-success bg-success/10',
        icon: ShieldCheck,
      },
      CAUTION: {
        border: 'border-warning',
        header: 'bg-warning text-body',
        text: 'text-warning',
        note: 'border-warning bg-warning/10',
        icon: CircleAlert,
      },
      DANGER: {
        border: 'border-error',
        header: 'bg-error text-on-semantic',
        text: 'text-error',
        note: 'border-error bg-error/10',
        icon: CircleAlert,
      },
    })[props.riskLevel],
)
</script>

<template>
  <section
    class="overflow-hidden rounded-large border-2 bg-surface-card shadow-card"
    :class="theme.border"
    aria-labelledby="transaction-risk-title"
  >
    <header
      class="flex min-h-16 items-center justify-between gap-md px-xl py-md"
      :class="theme.header"
    >
      <h2 id="transaction-risk-title" class="text-[20px] font-bold">
        거래 안전 확인
      </h2>
      <strong class="text-[20px] font-bold">{{ riskLabel }}</strong>
    </header>

    <div class="p-xl">
      <div class="flex items-center justify-between gap-lg">
        <div>
          <p class="text-[16px] font-medium text-body-muted">이상거래 의심도</p>
          <p class="mt-xs flex items-end gap-xs" :class="theme.text">
            <strong class="font-number text-[40px] font-bold leading-none">
              {{ riskScore }}
            </strong>
            <span class="text-[16px] font-medium text-body-muted">/ 100</span>
          </p>
        </div>
        <component :is="theme.icon" class="size-14" :class="theme.text" />
      </div>

      <p
        v-if="riskSummary"
        class="mt-xl rounded-medium border-l-4 p-lg text-[16px] font-medium leading-relaxed text-body"
        :class="theme.note"
      >
        {{ riskSummary }}
      </p>

      <div class="mt-xl">
        <h3 class="text-[18px] font-semibold text-body">확인할 내용</h3>
        <ul v-if="riskReasons.length" class="mt-md grid gap-md">
          <li
            v-for="reason in riskReasons"
            :key="reason"
            class="flex gap-sm text-[16px] font-medium leading-relaxed text-body-secondary"
          >
            <CircleAlert class="mt-1 size-5 shrink-0" :class="theme.text" />
            <span>{{ reason }}</span>
          </li>
        </ul>
        <p v-else class="mt-md text-[16px] font-medium text-body-secondary">
          특별히 확인할 내용이 없습니다.
        </p>
      </div>
    </div>
  </section>
</template>
