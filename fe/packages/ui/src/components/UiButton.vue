<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    variant?: 'primary' | 'secondary'
    disabled?: boolean
  }>(),
  {
    variant: 'primary',
    disabled: false,
  },
)
</script>

<template>
  <button
    class="inline-flex min-h-10 items-center justify-center rounded-md border px-3.5 py-2 text-sm font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:cursor-not-allowed disabled:opacity-50"
    :class="
      variant === 'primary'
        ? 'border-slate-900 bg-slate-900 text-white hover:bg-slate-800'
        : 'border-slate-300 bg-white text-slate-950 hover:bg-slate-50'
    "
    :disabled="disabled"
    type="button"
  >
    {{ label }}
  </button>
</template>
