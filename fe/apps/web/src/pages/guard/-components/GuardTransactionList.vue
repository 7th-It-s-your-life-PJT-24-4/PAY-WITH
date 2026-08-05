<script setup lang="ts">
import type { GuardTransaction } from '@/mocks/guard-home.mock'
import { useRouter } from 'vue-router'
import {
  guardTransactionCategoryIcons,
  guardTransactionStatusClasses,
  guardTransactionStatusLabels,
} from '@/pages/guard/-utils/guard-transaction-ui'

const props = defineProps<{
  transactions: GuardTransaction[]
  wardId?: number | null
}>()

const emit = defineEmits<{
  more: []
}>()

const router = useRouter()

function openDetail(transactionId: string) {
  router.push({
    name: 'guard-transaction-detail',
    params: { transactionId },
    query: props.wardId ? { wardId: props.wardId } : undefined,
  })
}
</script>

<template>
  <section aria-labelledby="guard-transactions-title">
    <div class="flex items-center justify-between">
      <h2
        id="guard-transactions-title"
        class="text-[18px] font-bold leading-[1.2] tracking-[-0.36px]"
      >
        최근 거래 내역
      </h2>
      <button
        class="flex size-8 items-center justify-end text-gray-700"
        type="button"
        aria-label="거래 내역 더보기"
        @click="emit('more')"
      >
        <span class="text-[32px] leading-none font-light">›</span>
      </button>
    </div>

    <p
      v-if="transactions.length === 0"
      class="py-8 text-center text-md font-medium leading-[1.2] tracking-[-0.28px] text-gray-700"
    >
      최근 거래 내역이 없어요.
    </p>

    <div v-else class="mt-lg">
      <template
        v-for="(transaction, index) in transactions"
        :key="transaction.id"
      >
        <p
          v-if="
            index === 0 || transactions[index - 1]?.date !== transaction.date
          "
          class="type-body-medium mb-xs text-gray-500"
          :class="index > 0 ? 'mt-md' : ''"
        >
          {{ transaction.date }}
        </p>

        <button
          class="flex h-[60px] w-full items-center bg-white px-sm text-left"
          type="button"
          :aria-label="`${transaction.date} 거래 상세 보기`"
          @click="openDetail(transaction.id)"
        >
          <span
            class="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-primary-500 text-white"
          >
            <component
              :is="guardTransactionCategoryIcons[transaction.category]"
              class="size-[18px]"
              weight="fill"
              aria-hidden="true"
            />
          </span>
          <div class="ml-md min-w-0 flex-1">
            <p
              class="text-[14px] font-semibold leading-[1.2] tracking-[-0.28px] text-black"
            >
              {{ transaction.amount }}
            </p>
            <p
              class="mt-xxs truncate text-[12px] font-medium leading-[1.2] tracking-[-0.24px] text-gray-700"
            >
              {{ transaction.description }}
            </p>
          </div>
          <span
            class="rounded-small px-[6px] py-xxs text-[10px] font-bold leading-[1.2] tracking-[-0.2px]"
            :class="guardTransactionStatusClasses[transaction.status]"
          >
            {{ guardTransactionStatusLabels[transaction.status] }}
          </span>
        </button>
      </template>
    </div>
  </section>
</template>
