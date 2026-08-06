<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { getUserIdFromAccessToken, tokenStorage } from '@/api/token-storage'
import { useUserQuery } from '@/composables/useUserQuery'

const router = useRouter()
const accessToken = tokenStorage.getAccessToken()
const currentUserId = accessToken
  ? (getUserIdFromAccessToken(accessToken) ?? 0)
  : 0
const userQuery = useUserQuery(currentUserId)

const avatarId = computed(() => userQuery.data.value?.avatarId ?? 1)
const formattedPhone = computed(() => {
  const phone = userQuery.data.value?.phone ?? ''
  if (phone.length === 11)
    return phone.replace(/^(\d{3})(\d{4})(\d{4})$/, '$1-$2-$3')
  if (phone.length === 10)
    return phone.replace(/^(\d{3})(\d{3})(\d{4})$/, '$1-$2-$3')
  return phone
})

const profileItems = computed(() => [
  { label: '이름', value: userQuery.data.value?.name ?? '-' },
  { label: '연락처', value: formattedPhone.value || '-' },
])
</script>

<template>
  <section class="flex flex-1 flex-col items-center" aria-label="내 정보">
    <p
      v-if="userQuery.isPending.value"
      class="mt-xxl text-center text-[22px] font-bold leading-[1.3] text-gray-700"
    >
      내 정보를 불러오는 중이에요.
    </p>

    <p
      v-else-if="userQuery.isError.value"
      class="mt-xxl text-center text-[22px] font-bold leading-[1.3] text-error"
      role="alert"
    >
      내 정보를 불러오지 못했어요.
    </p>

    <template v-else>
      <img
        :src="`/images/avatar/avatar${avatarId}.png`"
        alt="내 프로필"
        class="size-[120px] rounded-full object-cover shadow-[0_8px_24px_rgb(0_106_126/18%)]"
      />

      <dl class="mt-xl w-full space-y-md">
        <div
          v-for="item in profileItems"
          :key="item.label"
          class="flex min-h-[72px] items-center justify-between gap-lg rounded-[16px] border-2 border-primary-500 bg-white px-lg"
        >
          <dt
            class="shrink-0 text-[22px] font-bold leading-[1.2] text-primary-300"
          >
            {{ item.label }}
          </dt>
          <dd
            class="min-w-0 truncate text-right text-[22px] font-bold leading-[1.2] text-[#111827]"
          >
            {{ item.value }}
          </dd>
        </div>
      </dl>
    </template>

    <Button
      class="mt-xl min-h-[64px] w-full rounded-[16px] text-[24px] font-bold leading-[1.2]"
      label="확인"
      size="large"
      @click="router.push({ name: 'ward-my' })"
    />
  </section>
</template>
