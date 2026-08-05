<script setup lang="ts">
import { Camera, Plus } from '@lucide/vue'
import { Button } from '@pay-with/ui'

defineProps<{
  avatarId: number | null | undefined
}>()

const emit = defineEmits<{
  select: []
}>()
</script>

<template>
  <section>
    <p class="type-h4 text-body">프로필 이미지</p>
    <p class="type-body mt-xs text-body-muted">
      나를 표현할 아바타를 선택해 주세요
    </p>
    <div class="flex flex-col items-center pb-md pt-xl">
      <button
        aria-label="프로필 아바타 선택하기"
        class="relative flex size-[128px] items-center justify-center rounded-full border-2 border-dashed border-border-strong bg-disabled outline-none focus-visible:ring-2 focus-visible:ring-focus focus-visible:ring-offset-2"
        type="button"
        @click="emit('select')"
      >
        <img
          v-if="avatarId"
          :src="`/images/avatar/avatar${avatarId}.png`"
          alt="선택한 프로필 아바타"
          class="size-full rounded-full object-cover"
        />
        <Camera v-else aria-hidden="true" class="size-10 text-body-muted" />
        <span
          aria-hidden="true"
          class="absolute bottom-0 right-0 flex size-[40px] items-center justify-center rounded-full border-4 border-surface bg-primary-500 text-on-action shadow-card"
        >
          <Plus class="size-lg" :stroke-width="3" />
        </span>
      </button>
      <Button
        class="mt-md"
        label="프로필 사진 선택하기"
        pill
        size="small"
        type="button"
        variant="outline-primary"
        @click="emit('select')"
      />
    </div>
  </section>
</template>
