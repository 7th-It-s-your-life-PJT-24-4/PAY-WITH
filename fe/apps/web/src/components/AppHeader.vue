<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>

<template>
  <header class="border-b border-slate-200 bg-white">
    <div class="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
      <RouterLink to="/" class="text-lg font-semibold text-slate-950">
        {{ title }}
      </RouterLink>
      <span class="rounded bg-emerald-50 px-2 py-1 text-sm text-emerald-700">
        Vue 3 + Vite
      </span>
    </div>
  </header>
</template>
