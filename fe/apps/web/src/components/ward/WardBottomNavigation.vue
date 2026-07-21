<script setup lang="ts">
type WardNavigationValue = 'transfer' | 'payment' | 'history'

withDefaults(
  defineProps<{
    active?: WardNavigationValue
  }>(),
  {
    active: 'payment',
  },
)

const items: ReadonlyArray<{
  label: string
  value: WardNavigationValue
}> = [
  { label: '송금', value: 'transfer' },
  { label: '결제', value: 'payment' },
  { label: '내역', value: 'history' },
]

const emit = defineEmits<{
  navigate: [value: WardNavigationValue]
}>()
</script>

<template>
  <nav
    class="sticky bottom-0 mt-auto grid h-bottom-nav grid-cols-3 border-t border-border bg-surface-card shadow-card"
    aria-label="시니어 주요 기능"
  >
    <button
      v-for="item in items"
      :key="item.value"
      class="type-h1 min-h-bottom-nav px-xs text-gray-200 focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-focus"
      :class="[
        item.value === 'payment'
          ? 'mx-auto -mt-section flex size-28 items-center justify-center rounded-full bg-action text-on-action shadow-modal'
          : '',
        active === item.value && item.value !== 'payment'
          ? 'text-primary-300'
          : '',
      ]"
      type="button"
      :aria-current="active === item.value ? 'page' : undefined"
      @click="emit('navigate', item.value)"
    >
      {{ item.label }}
    </button>
  </nav>
</template>
