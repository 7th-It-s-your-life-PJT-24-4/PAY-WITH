<script setup lang="ts">
import { BottomSheet, Button } from '@pay-with/ui'
import { ref, watch } from 'vue'

const props = defineProps<{
  open: boolean
  avatarId: number
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  select: [avatarId: number]
}>()

const pendingAvatarId = ref(props.avatarId)

watch(
  () => props.open,
  (open) => {
    if (open) pendingAvatarId.value = props.avatarId
  },
)

function confirm() {
  emit('select', pendingAvatarId.value)
  emit('update:open', false)
}
</script>

<template>
  <BottomSheet
    :open="open"
    title="프로필 선택"
    description="사용할 프로필 이미지를 선택해 주세요."
    @update:open="emit('update:open', $event)"
  >
    <div class="mt-lg grid grid-cols-3 gap-md">
      <button
        v-for="id in 6"
        :key="id"
        class="aspect-square rounded-full p-1 outline-none transition-shadow focus-visible:ring-2 focus-visible:ring-primary-500"
        :class="pendingAvatarId === id ? 'ring-2 ring-primary-500' : ''"
        type="button"
        :aria-label="`프로필 ${id}`"
        :aria-pressed="pendingAvatarId === id"
        @click="pendingAvatarId = id"
      >
        <img
          :src="`/images/avatar/avatar${id}.png`"
          alt=""
          class="size-full rounded-full object-cover"
        />
      </button>
    </div>

    <Button
      class="mt-xl w-full"
      label="선택하기"
      variant="guard-cta"
      size="guard-cta"
      @click="confirm"
    />
  </BottomSheet>
</template>
