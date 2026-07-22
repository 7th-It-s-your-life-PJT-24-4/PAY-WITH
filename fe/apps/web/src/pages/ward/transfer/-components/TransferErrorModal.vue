<script setup lang="ts">
import { CircleAlert } from '@lucide/vue'
import { Button, Modal } from '@pay-with/ui'

withDefaults(
  defineProps<{
    open: boolean
    title?: string
    description: string
    retryLabel?: string
  }>(),
  {
    title: '다시 확인해 주세요',
    retryLabel: '다시 시도',
  },
)

const emit = defineEmits<{
  retry: []
  cancel: []
}>()
</script>

<template>
  <Modal
    :open="open"
    :title="title"
    :description="description"
    size="large"
    icon-tone="error"
    :close-on-outside="false"
    @update:open="!$event && emit('cancel')"
  >
    <template #icon>
      <CircleAlert class="size-xxl" :stroke-width="2.5" aria-hidden="true" />
    </template>
    <template #actions>
      <div class="flex flex-col gap-md">
        <Button
          class="w-full"
          :label="retryLabel"
          size="large"
          pill
          @click="emit('retry')"
        />
        <Button
          class="w-full"
          label="취소"
          size="large"
          variant="outline-primary"
          pill
          @click="emit('cancel')"
        />
      </div>
    </template>
  </Modal>
</template>
