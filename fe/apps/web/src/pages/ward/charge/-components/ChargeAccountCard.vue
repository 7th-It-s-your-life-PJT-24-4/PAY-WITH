<script setup lang="ts">
import { Check, ChevronDown, Plus } from '@lucide/vue'
import { computed, ref } from 'vue'

import ChargeBankMark from '@/pages/ward/charge/-components/ChargeBankMark.vue'
import type { ChargeAccount } from '@/types/charge'

const props = defineProps<{
  accounts: ChargeAccount[]
  selectedAccountId: number
}>()

const emit = defineEmits<{
  select: [accountId: number]
  add: []
}>()

const open = ref(false)
const selectedAccount = computed(
  () =>
    props.accounts.find(
      ({ accountId }) => accountId === props.selectedAccountId,
    ) ?? props.accounts[0],
)

function select(accountId: number) {
  emit('select', accountId)
  open.value = false
}
</script>

<template>
  <section class="relative" aria-label="출금 계좌">
    <button
      class="flex min-h-[88px] w-full items-center gap-md rounded-large border border-border bg-surface-card p-lg text-left shadow-card outline-none focus-visible:ring-2 focus-visible:ring-focus"
      type="button"
      :aria-expanded="open"
      aria-haspopup="listbox"
      @click="open = !open"
    >
      <ChargeBankMark
        v-if="selectedAccount"
        :bank-code="selectedAccount.bankCode"
      />
      <span v-if="selectedAccount" class="min-w-0 flex-1">
        <span class="type-h3 block">{{ selectedAccount.bankName }}</span>
        <span
          class="type-body-medium font-number block truncate text-body-muted"
        >
          {{ selectedAccount.accountNumber }}
        </span>
      </span>
      <ChevronDown
        class="size-xl shrink-0 transition-transform"
        :class="open ? 'rotate-180' : ''"
        aria-hidden="true"
      />
    </button>

    <div
      v-if="open"
      class="absolute inset-x-0 top-[calc(100%+var(--spacing-xs))] z-20 overflow-hidden rounded-large border border-border bg-surface-card shadow-modal"
      role="listbox"
      aria-label="등록 계좌 목록"
    >
      <button
        v-for="account in accounts"
        :key="account.accountId"
        class="flex min-h-touch-target w-full items-center gap-md border-b border-border px-lg py-md text-left last:border-b-0 hover:bg-primary-900 focus-visible:bg-primary-900 focus-visible:outline-none"
        type="button"
        role="option"
        :aria-selected="account.accountId === selectedAccountId"
        @click="select(account.accountId)"
      >
        <ChargeBankMark :bank-code="account.bankCode" />
        <span class="min-w-0 flex-1">
          <span class="type-h4 block">{{ account.bankName }}</span>
          <span
            class="type-body-medium font-number block truncate text-body-muted"
          >
            {{ account.accountNumber }}
          </span>
        </span>
        <Check
          v-if="account.accountId === selectedAccountId"
          class="size-xl text-primary-500"
          aria-hidden="true"
        />
      </button>
      <button
        class="type-h4 flex min-h-touch-target w-full items-center justify-center gap-sm px-lg py-md text-primary-500 hover:bg-primary-900 focus-visible:bg-primary-900 focus-visible:outline-none"
        type="button"
        @click="emit('add')"
      >
        <Plus class="size-xl" aria-hidden="true" />
        새 계좌 추가
      </button>
    </div>
  </section>
</template>
