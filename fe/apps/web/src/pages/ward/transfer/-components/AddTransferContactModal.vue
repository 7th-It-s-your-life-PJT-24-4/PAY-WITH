<script setup lang="ts">
import { UserRoundPlus } from '@lucide/vue'
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
      <UserRoundPlus class="size-xxl" :stroke-width="2.25" aria-hidden="true" />
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
