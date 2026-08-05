<script setup lang="ts">
import { Button, ConfirmModal, Toast } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { useUnpairGuardWardMutation } from '@/composables/useUnpairGuardWardMutation'
import { guardHomeOptions } from '@/lib/query/guard/home'
import GuardMyHeader from '@/pages/guard/my/-components/GuardMyHeader.vue'
import type { GuardWard } from '@/schemas/guard-home.schema'

const router = useRouter()
const guardHomeQuery = useQuery(guardHomeOptions())
const unpairMutation = useUnpairGuardWardMutation()
const selectedWard = ref<GuardWard | null>(null)
const isUnpairConfirmOpen = ref(false)
const isToastOpen = ref(false)
const toastMessage = ref('')

const wards = computed(() => guardHomeQuery.data.value?.wards ?? [])
const confirmTitle = computed(() =>
  selectedWard.value
    ? `${selectedWard.value.name}님과 연결을 해제할까요?`
    : '연결을 해제할까요?',
)

function openUnpairConfirm(ward: GuardWard) {
  selectedWard.value = ward
  isUnpairConfirmOpen.value = true
}

async function unpair() {
  if (!selectedWard.value) return

  const wardName = selectedWard.value.name
  try {
    await unpairMutation.mutateAsync(selectedWard.value.wardId)
    isUnpairConfirmOpen.value = false
    selectedWard.value = null
    toastMessage.value = `${wardName}님과의 연결을 해제했어요.`
    isToastOpen.value = true
  } catch (error) {
    isUnpairConfirmOpen.value = false
    toastMessage.value = await getApiErrorMessage(
      error,
      '연결을 해제하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
    isToastOpen.value = true
  }
}
</script>

<template>
  <main class="min-h-screen bg-white">
    <GuardMyHeader
      title="시니어 관리"
      show-back
      @back="router.push({ name: 'guard-my' })"
    />

    <section
      v-if="guardHomeQuery.isPending.value"
      class="flex min-h-[calc(100dvh-44px)] items-center justify-center px-mobile-gutter text-[16px] font-medium text-gray-600"
    >
      연결 정보를 불러오는 중이에요.
    </section>

    <section
      v-else-if="guardHomeQuery.isError.value"
      class="flex min-h-[calc(100dvh-44px)] items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-error"
    >
      연결 정보를 불러오지 못했어요.
    </section>

    <section
      v-else-if="wards.length === 0"
      class="flex min-h-[calc(100dvh-44px)] flex-col items-center justify-center px-mobile-gutter text-center"
    >
      <p
        class="text-[16px] font-medium leading-6 tracking-[-0.2px] text-gray-600"
      >
        연결된 시니어가 없어요.
      </p>
      <Button
        class="mt-lg w-full"
        label="시니어 연결하기"
        variant="guard-cta"
        size="guard-cta"
        @click="router.push({ name: 'guard-home' })"
      />
    </section>

    <section
      v-else
      class="px-mobile-gutter pb-xl pt-xl"
      aria-label="연결된 시니어"
    >
      <ul class="space-y-sm">
        <li
          v-for="ward in wards"
          :key="ward.wardId"
          class="flex items-center gap-md rounded-large bg-[#f0f3f8] p-md"
        >
          <img
            :src="`/images/avatar/avatar${ward.avatarId}.png`"
            :alt="`${ward.name} 프로필`"
            class="size-14 shrink-0 rounded-full object-cover"
          />
          <div class="min-w-0 flex-1">
            <p
              class="truncate text-[18px] font-semibold leading-[1.2] tracking-[-0.36px] text-[#232529]"
            >
              {{ ward.name }}
            </p>
            <p class="mt-1 text-[14px] font-medium text-gray-600">
              연결된 시니어
            </p>
          </div>
          <Button
            class="!min-h-10 shrink-0 !border !px-sm text-[14px]"
            label="연결 해제"
            variant="outline-danger"
            size="small"
            @click="openUnpairConfirm(ward)"
          />
        </li>
      </ul>
    </section>

    <ConfirmModal
      v-model:open="isUnpairConfirmOpen"
      :title="confirmTitle"
      confirm-variant="danger"
      :confirm-disabled="unpairMutation.isPending.value"
      @confirm="unpair"
    />
    <Toast v-model:open="isToastOpen" :message="toastMessage" />
  </main>
</template>
