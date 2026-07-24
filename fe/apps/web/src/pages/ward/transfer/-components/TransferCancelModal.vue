<script setup lang="ts">
import { TriangleAlert } from '@lucide/vue'
import { Button, Modal } from '@pay-with/ui'

defineProps<{
  open: boolean
  cancelling?: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  confirm: []
}>()
</script>

<template>
  <Modal
    :open="open"
    title="대기 중인 거래를 취소할까요?"
    description="취소하면 보호자가 더 이상 이 거래를 승인할 수 없습니다."
    size="large"
    icon-tone="error"
    :close-on-outside="!cancelling"
    :close-on-escape="!cancelling"
    @update:open="!cancelling && emit('update:open', $event)"
  >
    <template #icon>
      <TriangleAlert class="size-xxl" :stroke-width="2.25" aria-hidden="true" />
    </template>

    <template #actions="{ close }">
      <div class="flex flex-col gap-md">
        <Button
          class="w-full"
          :label="cancelling ? '거래를 취소하고 있습니다' : '거래 취소하기'"
          variant="danger"
          size="large"
          pill
          :disabled="cancelling"
          @click="emit('confirm')"
        />
        <Button
          class="w-full"
          label="거래 유지하기"
          variant="outline-primary"
          size="large"
          pill
          :disabled="cancelling"
          @click="close"
        />
      </div>
    </template>
  </Modal>
</template>
