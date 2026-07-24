<script setup lang="ts">
import { CircleAlert, House, Info, Phone } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useTransferStatus } from '@/composables/useTransferStatus'
import TransferExceptionDetailsCard from '@/pages/ward/transfer/-components/TransferExceptionDetailsCard.vue'
import TransferExceptionHero from '@/pages/ward/transfer/-components/TransferExceptionHero.vue'
import TransferGuardianCallModal from '@/pages/ward/transfer/-components/TransferGuardianCallModal.vue'
import { formatTransferDateTime } from '@/pages/ward/transfer/-utils/transfer-status-route'

const route = useRoute()
const router = useRouter()
const isGuardianCallModalOpen = ref(false)
const transactionId = computed(() => Number(route.params.transactionId))
const { transferDetail, errorMessage } = useTransferStatus(transactionId, {
  pollWhileHeld: true,
})
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
      label: '금액',
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
</script>

<template>
  <div
    class="flex min-h-[calc(100vh-var(--spacing-header)-var(--spacing-xl))] flex-col gap-xl"
  >
    <TransferExceptionHero
      title="거래를 진행할 수 없습니다"
      :description="`보호자의 승인을 기다리고 있는 송금이 있습니다.\n해당 송금이 완료된 후 다시 시도해 주세요.`"
    >
      <template #icon>
        <CircleAlert class="size-12" :stroke-width="2.25" />
      </template>
    </TransferExceptionHero>

    <TransferExceptionDetailsCard
      title="대기 중인 거래"
      badge-label="승인 대기 중"
      badge-status="warning"
      :rows="detailRows"
    />

    <section
      class="flex gap-md rounded-large bg-disabled/45 p-lg text-body-secondary"
    >
      <Info
        class="mt-xxs size-xl shrink-0 text-primary-300"
        :stroke-width="2.25"
        aria-hidden="true"
      />
      <p class="type-body-medium">
        이 기능은 안전한 금융 생활을 위해 설정되어 있습니다. 보호자에게 승인
        요청 알림이 이미 전송되었습니다.
      </p>
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
        label="홈으로"
        variant="outline-primary"
        size="large"
        @click="router.replace({ name: 'ward-home' })"
      >
        <template #leading>
          <House :stroke-width="2.5" />
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
  </div>
</template>
