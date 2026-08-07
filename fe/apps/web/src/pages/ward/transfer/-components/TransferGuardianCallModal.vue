<script setup lang="ts">
import { Phone } from '@lucide/vue'
import { Button, Modal } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'

import { wardGuardianQueryOptions } from '@/lib/query/ward/guardian'

defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

const wardGuardianQuery = useQuery(wardGuardianQueryOptions())

const guardian = computed(() => wardGuardianQuery.data.value)

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

const guardianName = computed(() => guardian.value?.name || '보호자')
const guardianPhone = computed(() => guardian.value?.phone || '')

const modalDescription = computed(() => {
  if (guardianPhone.value) {
    return `${guardianName.value}님의 연결된 번호\n${formatPhoneNumber(guardianPhone.value)}(으)로 전화를 겁니다.`
  }
  return '연결된 보호자의 번호로 전화를 겁니다.'
})

function callGuardian() {
  const digits = guardianPhone.value.replace(/\D/g, '')
  if (!digits) return
  globalThis.open(`tel:${digits}`, '_self')
  emit('update:open', false)
}
</script>

<template>
  <Modal
    :open="open"
    title="보호자에게 전화할까요?"
    :description="modalDescription"
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
          :disabled="!guardianPhone"
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
