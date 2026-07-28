<script setup lang="ts">
import { ChevronRight, ShieldAlert } from '@lucide/vue'

import type { TransferDetail } from '@/types/transfer'

defineProps<{
  transfers: TransferDetail[]
}>()

const emit = defineEmits<{
  select: [transactionId: number]
}>()

function formatAmount(amount: number) {
  return `${amount.toLocaleString('ko-KR')}원`
}
</script>

<template>
  <section
    v-if="transfers.length"
    class="overflow-hidden rounded-large border border-warning/30 bg-surface-card shadow-card"
    aria-labelledby="pending-transfer-title"
  >
    <header class="flex items-start gap-md p-xl">
      <span
        class="flex size-12 shrink-0 items-center justify-center rounded-full bg-warning/15 text-warning"
        aria-hidden="true"
      >
        <ShieldAlert class="size-6" />
      </span>
      <div>
        <h2 id="pending-transfer-title" class="type-h3 text-body">
          보호자 승인을 기다리고 있어요
        </h2>
        <p class="type-body-medium mt-xs text-body-secondary">
          확인이 필요한 송금 {{ transfers.length }}건이 있습니다.
        </p>
      </div>
    </header>

    <ul class="divide-y divide-border border-t border-border">
      <li v-for="transfer in transfers" :key="transfer.transactionId">
        <button
          type="button"
          class="flex min-h-28 w-full items-center gap-md px-xl py-lg text-left transition-colors hover:bg-primary-900/50 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary-500"
          :aria-label="`${transfer.recipientName} 님에게 ${formatAmount(transfer.amount)} 송금 상세 확인`"
          @click="emit('select', transfer.transactionId)"
        >
          <div class="min-w-0 flex-1">
            <div class="mt-xs flex items-baseline justify-between gap-md">
              <strong class="type-h3 truncate text-body">
                {{ transfer.recipientName }} 님에게
              </strong>
              <strong class="type-h3 shrink-0 font-number text-body">
                {{ formatAmount(transfer.amount) }}
              </strong>
            </div>
            <p class="type-body-medium mt-xs truncate text-body-secondary">
              {{ transfer.bankName }} · {{ transfer.accountNumber }}
            </p>
          </div>
          <ChevronRight
            class="size-6 shrink-0 text-body-muted"
            aria-hidden="true"
          />
        </button>
      </li>
    </ul>
  </section>
</template>
