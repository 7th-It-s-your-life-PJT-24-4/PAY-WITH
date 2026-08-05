<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  avatarId: number | null | undefined
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  select: [avatarId: number]
}>()

const pendingAvatarId = ref<number | null>(null)

watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) pendingAvatarId.value = props.avatarId ?? null
  },
)

function close() {
  emit('update:modelValue', false)
}

function confirm() {
  if (pendingAvatarId.value === null) return

  emit('select', pendingAvatarId.value)
  close()
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      aria-label="프로필 아바타 선택"
      aria-modal="true"
      class="fixed inset-0 z-50 flex items-end bg-black/40"
      role="dialog"
    >
      <section
        class="w-full rounded-t-large bg-surface px-mobile-gutter pb-[calc(24px+env(safe-area-inset-bottom))] pt-xl"
      >
        <h2 class="type-h3 text-body">프로필 아바타 선택</h2>
        <div class="mt-lg grid grid-cols-3 gap-md">
          <button
            v-for="id in 6"
            :key="id"
            :class="pendingAvatarId === id ? 'ring-2 ring-primary-500' : ''"
            class="rounded-full p-1"
            type="button"
            @click="pendingAvatarId = id"
          >
            <img
              :src="`/images/avatar/avatar${id}.png`"
              :alt="`아바타 ${id}`"
              class="aspect-square w-full rounded-full object-cover"
            />
          </button>
        </div>
        <div class="mt-xl grid grid-cols-2 gap-sm">
          <Button
            label="취소"
            type="button"
            variant="outline-primary"
            @click="close"
          />
          <Button
            :disabled="pendingAvatarId === null"
            label="확인"
            type="button"
            @click="confirm"
          />
        </div>
      </section>
    </div>
  </Teleport>
</template>
