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
    title="결제를 그만둘까요?"
    description="취소하면 현재 QR 코드는 더 이상 사용할 수 없습니다."
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
          :label="cancelling ? '결제를 취소하고 있습니다' : '결제 취소하기'"
          variant="danger"
          size="large"
          pill
          :disabled="cancelling"
          @click="emit('confirm')"
        />
        <Button
          class="w-full"
          label="결제 계속하기"
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
