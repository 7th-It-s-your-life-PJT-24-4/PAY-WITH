<script setup lang="ts">
import { BottomSheet } from '@pay-with/ui'

import GuardBankIconTile from '@/pages/guard/charge/-components/GuardBankIconTile.vue'
import type { GuardChargeBank } from '@/mocks/guard-charge.mock'

defineProps<{
  banks: GuardChargeBank[]
}>()

const open = defineModel<boolean>('open', { default: false })

const emit = defineEmits<{
  select: [bank: GuardChargeBank]
}>()
</script>

<template>
  <BottomSheet
    v-model:open="open"
    title="은행"
    description="충전 계좌를 연결할 은행 또는 증권사를 선택합니다."
    content-class="min-h-[646px] px-10 text-center"
  >
    <div class="mt-6 grid grid-cols-4 gap-x-[33px] gap-y-[30px]">
      <button
        v-for="bank in banks"
        :key="bank.id"
        class="flex h-[56px] flex-col items-center justify-start"
        type="button"
        @click="emit('select', bank)"
      >
        <GuardBankIconTile
          :icon-url="bank.iconUrl"
          :label="bank.name"
          :brand-class="bank.brandClass"
          icon-class="max-h-5 max-w-5"
        />
        <span
          class="mt-xs max-w-[54px] truncate text-center text-[12px] font-bold leading-[1.2] tracking-[-0.24px] text-black"
        >
          {{ bank.name }}
        </span>
      </button>
    </div>
  </BottomSheet>
</template>
