<script setup lang="ts">
import { Check, ChevronRight } from '@lucide/vue'
import { CheckboxIndicator, CheckboxRoot } from 'reka-ui'
import { computed } from 'vue'

import { signUpTerms } from '@/constants/sign-up-terms'

const props = defineProps<{
  serviceTerms: boolean
  privacyTerms: boolean
  identifierTerms: boolean
}>()

const emit = defineEmits<{
  'update:serviceTerms': [value: boolean]
  'update:privacyTerms': [value: boolean]
  'update:identifierTerms': [value: boolean]
  beforeNavigate: []
}>()

const allTermsAgreed = computed(
  () => props.serviceTerms && props.privacyTerms && props.identifierTerms,
)

function updateAllTerms(value: boolean | 'indeterminate') {
  const isChecked = value === true

  emit('update:serviceTerms', isChecked)
  emit('update:privacyTerms', isChecked)
  emit('update:identifierTerms', isChecked)
}

function updateTerm(
  termId: keyof typeof signUpTerms,
  value: boolean | 'indeterminate',
) {
  const isChecked = value === true

  if (termId === 'serviceTerms') emit('update:serviceTerms', isChecked)
  if (termId === 'privacyTerms') emit('update:privacyTerms', isChecked)
  if (termId === 'identifierTerms') emit('update:identifierTerms', isChecked)
}
</script>

<template>
  <section
    class="rounded-large border border-border bg-surface-card p-xl shadow-card"
  >
    <label
      class="flex cursor-pointer items-center gap-md border-b border-border pb-md"
    >
      <CheckboxRoot
        :model-value="allTermsAgreed"
        class="flex size-xl shrink-0 items-center justify-center rounded-small border border-border-strong bg-surface-card outline-none data-[state=checked]:border-primary-500 data-[state=checked]:bg-primary-500 focus-visible:ring-2 focus-visible:ring-focus"
        @update:model-value="updateAllTerms"
      >
        <CheckboxIndicator class="text-on-action">
          <Check class="size-lg" :stroke-width="3" />
        </CheckboxIndicator>
      </CheckboxRoot>
      <span class="type-body-medium text-body">약관에 모두 동의합니다</span>
    </label>

    <div class="mt-md flex flex-col gap-md">
      <div
        v-for="(term, termId) in signUpTerms"
        :key="termId"
        class="flex items-center justify-between gap-md"
      >
        <span class="flex items-center gap-md">
          <CheckboxRoot
            :aria-label="term.label"
            :model-value="
              termId === 'serviceTerms'
                ? serviceTerms
                : termId === 'privacyTerms'
                  ? privacyTerms
                  : identifierTerms
            "
            class="flex size-lg shrink-0 items-center justify-center rounded-small border border-border-strong bg-surface-card outline-none data-[state=checked]:border-primary-500 data-[state=checked]:bg-primary-500 focus-visible:ring-2 focus-visible:ring-focus"
            @update:model-value="updateTerm(termId, $event)"
          >
            <CheckboxIndicator class="text-on-action">
              <Check class="size-md" :stroke-width="3" />
            </CheckboxIndicator>
          </CheckboxRoot>
          <span class="type-body text-body-secondary">{{ term.label }}</span>
        </span>
        <RouterLink
          :aria-label="`${term.title} 상세 보기`"
          :to="`/auth/sign-up/terms/${termId}`"
          class="flex size-touch-target shrink-0 items-center justify-center text-body-muted outline-none focus-visible:ring-2 focus-visible:ring-focus"
          @click="emit('beforeNavigate')"
        >
          <ChevronRight aria-hidden="true" class="size-md" />
        </RouterLink>
      </div>
    </div>
  </section>
</template>
