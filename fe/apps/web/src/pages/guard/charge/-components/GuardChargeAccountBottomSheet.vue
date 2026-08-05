<script setup lang="ts">
import { Plus } from '@lucide/vue'
import { BottomSheet, Button } from '@pay-with/ui'

import GuardBankIconTile from '@/pages/guard/charge/-components/GuardBankIconTile.vue'
import type { Account } from '@/schemas/account.schema'

defineProps<{
  accounts: Account[]
  selectedAccountId: number | null
}>()

const open = defineModel<boolean>('open', { default: false })

const emit = defineEmits<{
  select: [accountId: number]
  add: []
}>()
</script>

<template>
  <BottomSheet
    v-model:open="open"
    title="어떤 계좌를 사용할까요?"
    description="충전에 사용할 계좌를 선택합니다."
    content-class="min-h-[472px]"
  >
    <div v-if="accounts.length > 0" class="mt-7 flex flex-col">
      <button
        v-for="account in accounts"
        :key="account.accountId"
        class="flex min-h-[60px] items-center rounded-xl bg-white text-left transition-colors duration-300"
        :aria-current="
          account.accountId === selectedAccountId ? 'true' : undefined
        "
        type="button"
        @click="emit('select', account.accountId)"
      >
        <GuardBankIconTile :label="account.bankName" />
        <span class="ml-sm min-w-0">
          <span
            class="block truncate text-[16px] font-bold leading-[1.2] tracking-[-0.32px] text-black"
          >
            {{ account.bankName }} {{ account.accountNo.slice(-4) }}
          </span>
          <span
            class="mt-xxs block text-[14px] font-medium leading-[1.2] tracking-[-0.28px] text-gray-600"
          >
            연동 계좌
          </span>
        </span>
      </button>
    </div>

    <div v-else class="flex min-h-[260px] flex-col items-center justify-center">
      <p
        class="text-center text-[20px] font-bold leading-[1.35] tracking-[-0.4px] text-black"
      >
        등록된 계좌가 없어요.
      </p>
      <p
        class="mt-xs text-center text-[16px] font-medium leading-[1.4] tracking-[-0.32px] text-gray-600"
      >
        계좌를 연동해주세요.
      </p>
      <Button
        class="mt-lg w-full"
        label="계좌 연동하기"
        variant="guard-cta"
        size="guard-cta"
        @click="emit('add')"
      />
    </div>

    <button
      v-if="accounts.length > 0"
      class="absolute bottom-[calc(22px+env(safe-area-inset-bottom))] left-5 flex h-10 items-center text-left"
      type="button"
      @click="emit('add')"
    >
      <span
        class="flex size-8 items-center justify-center rounded-[10px] bg-primary-500 text-white"
      >
        <Plus class="size-5" aria-hidden="true" />
      </span>
      <span
        class="ml-xs text-[16px] font-bold leading-[1.2] tracking-[-0.32px] text-black"
      >
        계좌추가
      </span>
    </button>
  </BottomSheet>
</template>
