<script setup lang="ts">
import type { NotificationListItemData } from '@/components/notifications/NotificationList.vue'

defineProps<{
  notification: NotificationListItemData
}>()

const emit = defineEmits<{
  select: [notification: NotificationListItemData]
}>()
</script>

<template>
  <button
    class="w-full rounded-large border border-border bg-surface-card p-lg text-left"
    :class="notification.isRead === false ? 'border-primary-500/40' : ''"
    type="button"
    @click="emit('select', notification)"
  >
    <div class="flex items-start justify-between gap-md">
      <h3 class="font-semibold text-body">{{ notification.title }}</h3>
      <span
        v-if="notification.isRead === false"
        class="mt-1 size-2 shrink-0 rounded-full bg-primary-500"
        aria-label="읽지 않은 알림"
      />
    </div>
    <p class="mt-xs text-sm leading-5 text-body-secondary">
      {{ notification.body }}
    </p>
    <time
      v-if="notification.receivedAt"
      class="mt-sm block text-xs text-body-muted"
    >
      {{ notification.receivedAt }}
    </time>
  </button>
</template>
