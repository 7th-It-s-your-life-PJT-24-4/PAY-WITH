<script setup lang="ts">
import { HeartHandshake } from '@lucide/vue'

const emit = defineEmits<{
  connect: []
}>()
</script>

<template>
  <section class="flex flex-col gap-xl" aria-labelledby="ward-unpaired-title">
    <div
      class="relative min-h-[126px] overflow-hidden rounded-large border border-border bg-gradient-to-br from-surface-card to-primary-900 p-xl"
    >
      <div
        class="absolute -right-12 -top-16 size-[192px] rounded-full bg-primary-500/5"
        aria-hidden="true"
      />
      <h1
        id="ward-unpaired-title"
        class="type-h1 relative max-w-[260px] text-body"
      >
        연결된 보호자가<br />없습니다.
      </h1>
    </div>

    <button
      class="flex aspect-square w-full flex-col items-center justify-center gap-xl rounded-large bg-action text-on-action shadow-card outline-none transition-colors hover:bg-action-active focus-visible:ring-2 focus-visible:ring-focus focus-visible:ring-offset-2"
      type="button"
      aria-label="보호자와 연결하기"
      @click="emit('connect')"
    >
      <HeartHandshake
        class="size-[150px]"
        :stroke-width="1.4"
        aria-hidden="true"
      />
      <span class="type-h1">보호자와 연결하기</span>
    </button>
  </section>
</template>
