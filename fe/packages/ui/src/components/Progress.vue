<script setup lang="ts">
import { ProgressIndicator, ProgressRoot } from 'reka-ui'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    value: number
    max?: number
    label: string
    indicatorColor?: string
  }>(),
  {
    max: 100,
    indicatorColor: 'var(--color-primary-500)',
  },
)

const normalizedMax = computed(() => Math.max(1, props.max))
const normalizedValue = computed(() =>
  Math.min(Math.max(0, props.value), normalizedMax.value),
)
const indicatorWidth = computed(
  () => `${(normalizedValue.value / normalizedMax.value) * 100}%`,
)
</script>

<template>
  <ProgressRoot
    class="h-6 overflow-hidden rounded-full bg-[#c9ced4]"
    :model-value="normalizedValue"
    :max="normalizedMax"
    :get-value-label="() => label"
    :get-value-text="(value, max) => `${value ?? 0} / ${max}`"
  >
    <ProgressIndicator
      class="h-full rounded-full transition-[width] duration-300"
      :style="{
        width: indicatorWidth,
        backgroundColor: indicatorColor,
      }"
    />
  </ProgressRoot>
</template>
