<script setup lang="ts">
import { ChevronRight, Plus, ShieldCheck } from '@lucide/vue'
import { computed } from 'vue'

import type { TransferRecipient } from '@/stores/transfer.store'
import { getBankPresentation } from '@/utils/bank-presentation'

const props = withDefaults(
  defineProps<{
    recipient: TransferRecipient
    showAddContact?: boolean
  }>(),
  {
    showAddContact: false,
  },
)

const emit = defineEmits<{
  select: [recipient: TransferRecipient]
  addContact: [recipient: TransferRecipient]
}>()

const bankPresentation = computed(() =>
  getBankPresentation({
    bankCode: props.recipient.bankCode,
    bankName: props.recipient.bank,
  }),
)
</script>

<template>
  <div
    class="flex w-full items-center justify-between border-b border-border transition-colors hover:bg-surface-card/60 last:border-b-0"
  >
    <!-- 메인 송금 선택 버튼 (좌측 로고 + 텍스트 정보) -->
    <button
      class="flex min-w-0 flex-1 items-center gap-md px-md py-md text-left outline-none focus-visible:bg-surface-card/80"
      type="button"
      :aria-label="`${recipient.name} ${recipient.bank} ${recipient.accountNumber} 송금 선택`"
      @click="emit('select', recipient)"
    >
      <!-- 은행 브랜드 원형 로고 타일 -->
      <span
        :class="[
          'flex size-11 shrink-0 items-center justify-center overflow-hidden rounded-full',
          bankPresentation.brandClass,
        ]"
        aria-hidden="true"
      >
        <img
          v-if="bankPresentation.iconUrl"
          class="size-7 object-contain"
          :src="bankPresentation.iconUrl"
          alt=""
        />
        <span v-else class="type-caption font-bold text-white">
          {{ recipient.bank.slice(0, 1) }}
        </span>
      </span>

      <!-- 수취인 정보 (이름 + 안심 뱃지 + 계좌) -->
      <span class="min-w-0 flex-1">
        <span class="flex items-center gap-xs">
          <strong class="type-h2 truncate text-body">{{
            recipient.name
          }}</strong>
          <span
            v-if="recipient.isContact"
            class="inline-flex shrink-0 items-center gap-xxs rounded-full bg-success/15 px-xs py-[2px] text-xs font-bold text-success"
            aria-label="안심계좌"
          >
            <ShieldCheck class="size-3.5" aria-hidden="true" />
            안심
          </span>
        </span>
        <span class="type-body-medium mt-xxs block truncate text-body-muted">
          {{ recipient.bank }} {{ recipient.accountNumber }}
        </span>
      </span>
    </button>

    <!-- 우측 액션: 안심계좌 추가 버튼 또는 송금 화살표 -->
    <div class="shrink-0 pr-md">
      <button
        v-if="showAddContact"
        class="flex min-h-touch-target items-center justify-center gap-xxs rounded-full border-2 border-primary-500 bg-surface-card px-md py-xs text-[14px] font-bold text-primary-500 transition-colors hover:border-primary-500 hover:bg-primary-500 hover:text-on-action active:border-action-active active:bg-action-active active:text-on-action focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus"
        type="button"
        :aria-label="`${recipient.name} 안심계좌 추가`"
        @click="emit('addContact', recipient)"
      >
        <Plus class="size-4 shrink-0" :stroke-width="2.5" aria-hidden="true" />
        <span>안심계좌</span>
      </button>
      <button
        v-else
        class="flex min-h-touch-target items-center justify-center text-body-muted outline-none"
        type="button"
        tabindex="-1"
        aria-hidden="true"
        @click="emit('select', recipient)"
      >
        <ChevronRight class="size-xl" :stroke-width="2.5" />
      </button>
    </div>
  </div>
</template>
