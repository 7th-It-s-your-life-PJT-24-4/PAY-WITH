<script setup lang="ts">
import { TabsList, TabsRoot, TabsTrigger } from 'reka-ui'
import { computed, ref, watch } from 'vue'

interface TabItem {
  value: string
  label: string
  disabled?: boolean
}

const props = defineProps<{
  items: TabItem[]
  modelValue?: string
  ariaLabel: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const internalValue = ref(props.modelValue ?? props.items[0]?.value ?? '')
const selectedValue = computed(() => props.modelValue ?? internalValue.value)

watch(
  () => props.modelValue,
  (value) => {
    if (value !== undefined) internalValue.value = value
  },
)

function updateSelectedValue(value: string | number) {
  internalValue.value = String(value)
  emit('update:modelValue', internalValue.value)
}
</script>

<template>
  <TabsRoot
    :model-value="selectedValue"
    @update:model-value="updateSelectedValue"
  >
    <TabsList
      class="flex max-w-full gap-xs overflow-x-auto"
      :aria-label="ariaLabel"
    >
      <TabsTrigger
        v-for="item in items"
        :key="item.value"
        :value="item.value"
        :disabled="item.disabled"
        class="type-h4 min-h-touch-target shrink-0 rounded-full border border-subbutton bg-surface-card px-md text-body-secondary outline-none transition-colors hover:bg-primary-900 focus-visible:ring-2 focus-visible:ring-focus data-[state=active]:border-action data-[state=active]:bg-action data-[state=active]:text-on-action disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
      >
        {{ item.label }}
      </TabsTrigger>
    </TabsList>
  </TabsRoot>
</template>
