<script setup lang="ts">
import { Check, Eye, ShieldCheck, UserRound } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import { usePairingStore } from '@/stores/pairing.store'

const router = useRouter()
const pairingStore = usePairingStore()
</script>

<template>
  <div class="flex flex-col items-center text-center">
    <span
      class="flex size-[96px] items-center justify-center rounded-full bg-primary-500 text-on-action shadow-card"
      aria-hidden="true"
    >
      <Check class="size-12" :stroke-width="3" />
    </span>

    <h1 class="type-h1 mt-lg text-body">연결 성공!</h1>
    <p class="type-body mt-sm text-body-muted">
      {{ pairingStore.guardian?.name }} 보호자님과 안전하게 연결되었습니다.<br />
      이제 보호자가 이상 거래를 함께 확인합니다.
    </p>

    <section
      class="mt-xl flex w-full items-center justify-between rounded-large bg-surface-card px-xl py-lg shadow-card"
      aria-label="연결된 보호자"
    >
      <div class="flex flex-col items-center gap-xs">
        <span
          class="flex size-[64px] items-center justify-center rounded-full border-2 border-primary-500 bg-primary-900 text-primary-500"
        >
          <UserRound class="size-8" aria-hidden="true" />
        </span>
        <span class="type-body-medium text-body">나</span>
      </div>

      <div class="flex flex-1 flex-col items-center px-sm text-primary-500">
        <div class="flex w-full items-center gap-xs">
          <span class="h-px flex-1 bg-primary-500/30" />
          <ShieldCheck class="size-xl" aria-hidden="true" />
          <span class="h-px flex-1 bg-primary-500/30" />
        </div>
        <span
          class="type-caption mt-xs rounded-full bg-primary-900 px-md py-xs"
        >
          안전 연결됨
        </span>
      </div>

      <div class="flex flex-col items-center gap-xs">
        <span
          class="flex size-[64px] items-center justify-center rounded-full border-2 border-border bg-disabled/40 text-body-secondary"
        >
          <UserRound class="size-8" aria-hidden="true" />
        </span>
        <span class="type-body-medium text-body">{{
          pairingStore.guardian?.name
        }}</span>
      </div>
    </section>

    <div class="mt-xl grid w-full gap-md text-left">
      <article
        class="flex gap-md rounded-medium border border-border bg-surface-card p-lg"
      >
        <ShieldCheck
          class="size-xl shrink-0 text-primary-500"
          aria-hidden="true"
        />
        <div>
          <h2 class="type-h4 text-body">보호 기능 활성화</h2>
          <p class="type-caption mt-xs text-body-muted">
            의심 거래나 고액 결제 시 보호자에게 바로 알림이 전송됩니다.
          </p>
        </div>
      </article>
      <article
        class="flex gap-md rounded-medium border border-border bg-surface-card p-lg"
      >
        <Eye class="size-xl shrink-0 text-primary-500" aria-hidden="true" />
        <div>
          <h2 class="type-h4 text-body">보호를 위한 모니터링</h2>
          <p class="type-caption mt-xs text-body-muted">
            보호자는 거래 내역을 확인하고 필요한 거래를 승인할 수 있습니다.
          </p>
        </div>
      </article>
    </div>

    <Button
      class="mt-xl w-full"
      label="홈으로"
      @click="router.replace({ name: 'ward-home' })"
    />
  </div>
</template>
