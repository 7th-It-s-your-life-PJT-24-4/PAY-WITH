<script setup lang="ts">
import NotificationListItem from '@/components/notifications/NotificationListItem.vue'

export type NotificationListItemData = {
  id: string
  title: string
  body: string
  receivedAt?: string
  isRead?: boolean
}

withDefaults(
  defineProps<{
    notifications?: NotificationListItemData[]
  }>(),
  { notifications: () => [] },
)

const emit = defineEmits<{
  select: [notification: NotificationListItemData]
}>()
</script>

<template>
  <section aria-label="알림 목록">
    <p
      v-if="notifications.length === 0"
      class="rounded-large border border-border bg-surface-card p-xl text-center text-body-muted"
    >
      새로운 알림이 없어요.
    </p>
    <ul v-else class="grid gap-sm">
      <li v-for="notification in notifications" :key="notification.id">
        <NotificationListItem
          :notification="notification"
          @select="emit('select', $event)"
        />
      </li>
    </ul>
  </section>
</template>
