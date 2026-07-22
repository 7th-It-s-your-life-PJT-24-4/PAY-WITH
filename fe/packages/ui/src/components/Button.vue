<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    label: string
    variant?:
      | 'primary'
      | 'secondary'
      | 'outline-primary'
      | 'danger'
      | 'outline-danger'
      | 'text'
    size?: 'small' | 'default' | 'large'
    pill?: boolean
    disabled?: boolean
    type?: 'button' | 'submit' | 'reset'
  }>(),
  {
    variant: 'primary',
    size: 'default',
    pill: false,
    disabled: false,
    type: 'button',
  },
)

const variantClass = computed(
  () =>
    ({
      primary:
        'border-action bg-action text-on-action hover:bg-action-active active:bg-action-active',
      secondary:
        'border-disabled bg-disabled text-body hover:border-border-strong',
      'outline-primary':
        'border-primary-500 bg-surface-card text-primary-500 hover:bg-primary-900',
      danger:
        'border-error bg-error text-on-semantic hover:brightness-95 active:brightness-90',
      'outline-danger':
        'border-error bg-surface-card text-error hover:bg-error/10',
      text: 'border-transparent bg-transparent text-primary-300 hover:bg-primary-900',
    })[props.variant],
)

const sizeClass = computed(() => {
  if (props.variant === 'text')
    return 'type-body-medium min-h-touch-target px-sm'

  return {
    small: 'type-h4 min-h-button-small px-xl',
    default: 'type-h4 min-h-button-default px-xl',
    large: 'type-h1 min-h-button-large px-xxl',
  }[props.size]
})

const contentGapClass = computed(() =>
  props.size === 'large' ? 'gap-md' : 'gap-xs',
)

const iconSizeClass = computed(() => {
  if (props.variant === 'text') return 'size-lg'

  return {
    small: 'size-lg',
    default: 'size-xl',
    large: 'size-xxl',
  }[props.size]
})
</script>

<template>
  <button
    class="inline-flex items-center justify-center border-2 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus disabled:cursor-not-allowed disabled:border-disabled disabled:bg-disabled disabled:text-on-disabled disabled:opacity-[var(--opacity-disabled)]"
    :class="[
      variantClass,
      sizeClass,
      contentGapClass,
      pill
        ? 'rounded-full'
        : size === 'large'
          ? 'rounded-large'
          : 'rounded-medium',
      size === 'large' && variant.startsWith('outline') ? 'border-[3px]' : '',
    ]"
    :disabled="disabled"
    :type="type"
  >
    <span
      v-if="$slots.leading"
      class="inline-flex shrink-0 items-center justify-center [&>img]:size-full [&>svg]:size-full"
      :class="iconSizeClass"
      aria-hidden="true"
    >
      <slot name="leading" />
    </span>
    <span>{{ label }}</span>
    <span
      v-if="$slots.trailing"
      class="inline-flex shrink-0 items-center justify-center [&>img]:size-full [&>svg]:size-full"
      :class="iconSizeClass"
      aria-hidden="true"
    >
      <slot name="trailing" />
    </span>
  </button>
</template>
