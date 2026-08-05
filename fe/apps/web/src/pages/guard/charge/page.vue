<script setup lang="ts">
import { Landmark } from '@lucide/vue'
import { PhWallet } from '@phosphor-icons/vue'
import { ConfirmModal } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { guardChargeHistoriesOptions } from '@/lib/query/guard/charge'
import { guardHomeOptions } from '@/lib/query/guard/home'
import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'
import { useGuardWardQuery } from '@/pages/guard/-composables/useGuardWardQuery'
import { usePairingStore } from '@/stores/pairing.store'

const router = useRouter()
const pairingStore = usePairingStore()
const { selectedWardId, selectWard } = useGuardWardQuery()
const isPairingConfirmOpen = ref(false)
const guardHomeQuery = useQuery(guardHomeOptions(selectedWardId))
const chargeHistoriesQuery = useQuery(guardChargeHistoriesOptions())

const seniors = computed(() =>
  (guardHomeQuery.data.value?.wards ?? []).map(
    ({ wardId, name, avatarId }) => ({
      id: String(wardId),
      name,
      imageUrl: `/images/avatar/avatar${avatarId}.png`,
    }),
  ),
)
const activeSeniorId = computed(() =>
  selectedWardId.value === null ? '' : String(selectedWardId.value),
)
const chargeHistories = computed(() =>
  (chargeHistoriesQuery.data.value ?? [])
    .filter(({ wardId }) => wardId === selectedWardId.value)
    .map((history) => ({
      ...history,
      id: String(history.transactionId),
      date: new Intl.DateTimeFormat('ko-KR', {
        month: 'long',
        day: 'numeric',
      }).format(new Date(history.createdAt)),
    })),
)
const hasChargeHistory = computed(() => chargeHistories.value.length > 0)
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)

watch(
  () => guardHomeQuery.data.value?.selectedWard?.wardId,
  (wardId) => {
    if (wardId) selectWard(wardId)
  },
  { immediate: true },
)

function selectSenior(wardId: string) {
  const parsedWardId = Number(wardId)
  if (!Number.isSafeInteger(parsedWardId) || parsedWardId <= 0) return
  selectWard(parsedWardId)
}

async function startPairing() {
  const issued = await pairingStore.issueCode()
  if (!issued) return

  isPairingConfirmOpen.value = false
  router.push({ name: 'guard-pairing-code' })
}
</script>

<template>
  <main
    class="relative min-h-screen pb-[calc(138px+env(safe-area-inset-bottom))]"
  >
    <div class="px-mobile-gutter pt-md">
      <GuardSeniorAvatarList
        :seniors="seniors"
        :active-senior-id="activeSeniorId"
        @add="isPairingConfirmOpen = true"
        @select="selectSenior"
      />

      <section class="mt-lg" aria-labelledby="guard-charge-history-title">
        <h1
          id="guard-charge-history-title"
          class="text-[18px] font-bold leading-[1.2] tracking-[-0.36px] text-black"
        >
          충전 내역
        </h1>

        <div v-if="hasChargeHistory" class="mt-lg">
          <template
            v-for="(history, index) in chargeHistories"
            :key="history.id"
          >
            <p
              v-if="
                index === 0 || chargeHistories[index - 1]?.date !== history.date
              "
              class="type-body-medium mb-xs text-gray-500"
              :class="index > 0 ? 'mt-md' : ''"
            >
              {{ history.date }}
            </p>

            <button
              class="flex h-[60px] w-full items-center bg-white px-sm text-left"
              type="button"
              :aria-label="`${history.date} 충전 상세 보기`"
              @click="
                router.push({
                  name: 'guard-charge-detail',
                  params: { chargeId: history.id },
                })
              "
            >
              <span
                class="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-primary-500 text-white"
              >
                <Landmark class="size-[18px]" aria-hidden="true" />
              </span>
              <div class="ml-md min-w-0 flex-1">
                <p
                  class="text-[14px] font-semibold leading-[1.2] tracking-[-0.28px] text-black"
                >
                  -{{ formatMoney(history.amount) }}원
                </p>
                <p
                  class="mt-xxs truncate text-[12px] font-medium leading-[1.2] tracking-[-0.24px] text-gray-700"
                >
                  {{ history.wardName }} 충전
                </p>
              </div>
            </button>
          </template>
        </div>

        <div
          v-else
          class="flex min-h-[calc(100dvh-260px-66px-env(safe-area-inset-bottom))] flex-col items-center justify-center text-center"
        >
          <p
            class="text-[16px] font-medium leading-6 tracking-[-0.2px] text-gray-500"
          >
            아직 충전한 이력이 없어요.<br />
            시니어에게 안전하게 자산을 전달하세요.
          </p>
          <button
            class="mt-lg flex h-14 w-full items-center justify-center rounded-[10px] bg-primary-500 text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-white shadow-[0_10px_15px_-3px_rgb(0_0_0/10%),0_4px_6px_-4px_rgb(0_0_0/10%)] transition-colors hover:bg-primary-400 active:bg-primary-300"
            type="button"
            @click="router.push({ name: 'guard-charge-be' })"
          >
            시니어에게 첫 충전하기
          </button>
        </div>
      </section>
    </div>

    <div
      v-if="hasChargeHistory"
      class="fixed inset-x-0 bottom-[calc(66px+env(safe-area-inset-bottom)+32px)] z-30 mx-auto flex w-full max-w-[390px] justify-end px-mobile-gutter"
    >
      <button
        class="flex h-10 items-center gap-xxs rounded-full bg-primary-500 px-[10px] text-4 font-medium leading-[1.6] tracking-[-0.32px] text-white shadow-[0_10px_15px_-3px_rgb(0_0_0/10%),0_4px_6px_-4px_rgb(0_0_0/10%)] transition-colors hover:bg-primary-400 active:bg-primary-300"
        type="button"
        @click="router.push({ name: 'guard-charge-be' })"
      >
        <PhWallet class="size-5" aria-hidden="true" weight="fill" />
        충전하기
      </button>
    </div>

    <ConfirmModal
      v-model:open="isPairingConfirmOpen"
      title="인증 코드를 생성할까요?"
      cancel-label="취소"
      :confirm-label="pairingStore.isIssuingCode ? '생성 중' : '생성'"
      :confirm-disabled="pairingStore.isIssuingCode"
      @confirm="startPairing"
    />
  </main>
</template>
