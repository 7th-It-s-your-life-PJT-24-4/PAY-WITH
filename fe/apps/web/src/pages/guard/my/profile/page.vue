<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { getUserIdFromAccessToken, tokenStorage } from '@/api/token-storage'
import { useUserQuery } from '@/composables/useUserQuery'
import { guardHomeOptions } from '@/lib/query/guard/home'
import GuardMyHeader from '@/pages/guard/my/-components/GuardMyHeader.vue'

const router = useRouter()
const accessToken = tokenStorage.getAccessToken()
const currentUserId = accessToken
  ? (getUserIdFromAccessToken(accessToken) ?? 0)
  : 0
const userQuery = useUserQuery(currentUserId)
const guardHomeQuery = useQuery(guardHomeOptions())

const avatarId = computed(() => userQuery.data.value?.avatarId ?? 1)
const formattedPhone = computed(() => {
  const phone = userQuery.data.value?.phone ?? ''
  if (phone.length === 11)
    return phone.replace(/^(\d{3})(\d{4})(\d{4})$/, '$1-$2-$3')
  if (phone.length === 10)
    return phone.replace(/^(\d{3})(\d{3})(\d{4})$/, '$1-$2-$3')
  return phone
})
const seniorCount = computed(() => guardHomeQuery.data.value?.wards.length ?? 0)
</script>

<template>
  <main class="min-h-screen bg-white">
    <GuardMyHeader
      title="내정보"
      show-back
      action="edit"
      @back="router.push({ name: 'guard-my' })"
      @action="router.push({ name: 'guard-my-profile-edit' })"
    />

    <section
      v-if="userQuery.isPending.value"
      class="flex min-h-[calc(100dvh-44px)] items-center justify-center px-mobile-gutter text-[16px] font-medium text-gray-600"
    >
      내 정보를 불러오는 중이에요.
    </section>

    <section
      v-else-if="userQuery.isError.value"
      class="flex min-h-[calc(100dvh-44px)] items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-error"
    >
      내 정보를 불러오지 못했어요.
    </section>

    <section v-else class="pt-xl" aria-label="내 정보">
      <img
        :src="`/images/avatar/avatar${avatarId}.png`"
        alt="내 프로필"
        class="mx-auto size-[100px] rounded-full object-cover"
      />

      <dl class="mt-xl px-mobile-gutter">
        <div class="flex h-14 items-center justify-between gap-lg">
          <dt
            class="shrink-0 text-[20px] font-medium leading-[1.2] tracking-[-0.4px] text-gray-700"
          >
            이름
          </dt>
          <dd
            class="truncate text-right text-[20px] font-semibold leading-[1.2] tracking-[-0.4px] text-black"
          >
            {{ userQuery.data.value?.name }}
          </dd>
        </div>
        <div class="flex h-14 items-center justify-between gap-lg">
          <dt
            class="shrink-0 text-[20px] font-medium leading-[1.2] tracking-[-0.4px] text-gray-700"
          >
            연락처
          </dt>
          <dd
            class="truncate text-right text-[20px] font-semibold leading-[1.2] tracking-[-0.4px] text-black"
          >
            {{ formattedPhone }}
          </dd>
        </div>
        <div class="flex h-14 items-center justify-between gap-lg">
          <dt
            class="shrink-0 text-[20px] font-medium leading-[1.2] tracking-[-0.4px] text-gray-700"
          >
            연동된 시니어
          </dt>
          <dd
            class="text-right text-[20px] font-semibold leading-[1.2] tracking-[-0.4px] text-black"
          >
            {{ seniorCount }}명
          </dd>
        </div>
      </dl>
    </section>
  </main>
</template>
