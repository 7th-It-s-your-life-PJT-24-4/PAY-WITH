<script setup lang="ts">
import { Modal } from '@pay-with/ui'

defineProps<{
  open: boolean
  title: string
  description: string
}>()
</script>

<template>
  <Modal
    :open="open"
    :title="title"
    :description="description"
    size="large"
    :close-on-outside="false"
    :close-on-escape="false"
  >
    <template #icon>
      <span
        class="size-10 animate-spin rounded-full border-4 border-primary-900 border-t-primary-500"
        aria-hidden="true"
      />
    </template>
    <p class="sr-only" role="status" aria-live="polite">{{ title }}</p>
  </Modal>
</template>
