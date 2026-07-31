<script setup lang="ts">
import { ChevronDown, Landmark } from '@lucide/vue'

import GuardBankIconTile from '@/pages/guard/charge/-components/GuardBankIconTile.vue'

defineProps<{
  bankName: string
  accountSuffix: string
  balance: string
  iconUrl?: string
  brandClass?: string
}>()

const emit = defineEmits<{
  click: []
}>()
</script>

<template>
  <button
    class="flex h-20 w-full items-center rounded-[20px] bg-[#F0F3F8] px-md text-left"
    type="button"
    aria-label="출금 계좌 선택"
    @click="emit('click')"
  >
    <GuardBankIconTile
      :icon-url="iconUrl"
      :label="bankName"
      :brand-class="brandClass"
    >
      <Landmark class="size-5 text-white" aria-hidden="true" />
    </GuardBankIconTile>

    <span class="ml-sm min-w-0 flex-1">
      <span
        class="block truncate text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-black"
      >
        {{ bankName }} {{ accountSuffix }} 에서
      </span>
      <span
        class="mt-xs block text-[14px] font-medium leading-[1.2] tracking-[-0.28px] text-gray-700"
      >
        {{ balance }}
      </span>
    </span>

    <ChevronDown class="size-6 shrink-0 text-gray-600" aria-hidden="true" />
  </button>
</template>
