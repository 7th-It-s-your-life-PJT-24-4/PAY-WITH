<script setup lang="ts">
import { BottomSheet } from '@pay-with/ui'

import GuardBankIconTile from '@/pages/guard/charge/-components/GuardBankIconTile.vue'
import type { Bank } from '@/schemas/bank.schema'
import { getBankPresentation } from '@/utils/bank-presentation'

defineProps<{
  banks: Bank[]
}>()

const open = defineModel<boolean>('open', { default: false })

const emit = defineEmits<{
  select: [bank: Bank]
}>()
</script>

<template>
  <BottomSheet
    v-model:open="open"
    title="은행"
    description="은행 또는 증권사를 선택합니다."
    content-class="min-h-[646px] px-10 text-center"
  >
    <div class="mt-6 grid grid-cols-4 gap-x-[33px] gap-y-[30px]">
      <button
        v-for="bank in banks"
        :key="bank.bankCode"
        class="flex h-[56px] flex-col items-center justify-start"
        type="button"
        @click="emit('select', bank)"
      >
        <GuardBankIconTile
          :icon-url="getBankPresentation(bank).iconUrl"
          :label="bank.bankName"
          :brand-class="getBankPresentation(bank).brandClass"
          icon-class="max-h-5 max-w-5"
        >
          <span class="text-[13px] font-bold text-white">
            {{ bank.bankName.slice(0, 1) }}
          </span>
        </GuardBankIconTile>
        <span
          class="mt-xs max-w-[54px] truncate text-center text-[12px] font-bold leading-[1.2] tracking-[-0.24px] text-black"
        >
          {{ bank.bankName }}
        </span>
      </button>
    </div>
  </BottomSheet>
</template>
