<script setup lang="ts">
import type { GuardTransaction } from '@/mocks/guard-home.mock'
import { useRouter } from 'vue-router'
import GuardTransactionCard from '@/pages/guard/-components/GuardTransactionCard.vue'

defineProps<{
  transactions: GuardTransaction[]
  wardId: number | null
}>()

const emit = defineEmits<{
  more: []
}>()

const router = useRouter()
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

        <GuardTransactionCard
          :amount="transaction.amount"
          :accessible-label="`${transaction.date} ${transaction.description} 거래 상세 보기`"
          :category="transaction.category"
          :description="transaction.description"
          :status="transaction.status"
          @select="
            router.push({
              name: 'guard-transaction-detail',
              params: { id: transaction.id },
              query: { wardId: wardId ?? undefined },
            })
          "
        />
      </template>
    </div>
  </section>
</template>
