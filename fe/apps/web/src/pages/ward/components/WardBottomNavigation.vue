<script setup lang="ts">
import { BottomNavigation } from '@pay-with/ui'

type WardNavigationValue = 'transfer' | 'payment' | 'history'

withDefaults(
  defineProps<{
    active?: WardNavigationValue
  }>(),
  {
    active: 'payment',
  },
)

const items: Array<{
  label: string
  value: WardNavigationValue
}> = [
  { label: '송금', value: 'transfer' },
  { label: '결제', value: 'payment' },
  { label: '내역', value: 'history' },
]

const emit = defineEmits<{
  navigate: [value: WardNavigationValue]
}>()

function handleNavigate(value: string) {
  emit('navigate', value as WardNavigationValue)
}
</script>

<template>
  <div
    class="fixed inset-x-0 bottom-0 z-40 mx-auto w-full max-w-[390px] bg-surface-card pb-[env(safe-area-inset-bottom)]"
  >
    <BottomNavigation
      :items="items"
      :active="active"
      variant="ward"
      center-action-value="payment"
      aria-label="시니어 주요 기능"
      @navigate="handleNavigate"
    />
  </div>
</template>
