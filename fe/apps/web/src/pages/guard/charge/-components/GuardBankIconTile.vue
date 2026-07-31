<script setup lang="ts">
withDefaults(
  defineProps<{
    iconUrl?: string
    label: string
    brandClass?: string
    iconClass?: string
  }>(),
  {
    iconUrl: undefined,
    brandClass: 'bg-primary-500',
    iconClass: 'max-h-[22px] max-w-[22px]',
  },
)
</script>

<template>
  <span
    :class="[
      'flex size-8 shrink-0 items-center justify-center rounded-[10px]',
      brandClass,
    ]"
  >
    <img
      v-if="iconUrl"
      :class="iconClass"
      :src="iconUrl"
      :alt="`${label} 아이콘`"
    />
    <slot v-else />
  </span>
</template>
