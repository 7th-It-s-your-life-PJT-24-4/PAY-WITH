<script setup lang="ts">
defineProps<{
  bankCode: string
}>()
</script>

<template>
  <span
    class="type-caption flex size-12 shrink-0 items-center justify-center rounded-full bg-primary-900 font-bold text-primary-300"
    aria-hidden="true"
  >
    {{ bankCode.slice(0, 2) }}
  </span>
</template>
