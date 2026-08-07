<script setup lang="ts">
import { PhPlus } from '@phosphor-icons/vue'

import type { GuardSeniorAvatar } from '@/mocks/guard-home.mock'

defineProps<{
  seniors: GuardSeniorAvatar[]
  activeSeniorId: string
}>()

const emit = defineEmits<{
  add: []
  select: [seniorId: string]
}>()
</script>

<template>
  <div class="flex items-start gap-md">
    <button
      v-for="senior in seniors"
      :key="senior.id"
      class="flex w-16 flex-col items-center gap-xxs"
      type="button"
      :aria-label="
        senior.hasPending ? `${senior.name} 이상 거래 있음` : senior.name
      "
      @click="emit('select', senior.id)"
    >
      <span
        class="flex size-16 items-center justify-center overflow-hidden rounded-full bg-gray-900 text-primary-500"
        :class="
          senior.id === activeSeniorId
            ? 'border-2 border-primary-500'
            : senior.hasPending
              ? 'border-2 border-error'
              : 'border-0'
        "
      >
        <img
          v-if="senior.imageUrl"
          class="size-full object-cover"
          :src="senior.imageUrl"
          :alt="`${senior.name} 프로필`"
        />
        <span v-else class="type-h3">{{ senior.name.slice(0, 1) }}</span>
      </span>
      <span
        class="type-caption font-medium"
        :class="
          senior.id === activeSeniorId ? 'text-primary-500' : 'text-gray-700'
        "
      >
        {{ senior.name }}
      </span>
    </button>

    <button
      class="flex size-16 items-center justify-center rounded-full border-2 border-primary-500 text-primary-500"
      type="button"
      aria-label="시니어 추가"
      @click="emit('add')"
    >
      <PhPlus class="size-6" aria-hidden="true" />
    </button>
  </div>
</template>
