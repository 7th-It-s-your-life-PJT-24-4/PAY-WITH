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
    class="grid h-header w-full grid-cols-[var(--spacing-button-small)_1fr_var(--spacing-button-small)] items-center border-b border-border bg-surface px-mobile-gutter"
  >
    <button
      v-if="showBack"
      class="flex size-button-small items-center justify-center rounded-full text-2xl text-primary-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus"
      type="button"
      aria-label="뒤로 가기"
      @click="emit('back')"
    >
      ←
    </button>
    <span v-else aria-hidden="true" />

    <p class="type-h3 text-center text-primary-300">
      {{ title }}
    </p>

    <button
      v-if="showProfile"
      class="flex size-button-small items-center justify-center rounded-full text-lg font-semibold text-primary-300 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus"
      type="button"
      aria-label="내 정보"
      @click="emit('profile')"
    >
      ○
    </button>
    <span v-else aria-hidden="true" />
  </header>
</template>
