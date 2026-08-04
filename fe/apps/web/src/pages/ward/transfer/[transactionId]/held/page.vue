<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useTransferStatus } from '@/composables/useTransferStatus'
import TransferHeldView from '@/pages/ward/transfer/-components/TransferHeldView.vue'
import { formatTransferDateTime } from '@/pages/ward/transfer/-utils/transfer-status-route'

const route = useRoute()
const router = useRouter()
const isCancelModalOpen = ref(false)
const transactionId = computed(() => Number(route.params.transactionId))
const {
  transferDetail,
  canCancel,
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
      value: `${transfer.holderName} / ${transfer.bankName}`,
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

function waitAtHome() {
  router.push({ name: 'ward-home' })
}
</script>

<template>
  <TransferHeldView
    v-model:cancel-open="isCancelModalOpen"
    :rows="detailRows"
    :can-cancel="canCancel"
    :cancelling="isCancelling"
    :error-message="errorMessage"
    @wait-home="waitAtHome"
    @cancel="confirmCancel"
  />
</template>
