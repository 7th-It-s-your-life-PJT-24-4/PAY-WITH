<script setup lang="ts">
const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9']

withDefaults(
  defineProps<{
    cancelLabel?: string
    disabled?: boolean
  }>(),
  {
    cancelLabel: '취소',
    disabled: false,
  },
)

const emit = defineEmits<{
  input: [value: string]
  cancel: []
  backspace: []
}>()
</script>

<template>
  <div
    class="grid w-full max-w-[350px] grid-cols-3 gap-md"
    role="group"
    aria-label="숫자 키패드"
  >
    <button
      v-for="key in keys"
      :key="key"
      class="type-numeric-input flex h-16 items-center justify-center rounded-medium border border-border bg-surface-card text-body shadow-card outline-none focus-visible:ring-2 focus-visible:ring-focus disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
      type="button"
      :disabled="disabled"
      :aria-label="key"
      @click="emit('input', key)"
    >
      {{ key }}
    </button>
    <button
      class="type-h4 flex h-16 items-center justify-center rounded-medium bg-disabled/60 text-body-secondary outline-none focus-visible:ring-2 focus-visible:ring-focus disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
      type="button"
      :disabled="disabled"
      @click="emit('cancel')"
    >
      {{ cancelLabel }}
    </button>
    <button
      class="type-numeric-input flex h-16 items-center justify-center rounded-medium border border-border bg-surface-card text-body shadow-card outline-none focus-visible:ring-2 focus-visible:ring-focus disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
      type="button"
      :disabled="disabled"
      aria-label="0"
      @click="emit('input', '0')"
    >
      0
    </button>
    <button
      class="flex h-16 items-center justify-center rounded-medium bg-disabled/60 text-body-secondary outline-none focus-visible:ring-2 focus-visible:ring-focus disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
      type="button"
      :disabled="disabled"
      aria-label="한 글자 지우기"
      @click="emit('backspace')"
    >
      <svg aria-hidden="true" class="h-5 w-6" viewBox="0 0 24 20" fill="none">
        <path
          d="M9 3h11a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H9L2 10l7-7Z"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linejoin="round"
        />
        <path
          d="m13 7 5 6m0-6-5 6"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
        />
      </svg>
    </button>
  </div>
</template>
