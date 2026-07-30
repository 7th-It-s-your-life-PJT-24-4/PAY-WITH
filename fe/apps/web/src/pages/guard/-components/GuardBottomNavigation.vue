<script setup lang="ts">
import { PhHouse, PhReceipt, PhUser, PhWallet } from '@phosphor-icons/vue'
import { BottomNavigation } from '@pay-with/ui'
import type { Component } from 'vue'

type GuardNavigationValue = 'home' | 'charge' | 'history' | 'my'

export type { GuardNavigationValue }

withDefaults(
  defineProps<{
    active?: GuardNavigationValue
  }>(),
  {
    active: 'home',
  },
)

const emit = defineEmits<{
  navigate: [value: GuardNavigationValue]
}>()

const items: Array<{
  label: string
  value: GuardNavigationValue
  icon: Component
}> = [
  { label: '홈', value: 'home', icon: PhHouse },
  { label: '충전', value: 'charge', icon: PhWallet },
  { label: '내역', value: 'history', icon: PhReceipt },
  { label: '마이', value: 'my', icon: PhUser },
]

function handleNavigate(value: string) {
  emit('navigate', value as GuardNavigationValue)
}
</script>

<template>
  <div
    class="fixed inset-x-0 bottom-0 z-40 mx-auto w-full max-w-[390px] bg-surface-card pb-[env(safe-area-inset-bottom)]"
  >
    <BottomNavigation
      :items="items"
      :active="active"
      variant="guard"
      aria-label="보호자 주요 기능"
      @navigate="handleNavigate"
    >
      <template #icon="{ item, active: isActive }">
        <component
          :is="item.icon"
          class="size-6"
          :class="[
            isActive ? 'text-primary-500' : 'text-gray-700',
            '[&_*]:stroke-current [&_*]:fill-current',
          ]"
          :weight="isActive ? 'fill' : 'regular'"
          :color="
            isActive ? 'var(--color-primary-500)' : 'var(--color-gray-700)'
          "
          :style="{
            color: isActive
              ? 'var(--color-primary-500)'
              : 'var(--color-gray-700)',
          }"
          aria-hidden="true"
        />
      </template>
    </BottomNavigation>
  </div>
</template>
