<script setup lang="ts">
import { computed, useId } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    label: string
    id?: string
    type?: 'text' | 'password'
    inputmode?: 'text' | 'numeric' | 'decimal' | 'email' | 'tel' | 'url'
    autocomplete?: string
    maxlength?: number
    placeholder?: string
    description?: string
    error?: string
    disabled?: boolean
    numeric?: boolean
    large?: boolean
    inputFilter?: RegExp
  }>(),
  {
    modelValue: '',
    id: undefined,
    type: 'text',
    inputmode: 'text',
    autocomplete: undefined,
    maxlength: undefined,
    placeholder: undefined,
    description: undefined,
    error: undefined,
    disabled: false,
    numeric: false,
    large: false,
    inputFilter: undefined,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const generatedId = useId()
const inputId = computed(() => props.id ?? generatedId)
const messageId = computed(() => `${inputId.value}-message`)

function updateValue(event: Event) {
  if (props.inputFilter && (event as InputEvent).isComposing) return

  const target = event.target as HTMLInputElement
  const value = props.inputFilter
    ? target.value.replace(props.inputFilter, '')
    : target.value
  target.value = value
  emit('update:modelValue', value)
}

function handleBeforeInput(event: InputEvent) {
  if (!props.inputFilter || event.isComposing || !event.data) return
  props.inputFilter.lastIndex = 0
  if (props.inputFilter.test(event.data)) event.preventDefault()
}

function handlePaste(event: ClipboardEvent) {
  if (!props.inputFilter) return
  event.preventDefault()
  const value = event.clipboardData?.getData('text') ?? ''
  emit('update:modelValue', value.replace(props.inputFilter, ''))
}

function handleCompositionEnd(event: CompositionEvent) {
  if (!props.inputFilter) return
  const target = event.target as HTMLInputElement
  const value = target.value.replace(props.inputFilter, '')
  target.value = value
  emit('update:modelValue', value)
}
</script>

<template>
  <div class="flex w-full flex-col gap-xs">
    <label :for="inputId" class="type-h4 text-body">
      {{ label }}
    </label>
    <input
      :id="inputId"
      class="w-full rounded-medium border bg-surface-card px-md text-body outline-none transition-colors placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20 disabled:cursor-not-allowed disabled:border-disabled disabled:bg-disabled/40 disabled:text-on-disabled"
      :class="[
        large ? 'h-[72px]' : 'h-[52px]',
        numeric
          ? large
            ? 'type-numeric-input-large'
            : 'type-numeric-input'
          : large
            ? 'type-h3'
            : 'text-[16px] font-normal leading-[1.2] tracking-[-0.32px]',
        error
          ? 'border-error focus:border-error focus:ring-error/20'
          : 'border-border-strong',
      ]"
      :type="type"
      :inputmode="inputmode"
      :autocomplete="autocomplete"
      :maxlength="maxlength"
      :placeholder="placeholder"
      :value="modelValue"
      :disabled="disabled"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="description || error ? messageId : undefined"
      @input="updateValue"
      @beforeinput="handleBeforeInput"
      @paste="handlePaste"
      @compositionend="handleCompositionEnd"
    />
    <p
      v-if="description || error"
      :id="messageId"
      class="type-caption"
      :class="error ? 'text-error' : 'text-body-secondary'"
    >
      {{ error ?? description }}
    </p>
  </div>
</template>
