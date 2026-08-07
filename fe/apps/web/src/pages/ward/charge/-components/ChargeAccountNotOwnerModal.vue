<script setup lang="ts">
import { CircleAlert } from '@lucide/vue'
import { Button, Modal } from '@pay-with/ui'

withDefaults(
  defineProps<{
    open: boolean
    title?: string
    description?: string
    confirmLabel?: string
  }>(),
  {
    title: '본인 명의의 계좌가 아니에요',
    description:
      '충전에 사용하는 계좌는 시니어(본인) 명의의 계좌만 등록할 수 있습니다. 계좌 정보를 다시 확인해 주세요.',
    confirmLabel: '다시 확인하기',
  },
)

const emit = defineEmits<{
  confirm: []
  'update:open': [open: boolean]
}>()

function handleConfirm() {
  emit('confirm')
  emit('update:open', false)
}
</script>

<template>
  <Modal
    :open="open"
    :title="title"
    :description="description"
    size="large"
    icon-tone="error"
    :close-on-outside="false"
    @update:open="emit('update:open', $event)"
  >
    <template #icon>
      <CircleAlert class="size-xxl" :stroke-width="2.5" aria-hidden="true" />
    </template>
    <template #actions>
      <div class="flex flex-col gap-md">
        <Button
          class="w-full"
          :label="confirmLabel"
          size="large"
          pill
          @click="handleConfirm"
        />
      </div>
    </template>
  </Modal>
</template>
