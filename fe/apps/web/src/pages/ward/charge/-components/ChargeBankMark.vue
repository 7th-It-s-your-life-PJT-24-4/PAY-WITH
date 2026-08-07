<script setup lang="ts">
import { computed } from 'vue'

import { getBankPresentation } from '@/utils/bank-presentation'

const props = defineProps<{
  bankCode: string
  bankName?: string
}>()

const presentation = computed(() =>
  getBankPresentation({
    bankCode: props.bankCode,
    bankName: props.bankName ?? props.bankCode,
  }),
)
</script>

<template>
  <span
    :class="[
      'flex size-12 shrink-0 items-center justify-center overflow-hidden rounded-full',
      presentation.brandClass,
    ]"
    aria-hidden="true"
  >
    <img
      v-if="presentation.iconUrl"
      class="size-8 object-contain"
      :src="presentation.iconUrl"
      alt=""
    />
    <span v-else class="type-caption font-bold text-white">
      {{ (bankName ?? bankCode).slice(0, 1) }}
    </span>
  </span>
</template>
