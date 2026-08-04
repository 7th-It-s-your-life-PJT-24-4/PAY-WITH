<script setup lang="ts">
import { Button } from '@pay-with/ui'

defineProps<{
  seniorName: string
  balance: string
}>()

const emit = defineEmits<{
  addSafeAccount: []
}>()
</script>

<template>
  <section
    class="rounded-large bg-[#f0f3f8] px-md pb-md pt-md"
    :aria-label="`${seniorName} 자산`"
  >
    <p class="type-h4 font-bold leading-6 text-gray-300">
      {{ seniorName }} 님의 현재 자산
    </p>
    <p class="mt-md text-[28px] font-bold leading-[21px] tracking-[-0.2px]">
      {{ balance }}원
    </p>
    <div class="mt-[18px] grid grid-cols-2 gap-sm">
      <Button
        class="w-full !min-h-[42px] !rounded-medium !border-[0.5px] !border-[#c9ced4] !bg-white !px-sm !py-[6px] !text-[14px] !font-bold !leading-[21px] !tracking-[-0.2px] !text-[#494d54]"
        label="충전하기"
        variant="secondary"
        size="small"
      />
      <Button
        class="w-full !min-h-[42px] !rounded-medium !border-[0.5px] !border-[#c9ced4] !bg-white !px-sm !py-[6px] !text-[14px] !font-bold !leading-[21px] !tracking-[-0.2px] !text-[#494d54]"
        label="안전계좌 추가하기"
        variant="secondary"
        size="small"
        @click="emit('addSafeAccount')"
      />
    </div>
  </section>
</template>
