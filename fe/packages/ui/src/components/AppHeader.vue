<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    showBack?: boolean
    showProfile?: boolean
  }>(),
  {
    showBack: false,
    showProfile: false,
  },
)

const emit = defineEmits<{
  back: []
  profile: []
}>()
</script>

<template>
  <header
    class="grid h-header w-full grid-cols-[var(--spacing-button-small)_1fr_var(--spacing-button-small)] items-center border-b border-border bg-surface-card px-lg shadow-[0_1px_1px_rgb(0_0_0/5%)]"
  >
    <slot name="left">
      <button
        v-if="showBack"
        class="flex size-button-small items-center justify-center rounded-full text-primary-300 outline-none focus-visible:ring-2 focus-visible:ring-focus"
        type="button"
        aria-label="뒤로 가기"
        @click="emit('back')"
      >
        <svg aria-hidden="true" class="size-4" viewBox="0 0 16 16" fill="none">
          <path
            d="M10.5 3 5.5 8l5 5"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
      <span v-else aria-hidden="true" />
    </slot>

    <h1 class="type-h3 truncate text-center text-primary-300">{{ title }}</h1>

    <slot name="right">
      <button
        v-if="showProfile"
        class="flex size-button-small items-center justify-center rounded-full text-primary-300 outline-none focus-visible:ring-2 focus-visible:ring-focus"
        type="button"
        aria-label="내 정보"
        @click="emit('profile')"
      >
        <svg aria-hidden="true" class="size-5" viewBox="0 0 20 20" fill="none">
          <circle
            cx="10"
            cy="10"
            r="8"
            stroke="currentColor"
            stroke-width="1.7"
          />
          <circle
            cx="10"
            cy="7"
            r="2.2"
            stroke="currentColor"
            stroke-width="1.5"
          />
          <path
            d="M5.8 15c.9-2.1 2.3-3.1 4.2-3.1s3.3 1 4.2 3.1"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
          />
        </svg>
      </button>
      <span v-else aria-hidden="true" />
    </slot>
  </header>
</template>
