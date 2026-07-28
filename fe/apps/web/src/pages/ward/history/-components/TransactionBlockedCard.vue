<script setup lang="ts">
import { ShieldX } from '@lucide/vue'
</script>

<template>
  <section
    class="flex min-h-[88px] items-center gap-md rounded-large border-2 border-error/30 bg-error/10 p-xl text-error"
    aria-label="보호자 거래 차단 결과"
  >
    <span
      class="flex size-8 shrink-0 items-center justify-center"
      aria-hidden="true"
    >
      <ShieldX class="size-6" :stroke-width="2.5" />
    </span>
    <h2 class="type-h2">거절된 거래입니다.</h2>
  </section>
</template>
