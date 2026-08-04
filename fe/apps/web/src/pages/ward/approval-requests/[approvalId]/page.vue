<script setup lang="ts">
import { Phone, TriangleAlert } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { wardApprovalDetailOptions } from '@/lib/query/ward/home'
import TransferExceptionDetailsCard from '@/pages/ward/transfer/-components/TransferExceptionDetailsCard.vue'
import TransferExceptionHero from '@/pages/ward/transfer/-components/TransferExceptionHero.vue'
import TransferGuardianCallModal from '@/pages/ward/transfer/-components/TransferGuardianCallModal.vue'
import { formatTransferDateTime } from '@/pages/ward/transfer/-utils/transfer-status-route'

const route = useRoute()
const router = useRouter()
const isGuardianCallModalOpen = ref(false)
const approvalId = computed(() => {
  const value = Number(route.params.approvalId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const approvalQuery = useQuery(wardApprovalDetailOptions(approvalId))
const formatMoney = (value: number) =>
  `${new Intl.NumberFormat('ko-KR').format(value)}원`
const detailRows = computed(() => {
  const approval = approvalQuery.data.value
  if (!approval) return []
  return [
    {
      label: '받는 분 / 은행',
      value: `${approval.holderName} / ${approval.bankName}`,
    },
    {
      label: '계좌번호',
      value: approval.accountNo,
      numeric: true,
    },
    {
      label: '송금 금액',
      value: formatMoney(approval.amount),
      emphasis: 'primary' as const,
      numeric: true,
    },
    ...(approval.memo ? [{ label: '메모', value: approval.memo }] : []),
    {
      label: '요청 시간',
      value: formatTransferDateTime(approval.requestedAt),
      numeric: true,
    },
  ]
})
</script>

<template>
  <div
    v-if="approvalQuery.isPending.value"
    class="flex min-h-64 items-center justify-center"
    aria-busy="true"
  >
    <p class="type-h3 text-body-muted">승인 대기 거래를 확인하고 있습니다</p>
  </div>

  <section
    v-else-if="approvalQuery.isError.value || !approvalQuery.data.value"
    class="flex min-h-64 flex-col items-center justify-center rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
    role="alert"
  >
    <h1 class="type-h3 text-body">승인 대기 거래를 확인할 수 없습니다</h1>
    <p class="type-body-medium mt-xs text-body-muted">
      이미 처리되었거나 만료된 거래일 수 있습니다.
    </p>
    <Button
      class="mt-xl w-full"
      label="홈으로 돌아가기"
      size="large"
      @click="router.replace({ name: 'ward-home' })"
    />
  </section>

  <div v-else class="flex flex-col gap-xl">
    <TransferExceptionHero
      title="보호자 승인을 기다리고 있어요"
      :description="`안전을 위해 보호자가
거래 내용을 확인하고 있습니다.`"
    >
      <template #icon>
        <TriangleAlert class="size-11" :stroke-width="2.25" />
      </template>
    </TransferExceptionHero>

    <TransferExceptionDetailsCard
      title="승인 대기 송금"
      badge-label="확인 필요"
      badge-status="warning"
      :rows="detailRows"
    />

    <div class="mt-auto flex flex-col gap-md pt-lg">
      <Button
        class="w-full"
        label="보호자에게 연락하기"
        variant="outline-primary"
        size="large"
        @click="isGuardianCallModalOpen = true"
      >
        <template #leading>
          <Phone :stroke-width="2.5" />
        </template>
      </Button>
      <Button
        class="w-full"
        label="홈에서 기다리기"
        size="large"
        @click="router.replace({ name: 'ward-home' })"
      />
    </div>

    <TransferGuardianCallModal v-model:open="isGuardianCallModalOpen" />
  </div>
</template>
