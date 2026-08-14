<script setup lang="ts">
import {
  guardTransactionCategoryIcons,
  guardTransactionStatusClasses,
  guardTransactionStatusLabels,
} from '@/pages/guard/-utils/guard-transaction-ui'
import type { GuardTransaction } from '@/mocks/guard-home.mock'

const props = defineProps<{
  amount: string
  accessibleLabel: string
  category: GuardTransaction['category']
  description: string
  status?: GuardTransaction['status']
}>()

const emit = defineEmits<{
  select: []
}>()
</script>

<template>
  <button
    class="flex min-h-[74px] w-full items-center bg-white px-sm text-left"
    type="button"
    :aria-label="props.accessibleLabel"
    @click="emit('select')"
  >
    <span
      class="flex size-11 shrink-0 items-center justify-center rounded-[14px] text-white"
      :class="category === 'charge' ? 'bg-[#7ADCE3]' : 'bg-primary-500'"
    >
      <component
        :is="guardTransactionCategoryIcons[category]"
        class="size-5"
        weight="fill"
        aria-hidden="true"
      />
    </span>

    <span class="ml-[13px] min-w-0 flex-1">
      <span
        class="block truncate text-[14px] font-medium leading-[1.2] tracking-[-0.28px] text-gray-700"
      >
        {{ description }}
      </span>
      <span
        data-testid="transaction-amount"
        class="mt-xxs block text-[17px] font-bold leading-[1.2] tracking-[-0.34px]"
        :class="category === 'charge' ? 'text-[#087E96]' : 'text-[#3D4348]'"
      >
        {{ amount }}
      </span>
    </span>

    <span
      v-if="status"
      class="ml-sm shrink-0 rounded-[8px] px-[11px] py-1 text-[12px] font-semibold leading-[1.2] tracking-[-0.24px]"
      :class="guardTransactionStatusClasses[status]"
    >
      {{ guardTransactionStatusLabels[status] }}
    </span>
  </button>
</template>
