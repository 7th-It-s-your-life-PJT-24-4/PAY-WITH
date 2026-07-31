<script setup lang="ts">
import { PhCrown, PhDress } from '@phosphor-icons/vue'
import type { Component } from 'vue'

import type { GuardTransaction } from '@/mocks/guard-home.mock'

defineProps<{
  transactions: GuardTransaction[]
}>()

const emit = defineEmits<{
  more: []
}>()

const categoryIcons: Record<GuardTransaction['category'], Component> = {
  transfer: PhCrown,
  payment: PhDress,
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

    <div class="mt-lg">
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

        <article class="flex h-[60px] items-center bg-white px-sm">
          <span
            class="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-primary-500 text-white"
          >
            <component
              :is="categoryIcons[transaction.category]"
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
            class="rounded-small bg-[#d5ffd8] px-[6px] py-xxs text-[10px] font-bold leading-[1.2] tracking-[-0.2px] text-success"
          >
            안전
          </span>
        </article>
      </template>
    </div>
  </section>
</template>
