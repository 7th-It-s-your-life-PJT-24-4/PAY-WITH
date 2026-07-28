<script setup lang="ts">
import { Check, ChevronDown, ChevronsUp, Landmark } from '@lucide/vue'
import {
  SelectContent,
  SelectItem,
  SelectItemIndicator,
  SelectItemText,
  SelectPortal,
  SelectRoot,
  SelectTrigger,
  SelectValue,
  SelectViewport,
} from 'reka-ui'
import { computed, ref } from 'vue'

import ChargeBankMark from '@/pages/ward/charge/-components/ChargeBankMark.vue'

export interface ChargeBankOption {
  code: string
  name: string
}

interface ScrollViewport {
  scrollTop: number
  scrollHeight: number
  clientHeight: number
}

const props = defineProps<{
  modelValue: string
  banks: ChargeBankOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const selectedBank = computed(() =>
  props.banks.find(({ code }) => code === props.modelValue),
)
const showScrollHint = ref(true)

function handleOpenChange(open: boolean) {
  if (open) showScrollHint.value = true
}

function handleViewportScroll(event: { currentTarget: unknown }) {
  const viewport = event.currentTarget as ScrollViewport
  const remaining =
    viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight
  showScrollHint.value = remaining > 2
}
</script>

<template>
  <SelectRoot
    :model-value="modelValue"
    @update:open="handleOpenChange"
    @update:model-value="
      emit('update:modelValue', typeof $event === 'string' ? $event : '')
    "
  >
    <SelectTrigger
      class="group flex h-[72px] w-full items-center gap-md rounded-medium border border-border-strong bg-surface-card px-md text-left shadow-card outline-none transition-colors hover:border-primary-500 focus-visible:border-focus focus-visible:ring-2 focus-visible:ring-focus/20 data-[state=open]:border-primary-500 data-[state=open]:ring-2 data-[state=open]:ring-primary-500/15"
      aria-label="은행 선택"
    >
      <SelectValue class="min-w-0 flex-1" placeholder="은행을 선택해주세요">
        <span v-if="selectedBank" class="flex min-w-0 items-center gap-md">
          <ChargeBankMark :bank-code="selectedBank.code" />
          <span class="type-h3 truncate text-body">
            {{ selectedBank.name }}
          </span>
        </span>
        <span
          v-else
          class="type-h3 flex min-w-0 items-center gap-md text-body-muted"
        >
          <span
            class="flex size-12 shrink-0 items-center justify-center rounded-full bg-disabled/60 text-primary-500"
            aria-hidden="true"
          >
            <Landmark class="size-xl" :stroke-width="2.25" />
          </span>
          은행을 선택해주세요
        </span>
      </SelectValue>
      <ChevronDown
        class="size-xl shrink-0 text-body-secondary transition-transform group-data-[state=open]:rotate-180"
        aria-hidden="true"
      />
    </SelectTrigger>

    <SelectPortal>
      <SelectContent
        class="z-50 w-[var(--reka-select-trigger-width)] overflow-hidden rounded-large border border-border bg-surface-card shadow-modal"
        position="popper"
        :side-offset="8"
        align="start"
      >
        <SelectViewport
          class="max-h-[240px] p-sm"
          @scroll.passive="handleViewportScroll"
        >
          <SelectItem
            v-for="bank in banks"
            :key="bank.code"
            class="group/item relative flex min-h-16 cursor-pointer select-none items-center gap-md rounded-medium px-md py-sm outline-none data-[highlighted]:bg-primary-900 data-[state=checked]:text-primary-500"
            :value="bank.code"
          >
            <ChargeBankMark :bank-code="bank.code" />
            <SelectItemText class="type-h3 min-w-0 flex-1 truncate">
              {{ bank.name }}
            </SelectItemText>
            <SelectItemIndicator
              class="flex size-xl shrink-0 items-center justify-center text-primary-500"
            >
              <Check class="size-xl" :stroke-width="2.5" aria-hidden="true" />
            </SelectItemIndicator>
          </SelectItem>
        </SelectViewport>

        <div
          v-if="showScrollHint"
          class="pointer-events-none flex min-h-touch-target select-none items-center justify-center gap-sm border-t border-primary-500/20 bg-primary-900 px-md text-primary-500"
          role="note"
        >
          <ChevronsUp class="size-xl shrink-0" aria-hidden="true" />
          <span class="type-body-medium">
            목록을 위로 밀어 더 많은 은행 보기
          </span>
        </div>
      </SelectContent>
    </SelectPortal>
  </SelectRoot>
</template>
