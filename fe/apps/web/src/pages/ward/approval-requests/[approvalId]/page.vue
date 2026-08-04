<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { HTTPError } from 'ky'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { wardApprovalDetailOptions } from '@/lib/query/ward/home'
import { getApiErrorMessage } from '@/api/error'
import TransferHeldView from '@/pages/ward/transfer/-components/TransferHeldView.vue'
import { formatTransferDateTime } from '@/pages/ward/transfer/-utils/transfer-status-route'

const route = useRoute()
const router = useRouter()
const approvalId = computed(() => {
  const value = Number(route.params.approvalId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const approvalQuery = useQuery(wardApprovalDetailOptions(approvalId))
const errorMessage = ref('')
const approvalNotFound = computed(
  () =>
    approvalQuery.error.value instanceof HTTPError &&
    approvalQuery.error.value.response.status === 404,
)
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

watch(
  () => approvalQuery.error.value,
  async (error) => {
    errorMessage.value =
      error && !approvalNotFound.value
        ? await getApiErrorMessage(
            error,
            '승인 대기 거래를 불러오지 못했습니다.',
          )
        : ''
  },
  { immediate: true },
)
</script>

<template>
  <section
    v-if="approvalId === null"
    class="flex min-h-64 flex-col items-center justify-center rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
    role="alert"
  >
    <h1 class="type-h3 text-body">올바르지 않은 승인 요청 번호입니다</h1>
    <Button
      class="mt-xl w-full"
      label="홈으로 돌아가기"
      size="large"
      @click="router.replace({ name: 'ward-home' })"
    />
  </section>

  <div
    v-else-if="approvalQuery.isPending.value"
    class="flex min-h-64 items-center justify-center"
    aria-busy="true"
  >
    <p class="type-h3 text-body-muted">승인 대기 거래를 확인하고 있습니다</p>
  </div>

  <section
    v-else-if="approvalNotFound"
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

  <section
    v-else-if="approvalQuery.isError.value || !approvalQuery.data.value"
    class="flex min-h-64 flex-col items-center justify-center rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
    role="alert"
  >
    <h1 class="type-h3 text-body">승인 대기 거래를 불러오지 못했습니다</h1>
    <p class="type-body-medium mt-xs text-body-muted">{{ errorMessage }}</p>
    <div class="mt-xl grid w-full gap-md">
      <Button
        class="w-full"
        :label="
          approvalQuery.isFetching.value
            ? '다시 확인하고 있습니다'
            : '다시 시도'
        "
        size="large"
        :disabled="approvalQuery.isFetching.value"
        @click="approvalQuery.refetch()"
      />
      <Button
        class="w-full"
        label="홈으로 돌아가기"
        variant="outline-primary"
        size="large"
        @click="router.replace({ name: 'ward-home' })"
      />
    </div>
  </section>

  <TransferHeldView
    v-else
    :rows="detailRows"
    @wait-home="router.replace({ name: 'ward-home' })"
  />
</template>
