<script setup lang="ts">
import backIconUrl from '@/assets/icons/transaction-detail-back.svg'
import headerSpacerIconUrl from '@/assets/icons/transaction-detail-header-spacer.svg'

withDefaults(
  defineProps<{
    title?: string
    backLabel?: string
  }>(),
  { title: '거래 상세', backLabel: '뒤로 가기' },
)

defineEmits<{
  back: []
}>()
</script>

<template>
  <header class="grid h-11 grid-cols-[44px_1fr_44px] items-center">
    <button
      class="flex size-11 items-center justify-center"
      type="button"
      :aria-label="backLabel"
      @click="$emit('back')"
    >
      <span class="flex size-6 items-center justify-center">
        <img
          class="h-[15.84px] w-[7.097px]"
          :src="backIconUrl"
          alt=""
          aria-hidden="true"
        />
      </span>
    </button>
    <h1
      v-if="title"
      class="text-center text-[20px] font-semibold leading-[1.2] tracking-[-0.4px] text-black"
    >
      {{ title }}
    </h1>
    <span v-else aria-hidden="true" />
    <img class="size-11" :src="headerSpacerIconUrl" alt="" aria-hidden="true" />
  </header>
</template>
