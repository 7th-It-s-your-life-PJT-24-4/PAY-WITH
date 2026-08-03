<script setup lang="ts">
import { CircleCheckBig, CircleX, Clock3, TriangleAlert } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'

import { useTransferStatus } from '@/composables/useTransferStatus'
import { getTransferResultAction } from '@/pages/ward/transfer/-utils/transfer-result'
import { formatTransferDateTime } from '@/pages/ward/transfer/-utils/transfer-status-route'
import { useTransferStore } from '@/stores/transfer.store'
import type { TransferStatus } from '@/types/transfer'

type ResultStatus = Extract<
  TransferStatus,
  'COMPLETED' | 'EXPIRED' | 'FAILED' | 'CANCELED'
>

const props = defineProps<{ status: ResultStatus }>()

const route = useRoute()
const router = useRouter()
const transferStore = useTransferStore()
const transactionId = computed(() => Number(route.params.transactionId))
const { transferDetail } = useTransferStatus(transactionId)
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)

const presentation = computed(() => {
  switch (props.status) {
    case 'COMPLETED':
      return {
        badge: '송금 완료',
        description: '',
        tone: 'success' as const,
      }
    case 'EXPIRED':
      return {
        badge: '승인 시간 만료',
        description: '승인 시간이 지나 송금이 완료되지 않았습니다.',
        tone: 'warning' as const,
      }
    case 'CANCELED':
      return {
        badge: '송금 취소',
        description: '요청한 송금이 취소되었습니다.',
        tone: 'neutral' as const,
      }
    case 'FAILED':
      return {
        badge: '송금 실패',
        description:
          transferDetail.value?.failureMessage ||
          '송금을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.',
        tone: 'error' as const,
      }
  }

  return {
    badge: '송금 실패',
    description: '송금을 완료하지 못했습니다.',
    tone: 'error' as const,
  }
})

const occurredAt = computed(() => {
  const detail = transferDetail.value
  if (!detail) return null
  if (props.status === 'COMPLETED') return detail.completedAt
  if (props.status === 'EXPIRED') return detail.expiredAt
  return detail.respondedAt ?? detail.requestedAt
})

const primaryAction = computed(() => {
  return getTransferResultAction(
    props.status,
    transferDetail.value?.failureCode ?? null,
  )
})

async function moveTo(routeName: string) {
  transferStore.reset()
  await router.replace({ name: routeName })
}

onBeforeRouteLeave((to) => {
  transferStore.reset()
  if (props.status === 'COMPLETED' && to.name !== 'ward-home')
    return { name: 'ward-home', replace: true }

  return true
})
</script>

<template>
  <div class="flex flex-col gap-md">
    <section class="rounded-large bg-surface-card p-xl shadow-card">
      <div class="text-center">
        <p
          class="type-h4 inline-flex items-center gap-xs rounded-full px-md py-xs"
          :class="{
            'bg-primary-900 text-primary-500': presentation.tone === 'success',
            'bg-warning/10 text-warning': presentation.tone === 'warning',
            'bg-error/10 text-error': presentation.tone === 'error',
            'bg-disabled/45 text-body-secondary':
              presentation.tone === 'neutral',
          }"
        >
          <CircleCheckBig
            v-if="presentation.tone === 'success'"
            class="size-lg"
            :stroke-width="2.5"
            aria-hidden="true"
          />
          <Clock3
            v-else-if="presentation.tone === 'warning'"
            class="size-lg"
            :stroke-width="2.5"
            aria-hidden="true"
          />
          <TriangleAlert
            v-else-if="presentation.tone === 'error'"
            class="size-lg"
            :stroke-width="2.5"
            aria-hidden="true"
          />
          <CircleX
            v-else
            class="size-lg"
            :stroke-width="2.5"
            aria-hidden="true"
          />
          {{ presentation.badge }}
        </p>
        <h2 class="type-amount mt-md">
          <span class="font-number">{{
            formatMoney(transferDetail?.amount ?? 0)
          }}</span
          ><span class="type-h2">원</span>
        </h2>
        <p
          v-if="presentation.description"
          class="type-body-medium mt-md whitespace-pre-line text-body-secondary"
        >
          {{ presentation.description }}
        </p>
      </div>
      <div class="mt-xl flex items-center gap-md border-t border-border pt-md">
        <span
          class="type-h2 flex size-12 items-center justify-center rounded-full bg-primary-900 text-primary-300"
          >{{ transferDetail?.holderName.slice(0, 1) }}</span
        >
        <div>
          <h3 class="type-h4">{{ transferDetail?.holderName }}</h3>
          <p class="type-body-medium text-body-muted">
            {{ transferDetail?.bankName }} {{ transferDetail?.accountNo }}
          </p>
        </div>
      </div>
    </section>

    <section class="rounded-large bg-surface-card p-xl shadow-card">
      <dl class="grid gap-xl">
        <div class="flex justify-between">
          <dt>{{ status === 'COMPLETED' ? '송금 일시' : '처리 일시' }}</dt>
          <dd>
            <span class="font-number">{{
              occurredAt ? formatTransferDateTime(occurredAt) : '-'
            }}</span>
          </dd>
        </div>
        <div class="flex justify-between">
          <dt>결제 수단</dt>
          <dd>PayWith 머니</dd>
        </div>
        <div>
          <dt>메모</dt>
          <dd class="mt-sm rounded-medium bg-gray-900 p-md">
            {{ transferDetail?.memo || '메모 없음' }}
          </dd>
        </div>
      </dl>
    </section>

    <section
      v-if="status === 'COMPLETED'"
      class="rounded-large border border-primary-500/30 bg-primary-500/10 p-xl"
    >
      <h3 class="type-h4 text-primary-300">안전하게 거래 보호 중</h3>
      <p class="type-body-medium mt-xs text-body-secondary">
        보호자님이 확인한 안전한 거래입니다. PayWith 보안 엔진이 24시간 자산을
        지키고 있습니다.
      </p>
    </section>

    <div class="mt-lg flex flex-col gap-md">
      <Button
        v-if="primaryAction"
        class="w-full"
        :label="primaryAction.label"
        size="large"
        pill
        @click="moveTo(primaryAction.routeName)"
      />
      <Button
        class="w-full"
        label="홈으로"
        :variant="status === 'COMPLETED' ? 'primary' : 'secondary'"
        size="large"
        pill
        @click="moveTo('ward-home')"
      />
    </div>
  </div>
</template>
