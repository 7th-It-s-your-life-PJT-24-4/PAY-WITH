<script setup lang="ts">
import { AppHeader } from '@pay-with/ui'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { signUpTerms, type SignUpTermId } from '@/constants/sign-up-terms'

const route = useRoute()
const router = useRouter()

const term = computed(() => {
  const termId = route.params.termId

  if (typeof termId !== 'string' || !(termId in signUpTerms)) return null

  return signUpTerms[termId as SignUpTermId]
})
</script>

<template>
  <main class="mx-auto min-h-screen w-full max-w-[390px] bg-surface">
    <AppHeader title="약관 상세" show-back @back="router.back()" />

    <article v-if="term" class="px-mobile-gutter pb-section pt-section">
      <p class="type-caption text-body-muted">시행일: {{ term.updatedAt }}</p>
      <h1 class="type-h2 mt-sm text-body">{{ term.title }}</h1>

      <div class="mt-xxl flex flex-col gap-xl">
        <section v-for="section in term.sections" :key="section.title">
          <h2 class="type-h4 text-body">{{ section.title }}</h2>
          <p
            v-for="paragraph in section.paragraphs"
            :key="paragraph"
            class="type-body-regular mt-sm leading-6 text-body-secondary"
          >
            {{ paragraph }}
          </p>
        </section>
      </div>

      <p
        class="type-caption mt-xxl rounded-medium bg-disabled/40 p-md text-body-muted"
      >
        실제 서비스 운영 전 약관 전문과 시행일은 법무 검토를 거쳐 확정해야
        합니다.
      </p>
    </article>

    <section v-else class="px-mobile-gutter pt-section text-center">
      <h1 class="type-h3 text-body">약관을 찾을 수 없습니다.</h1>
    </section>
  </main>
</template>
