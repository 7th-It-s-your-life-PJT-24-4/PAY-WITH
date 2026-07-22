<script setup lang="ts">
import { Button } from '@pay-with/ui'

withDefaults(
  defineProps<{
    balance: string
    locked?: boolean
  }>(),
  {
    locked: false,
  },
)

const emit = defineEmits<{
  contactGuardian: []
}>()
</script>

<template>
  <section
    class="relative overflow-hidden rounded-large border border-border p-xl shadow-card"
    :class="locked ? 'bg-error/10' : 'bg-surface-card'"
    aria-labelledby="ward-balance-title"
  >
    <div
      class="pointer-events-none absolute -right-12 -top-12 size-48 rounded-full blur-3xl"
      :class="locked ? 'bg-error/10' : 'bg-primary-500/10'"
    />
    <div class="relative flex flex-col gap-sm">
      <h2 id="ward-balance-title" class="type-h4 text-primary-300">
        나의 잔액
      </h2>
      <p class="flex items-baseline gap-xs text-body">
        <strong class="type-amount">{{ balance }}</strong>
        <span class="type-h2 font-medium">원</span>
      </p>

      <template v-if="locked">
        <p class="type-h1 flex items-center gap-xs text-error" role="status">
          <svg
            class="size-[38px] shrink-0"
            viewBox="0 0 38 38"
            fill="none"
            aria-hidden="true"
          >
            <path
              d="M12.5 16V12.5a6.5 6.5 0 0 1 13 0V16m-15 0h17a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-17a2 2 0 0 1-2-2V18a2 2 0 0 1 2-2Z"
              stroke="currentColor"
              stroke-width="3"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <circle cx="19" cy="25" r="2" fill="currentColor" />
          </svg>
          <strong>지갑 잠김</strong>
        </p>

        <p class="type-h4 text-primary-300">
          안전을 위해 지갑이 잠금처리되었습니다.
        </p>

        <Button
          class="w-full"
          label="보호자에게 연락하기"
          variant="outline-primary"
          size="large"
          @click="emit('contactGuardian')"
        >
          <template #leading>
            <svg viewBox="0 0 32 32" fill="none">
              <path
                d="M7.5 4.5h5l2 7-3.25 2a20.5 20.5 0 0 0 7.25 7.25l2-3.25 7 2v5A3.5 3.5 0 0 1 24 28C12.95 28 4 19.05 4 8a3.5 3.5 0 0 1 3.5-3.5Z"
                fill="currentColor"
              />
            </svg>
          </template>
        </Button>
      </template>
    </div>
  </section>
</template>
