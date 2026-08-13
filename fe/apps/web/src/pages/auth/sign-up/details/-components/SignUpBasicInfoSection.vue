<script setup lang="ts">
import { Check, ChevronDown } from '@lucide/vue'
import { Input } from '@pay-with/ui'
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

defineProps<{
  fullName: string
  birthDate: string
  gender: '남' | '여' | undefined
  fullNameError?: string
  birthDateError?: string
  genderError?: string
}>()

const emit = defineEmits<{
  'update:fullName': [value: string]
  'update:birthDate': [value: string]
  'update:gender': [value: '남' | '여' | undefined]
}>()

function inputValueFromEvent(event: unknown) {
  if (typeof event !== 'object' || event === null) return ''

  const target = Reflect.get(event, 'target')
  if (typeof target !== 'object' || target === null) return ''

  const value = Reflect.get(target, 'value')
  return typeof value === 'string' ? value : ''
}

function handleFullNameInput(value: string) {
  emit('update:fullName', value.replace(/[^가-힣]/g, '').slice(0, 30))
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <Input
      :error="fullNameError"
      :model-value="fullName"
      :maxlength="30"
      autocomplete="name"
      :input-filter="/[^가-힣]/g"
      label="성함 (실명)"
      placeholder="성함을 입력하세요"
      @update:model-value="handleFullNameInput($event)"
    />

    <section class="flex flex-col gap-sm">
      <label class="type-h4 text-body" for="sign-up-birth-date">생년월일</label>
      <input
        id="sign-up-birth-date"
        :aria-describedby="
          birthDateError ? 'sign-up-birth-date-error' : undefined
        "
        :aria-invalid="birthDateError ? 'true' : undefined"
        :value="birthDate"
        autocomplete="bday"
        class="h-[52px] w-full rounded-medium border border-border-strong bg-surface-card px-md text-[16px] font-normal leading-[1.2] tracking-[-0.32px] text-body outline-none placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
        inputmode="numeric"
        maxlength="10"
        pattern="\d{4}\.\d{2}\.\d{2}"
        placeholder="YYYY.MM.DD"
        type="text"
        @input="emit('update:birthDate', inputValueFromEvent($event))"
      />
      <p
        v-if="birthDateError"
        id="sign-up-birth-date-error"
        class="type-caption text-error"
      >
        {{ birthDateError }}
      </p>
    </section>

    <section class="flex flex-col gap-sm">
      <label id="sign-up-gender-label" class="type-h4 text-body">성별</label>
      <SelectRoot
        :model-value="gender"
        @update:model-value="emit('update:gender', $event)"
      >
        <SelectTrigger
          aria-labelledby="sign-up-gender-label"
          :aria-invalid="genderError ? 'true' : undefined"
          class="group flex h-[52px] w-full items-center gap-sm rounded-medium border border-border-strong bg-surface-card px-md text-left text-[14px] font-medium leading-[1.2] tracking-[-0.28px] text-body outline-none transition-colors data-[placeholder]:text-body-muted data-[state=open]:border-primary-500 focus-visible:border-focus focus-visible:ring-2 focus-visible:ring-focus/20"
        >
          <SelectValue
            class="min-w-0 flex-1"
            placeholder="성별을 선택해 주세요"
          />
          <ChevronDown
            aria-hidden="true"
            class="size-xl shrink-0 text-body-muted transition-transform group-data-[state=open]:rotate-180"
          />
        </SelectTrigger>

        <SelectPortal>
          <SelectContent
            align="start"
            class="z-50 w-[var(--reka-select-trigger-width)] overflow-hidden rounded-large border border-border bg-surface-card p-sm shadow-modal"
            position="popper"
            :side-offset="8"
          >
            <SelectViewport class="flex flex-col gap-xs">
              <SelectItem
                class="group/item relative flex min-h-touch-target cursor-pointer select-none items-center rounded-medium px-md py-sm outline-none data-[highlighted]:bg-primary-900 data-[state=checked]:text-primary-500"
                value="남"
              >
                <SelectItemText class="flex-1 text-[14px] font-medium"
                  >남성</SelectItemText
                >
                <SelectItemIndicator class="text-primary-500">
                  <Check
                    aria-hidden="true"
                    class="size-xl"
                    :stroke-width="2.5"
                  />
                </SelectItemIndicator>
              </SelectItem>
              <SelectItem
                class="group/item relative flex min-h-touch-target cursor-pointer select-none items-center rounded-medium px-md py-sm outline-none data-[highlighted]:bg-primary-900 data-[state=checked]:text-primary-500"
                value="여"
              >
                <SelectItemText class="flex-1 text-[14px] font-medium"
                  >여성</SelectItemText
                >
                <SelectItemIndicator class="text-primary-500">
                  <Check
                    aria-hidden="true"
                    class="size-xl"
                    :stroke-width="2.5"
                  />
                </SelectItemIndicator>
              </SelectItem>
            </SelectViewport>
          </SelectContent>
        </SelectPortal>
      </SelectRoot>
      <p v-if="genderError" class="type-caption text-error">
        {{ genderError }}
      </p>
    </section>
  </div>
</template>
