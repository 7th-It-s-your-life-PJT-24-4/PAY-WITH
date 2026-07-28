<script setup lang="ts">
import type { Component } from 'vue'

interface NavigationItem {
  value: string
  label: string
  icon?: Component
}

const props = withDefaults(
  defineProps<{
    items: NavigationItem[]
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

function isCenterAction(value: string) {
  return props.variant === 'ward' && props.centerActionValue === value
}

function getItemClasses(value: string) {
  if (isCenterAction(value)) {
    return [
      'type-h1 -mt-[45px] m-auto size-[120px] rounded-full',
      'bg-gradient-to-b from-primary-700 to-primary-500 text-on-action',
      'shadow-[0_8px_24px_rgb(8_13_18/18%)]',
      'hover:from-primary-600 hover:to-primary-400',
      'active:from-primary-500 active:to-primary-400',
    ]
  }

  return [
    props.variant === 'ward' ? 'type-h1' : 'type-h4',
    props.active === value ? 'text-primary-300' : 'text-body',
  ]
}
</script>

<template>
  <nav
    class="grid h-bottom-nav w-full bg-surface-card"
    :class="
      variant === 'ward'
        ? 'shadow-[0_-4px_16px_rgb(8_13_18/10%)]'
        : 'border-t border-border shadow-[0_-2px_5px_rgb(0_0_0/4%)]'
    "
    :style="{ gridTemplateColumns: `repeat(${items.length}, minmax(0, 1fr))` }"
    :aria-label="ariaLabel"
  >
    <button
      v-for="item in items"
      :key="item.value"
      class="relative flex min-h-touch-target flex-col items-center justify-center gap-xxs outline-none transition-colors focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-focus"
      :class="getItemClasses(item.value)"
      type="button"
      :data-center-action="isCenterAction(item.value) || undefined"
      :aria-current="active === item.value ? 'page' : undefined"
      @click="emit('navigate', item.value)"
    >
      <slot name="icon" :item="item" :active="active === item.value">
        <component
          :is="item.icon"
          v-if="item.icon"
          :class="isCenterAction(item.value) ? 'size-12' : 'size-6'"
          aria-hidden="true"
        />
      </slot>
      <span>{{ item.label }}</span>
    </button>
  </nav>
</template>
