<script setup lang="ts">
defineProps<{
  balance: string
}>()
</script>

<template>
  <section
    class="relative overflow-hidden rounded-large border border-border bg-surface-card p-xl shadow-card"
    aria-labelledby="ward-balance-title"
  >
    <div
      class="pointer-events-none absolute -right-12 -top-12 size-48 rounded-full bg-primary-500/10 blur-3xl"
    />
    <div class="relative">
      <h2 id="ward-balance-title" class="type-h4 text-primary-300">
        나의 잔액
      </h2>
      <p class="mt-sm flex items-baseline gap-xs text-body">
        <strong class="type-amount">
          {{ balance }}
        </strong>
        <span class="type-h2 font-medium">원</span>
      </p>
    </div>
  </section>
</template>
