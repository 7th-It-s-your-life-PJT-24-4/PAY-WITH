<script setup lang="ts">
import { Button, Input, Modal } from '@pay-with/ui'
import { ref, watch } from 'vue'

import type { TransferRecipient } from '@/stores/transfer.store'

const props = defineProps<{
  open: boolean
  recipient: TransferRecipient | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  confirm: [alias: string]
}>()

const alias = ref('')

watch(
  () => [props.open, props.recipient?.id] as const,
  ([open]) => {
    if (open) alias.value = ''
  },
)

function confirm() {
  emit('confirm', alias.value.trim())
}
</script>

<template>
  <Modal
    :open="open"
    title="연락처 추가"
    description="아래 계좌를 연락처에 추가할까요?"
    size="large"
    @update:open="emit('update:open', $event)"
  >
    <template #icon>
      <svg
        class="size-xxl"
        viewBox="0 0 48 48"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M14 8h20a4 4 0 0 1 4 4v24a4 4 0 0 1-4 4H14a4 4 0 0 1-4-4V12a4 4 0 0 1 4-4Z"
          stroke="currentColor"
          stroke-width="3"
        />
        <path
          d="M10 16H7m3 8H7m3 8H7m14-12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-6 10c0-4 2.7-7 6-7s6 3 6 7m7-4h8m-4-4v8"
          stroke="currentColor"
          stroke-width="3"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </template>

    <div v-if="recipient" class="border-t border-border pt-xl">
      <strong class="type-h2 block text-body">{{ recipient.name }}</strong>
      <p class="type-h3 mt-xs text-body-muted">
        {{ recipient.bank }} {{ recipient.accountNumber }}
      </p>

      <Input
        v-model="alias"
        class="mt-xl text-left"
        label="연락처 별칭"
        placeholder="연락처 별칭 입력(선택)"
        large
      />
    </div>

    <template #actions="{ close }">
      <div class="flex flex-col gap-md">
        <Button
          class="w-full"
          label="추가하기"
          size="large"
          pill
          @click="confirm"
        />
        <Button
          class="w-full"
          label="취소"
          variant="outline-primary"
          size="large"
          pill
          @click="close"
        />
      </div>
    </template>
  </Modal>
</template>
