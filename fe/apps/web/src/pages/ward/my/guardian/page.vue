<script setup lang="ts">
import { Phone, ShieldUser, TriangleAlert } from '@lucide/vue'
import { Button, Modal } from '@pay-with/ui'
import { ref } from 'vue'

import { mockGuardian } from '@/mocks/guardian.mock'

const isUnpairModalOpen = ref(false)

const guardianItems = [
  { label: '이름', value: mockGuardian.name },
  { label: '연락처', value: mockGuardian.phoneNumber },
] as const
</script>

<template>
  <section class="flex flex-1 flex-col" aria-label="보호자 관리">
    <div
      class="rounded-[20px] border-2 border-primary-500 bg-white px-lg py-xl text-center shadow-[0_8px_24px_rgb(0_106_126/10%)]"
    >
      <div
        class="mx-auto flex size-[96px] items-center justify-center rounded-full bg-primary-900 text-primary-500"
        aria-hidden="true"
      >
        <ShieldUser class="size-12" :stroke-width="2.2" />
      </div>

      <p class="mt-lg text-[24px] font-bold leading-[1.2] text-[#111827]">
        {{ mockGuardian.name }}님
      </p>
      <p class="mt-xs text-[18px] font-bold leading-[1.3] text-primary-300">
        현재 연결된 보호자
      </p>
    </div>

    <dl class="mt-xl space-y-md">
      <div
        v-for="item in guardianItems"
        :key="item.label"
        class="flex min-h-[72px] items-center justify-between gap-lg rounded-[16px] border-2 border-primary-500 bg-white px-lg"
      >
        <dt
          class="shrink-0 text-[22px] font-bold leading-[1.2] text-primary-300"
        >
          {{ item.label }}
        </dt>
        <dd
          class="min-w-0 truncate text-right text-[22px] font-bold leading-[1.2] text-[#111827]"
        >
          {{ item.value }}
        </dd>
      </div>
    </dl>

    <Button
      class="mt-xl w-full"
      label="보호자에게 연락하기"
      variant="outline-primary"
      size="large"
      pill
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
  </section>
</template>
