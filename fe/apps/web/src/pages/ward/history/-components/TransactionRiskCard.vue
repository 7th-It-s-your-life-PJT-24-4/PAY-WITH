<script setup lang="ts">
import { CircleAlert, ShieldCheck } from '@lucide/vue'
import { computed } from 'vue'

import { transactionRiskLabel } from '@/pages/ward/history/-utils/transaction-format'
import type { WardTransaction } from '@/types/transaction'

const props = defineProps<{
  transaction: WardTransaction
}>()

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
      BLOCKED: {
        border: 'border-error',
        header: 'bg-error text-on-semantic',
        text: 'text-error',
        note: 'border-error bg-error/10',
        icon: CircleAlert,
      },
    })[props.transaction.riskLevel],
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
      <h2 id="transaction-risk-title" class="type-h3">거래 안전 확인</h2>
      <strong class="type-h3">{{
        transactionRiskLabel[transaction.riskLevel]
      }}</strong>
    </header>

    <div class="p-xl">
      <div class="flex items-center justify-between gap-lg">
        <div>
          <p class="type-body-medium text-body-muted">거래 안전 점수</p>
          <p class="mt-xs flex items-end gap-xs" :class="theme.text">
            <strong class="text-[40px] leading-none font-number font-bold">
              {{ transaction.riskScore }}
            </strong>
            <span class="type-body-medium text-body-muted">/ 100</span>
          </p>
        </div>
        <component :is="theme.icon" class="size-14" :class="theme.text" />
      </div>

      <p
        class="type-body-medium mt-xl rounded-medium border-l-4 p-md text-body"
        :class="theme.note"
      >
        {{ transaction.riskSummary }}
      </p>

      <div class="mt-xl">
        <h3 class="type-h4 text-body">확인할 내용</h3>
        <ul v-if="transaction.riskReasons.length" class="mt-md grid gap-md">
          <li
            v-for="reason in transaction.riskReasons"
            :key="reason"
            class="type-body-medium flex gap-sm text-body-secondary"
          >
            <CircleAlert class="mt-0.5 size-5 shrink-0" :class="theme.text" />
            <span>{{ reason }}</span>
          </li>
        </ul>
        <p v-else class="type-body-medium mt-md text-body-secondary">
          특별히 확인할 내용이 없습니다.
        </p>
      </div>
    </div>
  </section>
</template>
