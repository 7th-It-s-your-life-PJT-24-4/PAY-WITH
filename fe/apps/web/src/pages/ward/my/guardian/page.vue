<script setup lang="ts">
import {
  Loader2,
  Phone,
  ShieldUser,
  TriangleAlert,
  UserPlus,
} from '@lucide/vue'
import { Button, Modal } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorCode } from '@/api/error'
import { wardGuardianQueryOptions } from '@/lib/query/ward/guardian'

const router = useRouter()
const wardGuardianQuery = useQuery(wardGuardianQueryOptions())

const isUnpairModalOpen = ref(false)

const guardian = computed(() => wardGuardianQuery.data.value)
const isUnpairedError = computed(() => {
  if (!wardGuardianQuery.isError.value) return false
  return getApiErrorCode(wardGuardianQuery.error.value) === 'WARD_001'
})

function formatPhoneNumber(phone: string): string {
  const digits = phone.replace(/\D/g, '')
  if (digits.length === 11) {
    return digits.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3')
  }
  if (digits.length === 10) {
    return digits.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3')
  }
  return phone
}

function callGuardian() {
  if (!guardian.value?.phone) return
  window.location.href = `tel:${guardian.value.phone}`
}

function goToPairing() {
  router.push({ name: 'ward-pairing' })
}
</script>

<template>
  <section class="flex flex-1 flex-col" aria-label="보호자 관리">
    <!-- 로딩 상태 -->
    <div
      v-if="wardGuardianQuery.isPending.value"
      class="flex flex-1 flex-col items-center justify-center py-2xl"
    >
      <Loader2 class="size-12 animate-spin text-primary-500" />
      <p class="mt-md text-[18px] font-bold text-gray-600">
        보호자 정보를 불러오고 있습니다...
      </p>
    </div>

    <!-- 미연동 상태 또는 WARD_001 에러 -->
    <div
      v-else-if="
        isUnpairedError ||
        (!guardian &&
          !wardGuardianQuery.isPending.value &&
          !wardGuardianQuery.isError.value)
      "
      class="flex flex-1 flex-col items-center justify-center px-lg py-2xl text-center"
    >
      <div
        class="flex size-[96px] items-center justify-center rounded-full bg-gray-100 text-gray-400"
        aria-hidden="true"
      >
        <ShieldUser class="size-12" :stroke-width="2.2" />
      </div>
      <h2 class="mt-lg text-[24px] font-bold text-[#111827]">
        연결된 보호자가 없습니다
      </h2>
      <p class="mt-xs text-[18px] font-bold text-gray-500">
        서비스를 안전하게 이용하려면 보호자와 연결해 주세요.
      </p>
      <Button
        class="mt-xl w-full"
        label="보호자 연결하기"
        variant="primary"
        size="large"
        pill
        @click="goToPairing"
      >
        <template #leading>
          <UserPlus class="size-7" :stroke-width="2.2" aria-hidden="true" />
        </template>
      </Button>
    </div>

    <!-- 기타 서버 에러 발생 시 -->
    <div
      v-else-if="wardGuardianQuery.isError.value"
      class="flex flex-1 flex-col items-center justify-center px-lg py-2xl text-center"
    >
      <div
        class="flex size-[96px] items-center justify-center rounded-full bg-red-50 text-red-500"
        aria-hidden="true"
      >
        <TriangleAlert class="size-12" :stroke-width="2.2" />
      </div>
      <h2 class="mt-lg text-[24px] font-bold text-[#111827]">
        보호자 정보를 불러오지 못했습니다
      </h2>
      <p class="mt-xs text-[18px] font-bold text-gray-500">
        잠시 후 다시 시도해 주세요.
      </p>
      <Button
        class="mt-xl w-full"
        label="다시 시도"
        variant="outline-primary"
        size="large"
        pill
        @click="wardGuardianQuery.refetch()"
      />
    </div>

    <!-- 보호자 연동 성공 상태 -->
    <div v-else-if="guardian" class="flex flex-1 flex-col">
      <div
        class="rounded-[20px] border-2 border-primary-500 bg-white px-lg py-xl text-center shadow-[0_8px_24px_rgb(0_106_126/10%)]"
      >
        <div
          class="mx-auto flex size-[96px] items-center justify-center overflow-hidden rounded-full bg-primary-900 text-primary-500"
          aria-hidden="true"
        >
          <img
            v-if="
              guardian.avatarId &&
              guardian.avatarId >= 1 &&
              guardian.avatarId <= 6
            "
            :src="`/images/avatar/avatar${guardian.avatarId}.png`"
            :alt="`${guardian.name}님의 아바타`"
            class="size-full object-cover"
          />
          <ShieldUser v-else class="size-12" :stroke-width="2.2" />
        </div>

        <p class="mt-lg text-[24px] font-bold leading-[1.2] text-[#111827]">
          {{ guardian.name }}님
        </p>
        <p class="mt-xs text-[18px] font-bold leading-[1.3] text-primary-300">
          현재 연결된 보호자
        </p>
      </div>

      <dl class="mt-xl space-y-md">
        <div
          class="flex min-h-[72px] items-center justify-between gap-lg rounded-[16px] border-2 border-primary-500 bg-white px-lg"
        >
          <dt
            class="shrink-0 text-[22px] font-bold leading-[1.2] text-primary-300"
          >
            이름
          </dt>
          <dd
            class="min-w-0 truncate text-right text-[22px] font-bold leading-[1.2] text-[#111827]"
          >
            {{ guardian.name }}
          </dd>
        </div>
        <div
          class="flex min-h-[72px] items-center justify-between gap-lg rounded-[16px] border-2 border-primary-500 bg-white px-lg"
        >
          <dt
            class="shrink-0 text-[22px] font-bold leading-[1.2] text-primary-300"
          >
            연락처
          </dt>
          <dd
            class="min-w-0 truncate text-right text-[22px] font-bold leading-[1.2] text-[#111827]"
          >
            {{ formatPhoneNumber(guardian.phone) }}
          </dd>
        </div>
      </dl>

      <Button
        class="mt-xl w-full"
        label="보호자에게 연락하기"
        variant="outline-primary"
        size="large"
        pill
        @click="callGuardian"
      >
        <template #leading>
          <Phone class="size-7" :stroke-width="2.2" aria-hidden="true" />
        </template>
      </Button>

      <Button
        class="mt-md w-full"
        label="연결 해제"
        variant="outline-danger"
        size="large"
        pill
        @click="isUnpairModalOpen = true"
      />

      <Modal
        :open="isUnpairModalOpen"
        title="해제 불가"
        description="보호자만 해제할 수 있습니다."
        size="large"
        icon-tone="error"
        @update:open="isUnpairModalOpen = $event"
      >
        <template #icon>
          <TriangleAlert
            class="size-xxl"
            :stroke-width="2.25"
            aria-hidden="true"
          />
        </template>

        <template #actions="{ close }">
          <Button
            class="w-full"
            label="확인"
            variant="outline-primary"
            size="large"
            pill
            @click="close"
          />
        </template>
      </Modal>
    </div>
  </section>
</template>
