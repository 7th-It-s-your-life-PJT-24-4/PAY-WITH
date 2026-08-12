<script setup lang="ts">
import { ShieldX } from '@lucide/vue'

defineProps<{
  title?: string
  description?: string
  ariaLabel?: string
}>()
</script>

<template>
  <section
    class="flex items-center gap-md rounded-large border-2 border-error/30 bg-error/10 p-lg text-error"
    :aria-label="ariaLabel ?? '거래 처리 결과'"
  >
    <span
      class="flex size-10 shrink-0 items-center justify-center rounded-full bg-error/20"
      aria-hidden="true"
    >
      <ShieldX class="size-6" :stroke-width="2.5" />
    </span>
    <div class="min-w-0 flex-1">
      <h2 class="text-[20px] font-bold leading-tight tracking-[-0.4px]">
        {{ title ?? '거절된 거래입니다.' }}
      </h2>
      <p
        v-if="description"
        class="mt-xs text-[16px] font-medium leading-relaxed opacity-90"
      >
        {{ description }}
      </p>
    </div>
  </section>
</template>
