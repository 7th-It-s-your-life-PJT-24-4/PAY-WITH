<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    title: string
    description?: string
    tone?: 'warning' | 'error'
  }>(),
  {
    description: undefined,
    tone: 'warning',
  },
)

const toneClass = computed(() =>
  props.tone === 'error'
    ? 'bg-error/15 text-error ring-error/10'
    : 'bg-warning/10 text-warning ring-warning/10',
)
</script>

<template>
  <section class="flex flex-col items-center text-center">
    <div
      class="flex size-20 items-center justify-center rounded-full ring-8"
      :class="toneClass"
      aria-hidden="true"
    >
      <slot name="icon" />
    </div>
    <h2 class="type-h1 mt-xl whitespace-pre-line">{{ title }}</h2>
    <p
      v-if="description"
      class="type-body-medium mt-md whitespace-pre-line text-body-secondary"
    >
      {{ description }}
    </p>
  </section>
</template>
