<script setup lang="ts">
import { Phone, ShieldCheck, TriangleAlert, X } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import TransferExceptionDetailsCard from '@/pages/ward/transfer/-components/TransferExceptionDetailsCard.vue'
import TransferExceptionHero from '@/pages/ward/transfer/-components/TransferExceptionHero.vue'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const pendingTransfer = computed(() => transferStore.pendingTransfer)
const formatMoney = (value: number) =>
  `${new Intl.NumberFormat('ko-KR').format(value)}원`

const detailRows = computed(() => {
  const transfer = pendingTransfer.value
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
      value: transfer.requestedAt,
      numeric: true,
    },
  ]
})

function cancelTransfer() {
  transferStore.reset()
  router.replace({ name: 'ward-home' })
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
        @click="transferStore.requestGuardianContact"
      >
        <template #leading>
          <Phone :stroke-width="2.5" />
        </template>
      </Button>
      <Button
        class="w-full !gap-sm !px-md"
        label="거래 취소하기"
        variant="outline-danger"
        size="large"
        @click="cancelTransfer"
      >
        <template #leading>
          <X :stroke-width="2.5" />
        </template>
      </Button>
      <p
        v-if="transferStore.guardianContactRequested"
        class="sr-only"
        role="status"
        aria-live="polite"
      >
        보호자 연락을 요청했습니다.
      </p>
    </div>
  </div>
</template>
