<script setup lang="ts">
import { House, Phone, ShieldAlert } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'

import { useTransferStatus } from '@/composables/useTransferStatus'
import TransferExceptionDetailsCard from '@/pages/ward/transfer/-components/TransferExceptionDetailsCard.vue'
import TransferExceptionHero from '@/pages/ward/transfer/-components/TransferExceptionHero.vue'
import TransferGuardianCallModal from '@/pages/ward/transfer/-components/TransferGuardianCallModal.vue'
import { formatTransferDateTime } from '@/pages/ward/transfer/-utils/transfer-status-route'
import { useTransferStore } from '@/stores/transfer.store'

const route = useRoute()
const router = useRouter()
const transferStore = useTransferStore()
const isGuardianCallModalOpen = ref(false)
const transactionId = computed(() => Number(route.params.transactionId))
const { transferDetail } = useTransferStatus(transactionId)
const formatMoney = (value: number) =>
  `${new Intl.NumberFormat('ko-KR').format(value)}원`

const detailRows = computed(() => {
  const transfer = transferDetail.value
  if (!transfer) return []
  return [
    {
      label: '거래 금액',
      value: formatMoney(transfer.amount),
      emphasis: 'error' as const,
      numeric: true,
      large: true,
    },
    {
      label: '받는 분 / 은행',
      value: `${transfer.recipientName} / ${transfer.bankName}`,
      large: true,
    },
    {
      label: '요청 일시',
      value: formatTransferDateTime(transfer.requestedAt),
      numeric: true,
      large: true,
    },
  ]
})

function goHome() {
  transferStore.reset()
  router.replace({ name: 'ward-home' })
}

onBeforeRouteLeave(() => {
  transferStore.reset()
  return true
})
</script>

<template>
  <div
    class="flex min-h-[calc(100vh-var(--spacing-header)-var(--spacing-xl))] flex-col gap-xl"
  >
    <TransferExceptionHero
      title="보호자가&#10;거래를 거절했습니다"
      tone="error"
    >
      <template #icon>
        <ShieldAlert class="size-10" :stroke-width="2.25" />
      </template>
    </TransferExceptionHero>

    <section
      class="flex items-center gap-md rounded-large border-2 border-error/20 bg-error/10 p-xl text-error"
    >
      <ShieldAlert
        class="size-xl shrink-0"
        :stroke-width="2.25"
        aria-hidden="true"
      />
      <h3 class="type-h2">위험한 거래로 추정됩니다</h3>
    </section>

    <TransferExceptionDetailsCard title="취소된 거래 내역" :rows="detailRows" />

    <div class="mt-auto flex flex-col gap-md pt-lg">
      <Button
        class="w-full !gap-sm !px-md"
        label="보호자에게 연락하기"
        size="large"
        @click="isGuardianCallModalOpen = true"
      >
        <template #leading>
          <Phone :stroke-width="2.5" />
        </template>
      </Button>
      <Button
        class="w-full !gap-sm !px-md"
        label="홈으로 이동"
        variant="secondary"
        size="large"
        @click="goHome"
      >
        <template #leading>
          <House :stroke-width="2.5" />
        </template>
      </Button>
    </div>

    <TransferGuardianCallModal v-model:open="isGuardianCallModalOpen" />
  </div>
</template>
