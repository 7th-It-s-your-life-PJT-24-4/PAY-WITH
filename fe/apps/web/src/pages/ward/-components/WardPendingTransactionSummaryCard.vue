<script setup lang="ts">
import { ChevronRight, ShieldAlert } from '@lucide/vue'

defineProps<{
  count: number
}>()

const emit = defineEmits<{
  open: []
}>()
</script>

<template>
  <section v-if="count" aria-labelledby="pending-transaction-summary-title">
    <button
      type="button"
      class="flex min-h-28 w-full items-center gap-md rounded-large border border-warning/30 bg-surface-card p-xl text-left shadow-card transition-colors hover:bg-primary-900/50 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary-500"
      :aria-label="`보호자 승인 대기 거래 ${count}건 확인하기`"
      @click="emit('open')"
    >
      <span
        class="flex size-12 shrink-0 items-center justify-center rounded-full bg-warning/15 text-warning"
        aria-hidden="true"
      >
        <ShieldAlert class="size-6" />
      </span>
      <span class="min-w-0 flex-1">
        <strong
          id="pending-transaction-summary-title"
          class="type-h3 block leading-tight text-body break-keep"
        >
          <span class="inline-block">승인 대기 거래가 </span>
          <span class="inline-block"
            ><span class="text-warning">{{ count }}건</span> 있습니다</span
          >
        </strong>
      </span>
      <ChevronRight
        class="size-6 shrink-0 text-body-muted"
        aria-hidden="true"
      />
    </button>
  </section>
</template>
