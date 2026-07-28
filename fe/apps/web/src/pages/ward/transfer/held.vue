<script setup lang="ts">
import { Phone, ShieldCheck, TriangleAlert, X } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

import { useTransferStatus } from '@/composables/useTransferStatus'
import TransferCancelModal from '@/pages/ward/transfer/-components/TransferCancelModal.vue'
import TransferExceptionDetailsCard from '@/pages/ward/transfer/-components/TransferExceptionDetailsCard.vue'
import TransferExceptionHero from '@/pages/ward/transfer/-components/TransferExceptionHero.vue'
import TransferGuardianCallModal from '@/pages/ward/transfer/-components/TransferGuardianCallModal.vue'
import { formatTransferDateTime } from '@/pages/ward/transfer/-utils/transfer-status-route'

const route = useRoute()
const isGuardianCallModalOpen = ref(false)
const isCancelModalOpen = ref(false)
const transactionId = computed(() => Number(route.params.transactionId))
const {
  transferDetail,
  isCancelling,
  errorMessage,
  cancel: cancelTransfer,
} = useTransferStatus(transactionId, { pollWhileHeld: true })
const formatMoney = (value: number) =>
  `${new Intl.NumberFormat('ko-KR').format(value)}원`

const detailRows = computed(() => {
  const transfer = transferDetail.value
  if (!transfer) return []
  return [
    {
      label: '받는 분 / 은행',
      value: `${transfer.recipientName} / ${transfer.bankName}`,
    },
    {
      label: '송금 금액',
      value: formatMoney(transfer.amount),
      emphasis: 'primary' as const,
      numeric: true,
    },
    {
      label: '요청 시간',
      value: formatTransferDateTime(transfer.requestedAt),
      numeric: true,
    },
  ]
})

async function confirmCancel() {
  const detail = await cancelTransfer()
  if (detail?.status === 'CANCELED') isCancelModalOpen.value = false
}
</script>

<template>
  <div
    class="flex min-h-[calc(100vh-var(--spacing-header)-var(--spacing-xl))] flex-col gap-xl"
  >
    <TransferExceptionHero
      title="잠깐 확인해 보세요!"
      :description="`평소와 다른 송금이 감지되어\n안전을 위해 잠시 멈췄습니다.`"
    >
      <template #icon>
        <TriangleAlert class="size-11" :stroke-width="2.25" />
      </template>
    </TransferExceptionHero>

    <TransferExceptionDetailsCard
      title="이상 거래 내용"
      badge-label="확인 필요"
      badge-status="warning"
      :rows="detailRows"
    />

    <section
      class="flex gap-md rounded-large border border-primary-500/30 bg-primary-500/10 p-lg text-primary-200"
    >
      <ShieldCheck
        class="mt-xxs size-xl shrink-0"
        :stroke-width="2.25"
        aria-hidden="true"
      />
      <div>
        <h3 class="type-h4">PayWith 안전 가이드</h3>
        <p class="type-body-medium mt-xxs">
          보호자가 등록한 안전 범위를 벗어난 송금입니다. 잘 모르는 거래라면
          취소하는 것이 안전합니다.
        </p>
      </div>
    </section>

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
        :label="isCancelling ? '거래를 취소하고 있습니다' : '거래 취소하기'"
        variant="outline-danger"
        size="large"
        :disabled="isCancelling"
        @click="isCancelModalOpen = true"
      >
        <template #leading>
          <X :stroke-width="2.5" />
        </template>
      </Button>
      <p
        v-if="errorMessage"
        class="type-body-medium text-center text-error"
        role="alert"
      >
        {{ errorMessage }}
      </p>
    </div>

    <TransferGuardianCallModal v-model:open="isGuardianCallModalOpen" />
    <TransferCancelModal
      v-model:open="isCancelModalOpen"
      :cancelling="isCancelling"
      @confirm="confirmCancel"
    />
  </div>
</template>
