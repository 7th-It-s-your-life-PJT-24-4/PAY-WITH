<script setup lang="ts">
import QrcodeVue from 'qrcode.vue'

defineProps<{
  paymentToken: string
  expired: boolean
  processing?: boolean
}>()

const emit = defineEmits<{
  reissue: []
}>()
</script>

<template>
  <section
    class="rounded-[32px] bg-surface-card p-xxl shadow-modal"
    aria-label="결제 QR 영역"
  >
    <div
      v-if="!expired"
      class="relative flex aspect-square w-full items-center justify-center rounded-large border-[6px] border-primary-900 bg-white p-md"
      aria-label="결제 QR 코드"
    >
      <QrcodeVue
        :value="paymentToken"
        :size="512"
        level="M"
        render-as="svg"
        :margin="2"
        class="size-full"
        aria-hidden="true"
      />
      <div
        v-if="processing"
        class="absolute inset-0 flex flex-col items-center justify-center gap-md rounded-medium bg-white/95 text-primary-300"
        role="status"
      >
        <span
          class="size-12 animate-spin rounded-full border-4 border-primary-900 border-t-primary-300"
        />
        <span class="type-h3">결제를 처리하고 있습니다</span>
      </div>
    </div>

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
