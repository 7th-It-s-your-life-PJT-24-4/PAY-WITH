<script setup lang="ts">
import { LoaderCircle } from '@lucide/vue'
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
      <LoaderCircle
        class="size-xxl animate-spin"
        :stroke-width="2.5"
        aria-hidden="true"
      />
    </template>
    <p class="sr-only" role="status" aria-live="polite">{{ title }}</p>
  </Modal>
</template>
