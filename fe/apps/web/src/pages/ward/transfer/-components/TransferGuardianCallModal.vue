<script setup lang="ts">
import { Phone } from '@lucide/vue'
import { Button, Modal } from '@pay-with/ui'

import { mockGuardian } from '@/mocks/guardian.mock'

defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

function callGuardian() {
  const phoneNumber = mockGuardian.phoneNumber.replaceAll('-', '')
  globalThis.open(`tel:${phoneNumber}`, '_self')
  emit('update:open', false)
}
</script>

<template>
  <Modal
    :open="open"
    title="보호자에게 전화할까요?"
    :description="`${mockGuardian.name}의 연결된 번호\n${mockGuardian.phoneNumber}(으)로 전화를 겁니다.`"
    size="large"
    @update:open="emit('update:open', $event)"
  >
    <template #icon>
      <Phone class="size-xxl" :stroke-width="2.25" aria-hidden="true" />
    </template>

    <template #actions="{ close }">
      <div class="flex flex-col gap-md">
        <Button
          class="w-full"
          label="전화 걸기"
          size="large"
          pill
          @click="callGuardian"
        />
        <Button
          class="w-full"
          label="취소"
          variant="outline-primary"
          size="large"
          pill
          @click="close"
        />
      </div>
    </template>
  </Modal>
</template>
