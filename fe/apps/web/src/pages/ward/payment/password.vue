<script setup lang="ts">
import { PinKeypad } from '@pay-with/ui'
import { useRouter } from 'vue-router'

const router = useRouter()

function handleComplete() {
  router.push({ name: 'ward-payment-complete' })
}
</script>

<template>
  <div
    class="flex min-h-[calc(100vh-var(--spacing-header)-var(--spacing-xl))] flex-col"
  >
    <section>
      <h2 class="type-h1 text-body">
        안전한 결제를 위해<br />비밀번호를 입력해주세요
      </h2>
    </section>

    <section
      class="-mx-mobile-gutter mt-auto rounded-t-[32px] bg-surface-card px-xl pb-xl pt-[88px] shadow-[0_-8px_12px_rgb(0_0_0/4%)]"
      aria-label="비밀번호 키패드"
    >
      <PinKeypad @complete="handleComplete" />
    </section>
  </div>
</template>
