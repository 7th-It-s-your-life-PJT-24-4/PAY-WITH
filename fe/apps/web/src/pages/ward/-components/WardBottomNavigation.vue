<script setup lang="ts">
import { ArrowUpFromLine, Home, QrCode } from '@lucide/vue'
import { BottomNavigation } from '@pay-with/ui'
import { computed } from 'vue'
import type { Component } from 'vue'

type WardNavigationValue = 'transfer' | 'home' | 'payment'

const props = withDefaults(
  defineProps<{
    active?: WardNavigationValue | ''
    isPaired?: boolean
  }>(),
  {
    active: 'home',
    isPaired: true,
  },
)

const items = computed<
  Array<{
    label: string
    value: WardNavigationValue
    icon: Component
    disabled?: boolean
  }>
>(() => [
  {
    label: '송금',
    value: 'transfer',
    icon: ArrowUpFromLine,
    disabled: !props.isPaired,
  },
  { label: '홈', value: 'home', icon: Home },
  {
    label: '결제',
    value: 'payment',
    icon: QrCode,
    disabled: !props.isPaired,
  },
])

const emit = defineEmits<{
  navigate: [value: WardNavigationValue]
}>()

function handleNavigate(value: string) {
  emit('navigate', value as WardNavigationValue)
}
</script>

<template>
  <div
    class="fixed inset-x-0 bottom-0 z-40 mx-auto w-full max-w-[390px] bg-transparent [clip-path:inset(-80px_0_-40px_0)]"
  >
    <BottomNavigation
      :items="items"
      :active="active"
      variant="ward"
      center-action-value="home"
      aria-label="시니어 주요 기능"
      @navigate="handleNavigate"
    />
  </div>
</template>
