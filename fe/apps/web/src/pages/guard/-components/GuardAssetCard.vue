<script setup lang="ts">
import { Button } from '@pay-with/ui'

defineProps<{
  seniorName: string
  balance: string
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
    <Button
      class="mt-[18px] w-full !min-h-[42px] !rounded-medium !border-[0.5px] !border-[#c9ced4] !bg-white !px-sm !py-[6px] !text-[14px] !font-bold !leading-[21px] !tracking-[-0.2px] !text-[#494d54]"
      label="충전하기"
      variant="secondary"
      size="small"
    />
  </section>
</template>
