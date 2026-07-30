<script setup lang="ts">
import { ref } from 'vue'

import {
  mockGuardSeniors,
  mockGuardTransactions,
} from '@/mocks/guard-home.mock'
import GuardAssetCard from '@/pages/guard/-components/GuardAssetCard.vue'
import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'
import GuardTransactionList from '@/pages/guard/-components/GuardTransactionList.vue'

const isConnected = ref(false)
</script>

<template>
  <main class="min-h-screen pb-[calc(66px+env(safe-area-inset-bottom))]">
    <section
      v-if="!isConnected"
      class="flex min-h-[calc(100dvh-66px-env(safe-area-inset-bottom))] flex-col items-center justify-center px-mobile-gutter text-center"
      aria-labelledby="guard-unpaired-title"
    >
      <h1
        id="guard-unpaired-title"
        class="text-[16px] font-medium leading-6 tracking-[-0.2px] text-gray-500"
      >
        연결된 시니어가 없어요.<br />
        시니어와 연동해 안전하게 보호하세요.
      </h1>
      <button
        class="mt-lg flex h-14 w-full items-center justify-center rounded-[10px] bg-primary-500 text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-white shadow-[0_10px_15px_-3px_rgb(0_0_0/10%),0_4px_6px_-4px_rgb(0_0_0/10%)] transition-colors hover:bg-primary-400 active:bg-primary-300"
        type="button"
        @click="isConnected = true"
      >
        시니어와 연결하기
      </button>
    </section>

    <div v-else class="px-mobile-gutter pt-md">
      <GuardSeniorAvatarList
        :seniors="mockGuardSeniors"
        active-senior-id="sui"
      />

      <GuardAssetCard class="mt-md" senior-name="수이" balance="1,000,000" />

      <GuardTransactionList
        class="mt-md"
        :transactions="mockGuardTransactions"
      />
    </div>
  </main>
</template>
