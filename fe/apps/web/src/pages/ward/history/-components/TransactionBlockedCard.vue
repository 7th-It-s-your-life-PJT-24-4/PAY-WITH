<script setup lang="ts">
import { ShieldX } from '@lucide/vue'

defineProps<{
  title?: string
  ariaLabel?: string
}>()
</script>

<template>
  <section
    class="flex min-h-[72px] items-center gap-sm rounded-large border-2 border-error/30 bg-error/10 px-md py-lg text-error"
    :aria-label="ariaLabel ?? '거래 처리 결과'"
  >
    <span
      class="flex size-7 shrink-0 items-center justify-center"
      aria-hidden="true"
    >
      <ShieldX class="size-6" :stroke-width="2.5" />
    </span>
    <h2
      class="type-h2 break-keep text-[21px] font-bold leading-tight tracking-[-0.6px]"
    >
      {{ title ?? '거절된 거래입니다.' }}
    </h2>
  </section>
</template>
