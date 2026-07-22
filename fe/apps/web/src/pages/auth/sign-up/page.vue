<script setup lang="ts">
import { HeartHandshake, UserRound } from '@lucide/vue'
import { AppHeader, Button } from '@pay-with/ui'
import { RadioGroupIndicator, RadioGroupItem, RadioGroupRoot } from 'reka-ui'
import { useForm } from 'vee-validate'
import { useRouter } from 'vue-router'

import { signUpRoleSchema, type SignUpRole } from '@/schemas/sign-up.schema'
import { useSignUpStore } from '@/stores/sign-up.store'

const router = useRouter()
const signUpStore = useSignUpStore()

type SignUpRoleForm = {
  role?: SignUpRole
}

const roleOptions = [
  {
    value: 'senior' as const,
    label: '시니어',
    description: '안전하게 자산을 관리하고 싶어요',
    icon: UserRound,
    iconClass: 'bg-primary-500/10 text-primary-500',
  },
  {
    value: 'guardian' as const,
    label: '보호자',
    description: '가족의 금융 활동을 돕고 싶어요',
    icon: HeartHandshake,
    iconClass: 'bg-warning/10 text-warning',
  },
] satisfies Array<{
  value: SignUpRole
  label: string
  description: string
  icon: typeof UserRound
  iconClass: string
}>

const { defineField, errors, handleSubmit } = useForm<SignUpRoleForm>({
  validationSchema: {
    role(value: unknown) {
      const result = signUpRoleSchema.shape.role.safeParse(value)

      return result.success
        ? true
        : (result.error.issues[0]?.message ?? '가입 유형을 선택해 주세요.')
    },
  },
})

const [role] = defineField('role')

const submitFirstStep = handleSubmit((values) => {
  if (!values.role) {
    return
  }

  signUpStore.setRole(values.role)
  router.push('/auth/sign-up/details')
})
</script>

<template>
  <main
    class="mx-auto flex min-h-screen w-full max-w-[390px] flex-col bg-surface"
  >
    <AppHeader title="회원가입" show-back @back="router.back()" />

    <section
      class="flex flex-col items-center pt-xxl"
      aria-label="회원가입 진행 상태"
    >
      <div class="mb-xs flex gap-xs" aria-hidden="true">
        <span class="size-xs rounded-full bg-primary-300" />
        <span class="size-xs rounded-full bg-border" />
      </div>
      <p class="type-caption text-body-muted">1 / 2 단계</p>
    </section>

    <form
      class="flex flex-1 flex-col px-mobile-gutter pt-section"
      @submit.prevent="submitFirstStep"
    >
      <section class="pb-section text-center">
        <h1 class="type-h1 text-body">
          어떤 유형으로<br />
          가입하시겠습니까?
        </h1>
        <p class="type-h4 mt-sm text-body-muted">
          사용자님의 환경에 맞는 역할을 선택해 주세요.
        </p>
      </section>

      <RadioGroupRoot
        v-model="role"
        aria-label="가입 유형"
        class="grid grid-cols-2 gap-md"
        name="role"
      >
        <RadioGroupItem
          v-for="option in roleOptions"
          :key="option.value"
          :value="option.value"
          class="group flex min-h-[264px] flex-col items-center justify-center gap-lg rounded-large border-2 border-transparent bg-surface-card p-[26px] text-center shadow-card outline-none transition-colors data-[state=checked]:border-primary-500 focus-visible:ring-2 focus-visible:ring-focus focus-visible:ring-offset-2"
        >
          <span
            aria-hidden="true"
            class="flex size-[64px] items-center justify-center rounded-full"
            :class="option.iconClass"
          >
            <component :is="option.icon" class="size-7" :stroke-width="1.8" />
          </span>

          <span class="flex flex-1 flex-col justify-center gap-xs">
            <span class="type-h3 text-body">{{ option.label }}</span>
            <span class="type-body-medium leading-6 text-body-muted">
              {{ option.description }}
            </span>
          </span>

          <span
            class="flex size-xl items-center justify-center rounded-full border-2 border-subbutton group-data-[state=checked]:border-primary-500"
          >
            <RadioGroupIndicator
              class="size-[10px] rounded-full bg-primary-500"
            />
          </span>
        </RadioGroupItem>
      </RadioGroupRoot>

      <p
        v-if="errors.role"
        class="type-caption mt-sm text-center text-error"
        role="alert"
      >
        {{ errors.role }}
      </p>

      <div class="min-h-[256px] flex-1" />

      <div
        class="sticky bottom-0 -mx-mobile-gutter bg-gradient-to-t from-surface via-surface to-transparent px-mobile-gutter pb-lg pt-section"
      >
        <Button
          class="w-full shadow-[0_10px_15px_-3px_rgb(0_0_0/10%),0_4px_6px_-4px_rgb(0_0_0/10%)]"
          label="다음"
          type="submit"
        />
      </div>
    </form>
  </main>
</template>
