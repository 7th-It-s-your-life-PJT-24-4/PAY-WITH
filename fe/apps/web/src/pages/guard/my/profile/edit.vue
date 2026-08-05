<script setup lang="ts">
import { Plus } from '@lucide/vue'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { getUserIdFromAccessToken, tokenStorage } from '@/api/token-storage'
import { useUpdateUserMutation } from '@/composables/useUpdateUserMutation'
import { useUserQuery } from '@/composables/useUserQuery'
import GuardMyHeader from '@/pages/guard/my/-components/GuardMyHeader.vue'
import GuardProfileAvatarPicker from '@/pages/guard/my/-components/GuardProfileAvatarPicker.vue'

const router = useRouter()
const accessToken = tokenStorage.getAccessToken()
const currentUserId = accessToken
  ? (getUserIdFromAccessToken(accessToken) ?? 0)
  : 0
const userQuery = useUserQuery(currentUserId)
const updateUserMutation = useUpdateUserMutation()
const name = ref('')
const avatarId = ref(1)
const isAvatarPickerOpen = ref(false)
const errorMessage = ref('')
const initialized = ref(false)

watch(
  () => userQuery.data.value,
  (user) => {
    if (!user || initialized.value) return
    name.value = user.name
    avatarId.value = user.avatarId ?? 1
    initialized.value = true
  },
  { immediate: true },
)

const canSave = computed(
  () =>
    name.value.trim().length > 0 &&
    initialized.value &&
    !updateUserMutation.isPending.value,
)

async function save() {
  if (!canSave.value || !currentUserId) return

  errorMessage.value = ''
  try {
    await updateUserMutation.mutateAsync({
      id: currentUserId,
      body: { name: name.value.trim(), avatarId: avatarId.value },
    })
    await router.replace({ name: 'guard-my-profile' })
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      '내 정보를 수정하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }
}
</script>

<template>
  <main class="min-h-screen bg-white">
    <GuardMyHeader
      title="내정보"
      show-back
      action="save"
      :action-disabled="!canSave"
      @back="router.push({ name: 'guard-my-profile' })"
      @action="save"
    />

    <section class="pt-xl" aria-label="내 정보 수정">
      <button
        class="relative mx-auto block size-[100px] rounded-full outline-none focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:ring-offset-2"
        type="button"
        aria-label="프로필 이미지 변경"
        @click="isAvatarPickerOpen = true"
      >
        <img
          :src="`/images/avatar/avatar${avatarId}.png`"
          alt=""
          class="size-[100px] rounded-full object-cover"
        />
        <span
          class="absolute bottom-2 right-2 flex size-4 items-center justify-center rounded-full bg-primary-900 text-primary-500"
        >
          <Plus class="size-2.5" :stroke-width="2" aria-hidden="true" />
        </span>
      </button>

      <form class="mt-md px-[18px]" @submit.prevent="save">
        <label
          for="guard-profile-name"
          class="block text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-[#171c1e]"
        >
          성함 (실명)
        </label>
        <input
          id="guard-profile-name"
          v-model="name"
          class="mt-sm h-[52px] w-full rounded-[8px] border border-gray-800 bg-white px-[17px] text-[16px] font-medium text-[#171c1e] outline-none placeholder:text-[#6b7280] focus:border-primary-500 focus:ring-1 focus:ring-primary-500"
          type="text"
          autocomplete="name"
          maxlength="30"
          placeholder="성함을 입력하세요"
        />

        <p
          v-if="errorMessage"
          class="mt-sm text-[14px] font-medium text-error"
          role="alert"
        >
          {{ errorMessage }}
        </p>
      </form>
    </section>

    <GuardProfileAvatarPicker
      v-model:open="isAvatarPickerOpen"
      :avatar-id="avatarId"
      @select="avatarId = $event"
    />
  </main>
</template>
