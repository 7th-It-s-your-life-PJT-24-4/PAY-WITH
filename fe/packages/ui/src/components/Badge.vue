<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    label: string
    status?: 'success' | 'warning' | 'error' | 'safe'
  }>(),
  {
    status: 'success',
  },
)

const statusClass = computed(
  () =>
    ({
      success: 'bg-success/10 text-success',
      warning: 'bg-warning/10 text-warning',
      error: 'bg-error/10 text-error',
      safe: 'bg-success/10 text-success',
    })[props.status],
)
</script>

<template>
  <span
    class="type-caption inline-flex min-h-[18px] items-center gap-xxs rounded-small px-xs font-medium"
    :class="[
      statusClass,
      status === 'safe' ? 'rounded-full py-xxs uppercase' : '',
    ]"
  >
    <slot name="icon">
      <svg
        v-if="status === 'safe'"
        aria-hidden="true"
        class="size-3"
        viewBox="0 0 12 12"
        fill="none"
      >
        <path
          d="m2.5 6.2 2.1 2.1 4.9-5"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </slot>
    {{ label }}
  </span>
</template>
