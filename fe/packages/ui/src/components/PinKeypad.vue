<script setup lang="ts">
import { Delete } from '@lucide/vue'
import { computed, onBeforeUnmount, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    length?: number
    randomize?: boolean
    randomizeOnInput?: boolean
    pseudoClick?: boolean
    activeDuration?: number
    disabled?: boolean
    error?: string
    cancelLabel?: string
    variant?: 'card' | 'minimal'
    keyOrder?: string[]
  }>(),
  {
    length: 6,
    randomize: true,
    randomizeOnInput: false,
    pseudoClick: true,
    activeDuration: 180,
    disabled: false,
    error: undefined,
    cancelLabel: '',
    variant: 'card',
    keyOrder: undefined,
  },
)

const emit = defineEmits<{
  complete: [pin: string]
  change: [length: number]
  cancel: []
}>()

const defaultKeys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0']

function shuffleKeys() {
  const nextKeys = [...defaultKeys]
  for (let index = nextKeys.length - 1; index > 0; index -= 1) {
    const target = Math.floor(Math.random() * (index + 1))
    ;[nextKeys[index], nextKeys[target]] = [nextKeys[target]!, nextKeys[index]!]
  }
  return nextKeys
}

const keys = ref(
  props.keyOrder && props.keyOrder.length === defaultKeys.length
    ? [...props.keyOrder]
    : props.randomize
      ? shuffleKeys()
      : [...defaultKeys],
)
const pin = ref('')
const activeKey = ref<string | null>(null)
const pseudoActiveKey = ref<string | null>(null)
let activeTimer: ReturnType<typeof setTimeout> | undefined

const filledCount = computed(() => Math.min(pin.value.length, props.length))

function clearActiveState() {
  if (activeTimer) clearTimeout(activeTimer)
  activeTimer = undefined
  activeKey.value = null
  pseudoActiveKey.value = null
}

function showClickFeedback(value: string) {
  clearActiveState()
  activeKey.value = value

  if (props.pseudoClick) {
    const candidates = keys.value.filter((key) => key !== value)
    pseudoActiveKey.value =
      candidates[Math.floor(Math.random() * candidates.length)] ?? null
  }

  activeTimer = setTimeout(clearActiveState, props.activeDuration)
}

function input(value: string) {
  if (props.disabled || pin.value.length >= props.length) return

  showClickFeedback(value)
  pin.value += value
  emit('change', pin.value.length)

  if (props.randomizeOnInput) keys.value = shuffleKeys()
  if (pin.value.length === props.length) emit('complete', pin.value)
}

function backspace() {
  if (props.disabled || pin.value.length === 0) return

  pin.value = pin.value.slice(0, -1)
  emit('change', pin.value.length)
}

function reset() {
  clearActiveState()
  pin.value = ''
  if (props.keyOrder && props.keyOrder.length === defaultKeys.length) {
    keys.value = [...props.keyOrder]
  } else if (props.randomize) {
    keys.value = shuffleKeys()
  }
  emit('change', 0)
}

function cancel() {
  if (props.disabled) return
  reset()
  emit('cancel')
}

defineExpose({ reset })
onBeforeUnmount(clearActiveState)
</script>

<template>
  <div class="flex w-full flex-col items-center">
    <div
      class="flex justify-center gap-md"
      role="status"
      :aria-label="`비밀번호 ${filledCount}자리 입력됨`"
    >
      <span
        v-for="index in length"
        :key="index"
        class="size-md rounded-full"
        :class="index <= filledCount ? 'bg-primary-500' : 'bg-gray-800'"
      />
    </div>

    <p
      v-if="error"
      class="type-body-medium mt-md text-center text-error"
      role="alert"
    >
      {{ error }}
    </p>

    <div
      class="grid w-full max-w-[350px] grid-cols-3"
      :class="
        variant === 'minimal'
          ? 'mt-[210px] gap-x-[44px] gap-y-[20px]'
          : 'mt-xxl gap-md'
      "
      role="group"
      aria-label="비밀번호 숫자 키패드"
    >
      <button
        v-for="key in keys.slice(0, 9)"
        :key="key"
        class="flex items-center justify-center outline-none focus-visible:ring-2 focus-visible:ring-focus disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
        :class="
          variant === 'minimal'
            ? [
                'h-[50px] text-[24px] font-bold leading-[1.6] tracking-[-0.48px] text-primary-500',
              ]
            : [
                'type-numeric-input h-16 rounded-medium border border-border bg-surface-card text-body shadow-card transition-colors',
                activeKey === key || pseudoActiveKey === key
                  ? 'border-primary-500 bg-primary-900 text-primary-300'
                  : '',
              ]
        "
        type="button"
        :disabled="disabled"
        :aria-label="key"
        :data-feedback="
          activeKey === key
            ? 'active'
            : pseudoActiveKey === key
              ? 'pseudo'
              : undefined
        "
        @click="input(key)"
      >
        <span
          v-if="variant === 'minimal'"
          class="flex size-11 items-center justify-center rounded-full transition-colors"
          :class="
            activeKey === key || pseudoActiveKey === key ? 'bg-primary-900' : ''
          "
        >
          {{ key }}
        </span>
        <template v-else>{{ key }}</template>
      </button>

      <button
        class="flex items-center justify-center outline-none focus-visible:ring-2 focus-visible:ring-focus disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
        :class="
          variant === 'minimal'
            ? 'h-[50px] rounded-full text-[14px] font-semibold text-gray-600'
            : 'type-h4 h-16 rounded-medium bg-disabled/60 text-body-secondary'
        "
        type="button"
        :disabled="disabled"
        aria-label="비밀번호 입력 취소"
        @click="cancel"
      >
        {{ cancelLabel }}
      </button>

      <button
        class="flex items-center justify-center outline-none focus-visible:ring-2 focus-visible:ring-focus disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
        :class="
          variant === 'minimal'
            ? [
                'h-[50px] text-[24px] font-bold leading-[1.6] tracking-[-0.48px] text-primary-500',
              ]
            : [
                'type-numeric-input h-16 rounded-medium border border-border bg-surface-card text-body shadow-card transition-colors',
                activeKey === keys[9] || pseudoActiveKey === keys[9]
                  ? 'border-primary-500 bg-primary-900 text-primary-300'
                  : '',
              ]
        "
        type="button"
        :disabled="disabled"
        :aria-label="keys[9]"
        :data-feedback="
          activeKey === keys[9]
            ? 'active'
            : pseudoActiveKey === keys[9]
              ? 'pseudo'
              : undefined
        "
        @click="input(keys[9]!)"
      >
        <span
          v-if="variant === 'minimal'"
          class="flex size-11 items-center justify-center rounded-full transition-colors"
          :class="
            activeKey === keys[9] || pseudoActiveKey === keys[9]
              ? 'bg-primary-900'
              : ''
          "
        >
          {{ keys[9] }}
        </span>
        <template v-else>{{ keys[9] }}</template>
      </button>

      <button
        class="flex items-center justify-center outline-none focus-visible:ring-2 focus-visible:ring-focus disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
        :class="
          variant === 'minimal'
            ? 'h-[50px] rounded-full text-gray-600'
            : 'h-16 rounded-medium bg-disabled/60 text-body-secondary'
        "
        type="button"
        :disabled="disabled"
        aria-label="한 글자 지우기"
        @click="backspace"
      >
        <Delete class="size-6" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>
