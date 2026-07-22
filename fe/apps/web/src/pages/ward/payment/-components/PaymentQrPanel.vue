<script setup lang="ts">
defineProps<{
  expired: boolean
}>()

const emit = defineEmits<{
  scan: []
  reissue: []
}>()
</script>

<template>
  <section
    class="rounded-[32px] bg-surface-card p-xxl shadow-modal"
    aria-label="결제 QR 코드"
  >
    <button
      v-if="!expired"
      class="flex aspect-square w-full items-center justify-center rounded-large border-[6px] border-primary-900 bg-white p-md outline-none focus-visible:ring-2 focus-visible:ring-focus"
      type="button"
      aria-label="QR 코드 스캔 완료"
      @click="emit('scan')"
    >
      <svg viewBox="0 0 29 29" class="size-full text-body" aria-hidden="true">
        <rect width="29" height="29" fill="white" />
        <path
          fill="currentColor"
          d="M1 1h8v8H1V1Zm2 2v4h4V3H3Zm17-2h8v8h-8V1Zm2 2v4h4V3h-4ZM1 20h8v8H1v-8Zm2 2v4h4v-4H3Zm8-20h3v3h-3V2Zm4 0h3v6h-3V2Zm-4 5h5v3h-5V7Zm7 3h3v3h-3v-3Zm-8 2h4v4h-4v-4Zm6 2h3v3h-3v-3Zm5 1h7v3h-7v-3Zm-9 3h3v3h-3v-3Zm5 1h3v7h-3v-7Zm5 1h3v3h-3v-3Zm4 0h2v8h-5v-3h2v-2h-2v-3h3Zm-15 4h4v4h-4v-4Z"
        />
      </svg>
    </button>

    <button
      v-else
      class="flex aspect-square w-full flex-col items-center justify-center gap-lg rounded-large border-[6px] border-primary-900 bg-white text-body outline-none focus-visible:ring-2 focus-visible:ring-focus"
      type="button"
      @click="emit('reissue')"
    >
      <span
        class="flex size-40 items-center justify-center rounded-full bg-primary-900 text-primary-300"
        aria-hidden="true"
      >
        <svg viewBox="0 0 96 96" class="size-24" fill="none">
          <path
            d="M70 32A30 30 0 0 0 24 50m2-18v18h18M26 64a30 30 0 0 0 46-18m-2 18V46H52"
            stroke="currentColor"
            stroke-width="8"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </span>
      <span class="type-h3">QR 코드 재발급</span>
    </button>
  </section>
</template>
