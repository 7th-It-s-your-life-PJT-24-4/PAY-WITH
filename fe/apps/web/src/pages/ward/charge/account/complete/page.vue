<script setup lang="ts">
import { CircleCheckBig } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import ChargeBankMark from '@/pages/ward/charge/-components/ChargeBankMark.vue'
import { useChargeStore } from '@/stores/charge.store'

const router = useRouter()
const chargeStore = useChargeStore()
</script>

<template>
  <div class="flex flex-col gap-section text-center">
    <section class="flex flex-col items-center">
      <span
        class="flex size-24 items-center justify-center rounded-full bg-action text-on-action shadow-modal"
        aria-hidden="true"
      >
        <CircleCheckBig class="size-12" :stroke-width="2.5" />
      </span>
      <h2 class="type-h1 mt-xl">계좌 등록이 완료되었습니다</h2>
    </section>

    <article
      v-if="chargeStore.registeredAccount"
      class="flex items-center gap-md rounded-large border border-border bg-surface-card p-xl text-left shadow-card"
    >
      <ChargeBankMark :bank-code="chargeStore.registeredAccount.bankCode" />
      <div class="min-w-0">
        <h3 class="type-h3">{{ chargeStore.registeredAccount.bankName }}</h3>
        <p class="type-h2 font-number truncate">
          {{ chargeStore.registeredAccount.accountNo }}
        </p>
      </div>
    </article>

    <Button
      class="w-full"
      label="확인"
      size="large"
      @click="router.replace({ name: 'ward-charge' })"
    />
  </div>
</template>
