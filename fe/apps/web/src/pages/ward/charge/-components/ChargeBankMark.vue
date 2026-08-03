<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  bankCode: string
}>()

const bankLabel = computed(
  () =>
    ({
      '004': 'KB',
      '011': 'NH',
      '020': '우리',
      '081': '하나',
      '088': '신한',
    })[props.bankCode] ?? props.bankCode,
)
</script>

<template>
  <span
    class="type-caption flex size-12 shrink-0 items-center justify-center rounded-full bg-primary-900 font-bold text-primary-300"
    aria-hidden="true"
  >
    {{ bankLabel }}
  </span>
</template>
