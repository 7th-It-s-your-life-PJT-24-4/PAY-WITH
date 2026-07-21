<script setup lang="ts">
import type { Component } from 'vue'

interface UiNavigationItem {
  value: string
  label: string
  icon?: Component
}

withDefaults(
  defineProps<{
    items: UiNavigationItem[]
    active: string
    variant?: 'guardian' | 'ward'
    centerActionValue?: string
    ariaLabel?: string
  }>(),
  {
    variant: 'guardian',
    centerActionValue: undefined,
    ariaLabel: '주요 메뉴',
  },
)

const emit = defineEmits<{
  navigate: [value: string]
}>()
</script>

<template>
  <nav
    class="grid h-bottom-nav w-full border-t border-border bg-surface-card shadow-[0_-2px_5px_rgb(0_0_0/4%)]"
    :style="{ gridTemplateColumns: `repeat(${items.length}, minmax(0, 1fr))` }"
    :aria-label="ariaLabel"
  >
    <button
      v-for="item in items"
      :key="item.value"
      class="relative flex min-h-touch-target flex-col items-center justify-center gap-xxs text-body-muted outline-none transition-colors focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-focus"
      :class="[
        variant === 'ward' && centerActionValue === item.value
          ? 'type-h1'
          : 'type-h4',
        active === item.value ? 'text-primary-300' : '',
        variant === 'ward' && centerActionValue === item.value
          ? '-mt-section m-auto size-[120px] rounded-full bg-action text-on-action shadow-modal'
          : '',
      ]"
      type="button"
      :aria-current="active === item.value ? 'page' : undefined"
      @click="emit('navigate', item.value)"
    >
      <slot name="icon" :item="item" :active="active === item.value">
        <component
          :is="item.icon"
          v-if="item.icon"
          class="size-6"
          aria-hidden="true"
        />
      </slot>
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>
