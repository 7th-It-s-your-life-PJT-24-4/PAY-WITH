<script setup lang="ts">
import { Check, X } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type TransactionDecision = 'approved' | 'rejected'

const route = useRoute()
const router = useRouter()
const transactionId = computed(() => String(route.params.transactionId))
const decision = computed<TransactionDecision>(() =>
  route.query.decision === 'rejected' ? 'rejected' : 'approved',
)
const isApproved = computed(() => decision.value === 'approved')

function confirm() {
  router.replace({
    name: 'guard-transaction-detail',
    params: { transactionId: transactionId.value },
    query: { decision: decision.value },
  })
}
</script>

<template>
  <main
    class="flex min-h-screen flex-col bg-white px-mobile-gutter pb-mobile-gutter"
  >
    <section
      class="flex flex-1 flex-col items-center justify-center pb-[92px] text-center"
      aria-labelledby="transaction-decision-complete-title"
    >
      <span
        class="flex size-[76px] items-center justify-center rounded-full"
        :class="
          isApproved ? 'bg-primary-500 text-white' : 'bg-error text-white'
        "
        aria-hidden="true"
      >
        <Check v-if="isApproved" class="size-12" :stroke-width="3.5" />
        <X v-else class="size-12" :stroke-width="3.5" />
      </span>
      <h1
        id="transaction-decision-complete-title"
        class="mt-lg text-[28px] font-bold leading-[1.2] tracking-[-0.56px] text-black"
      >
        이상 거래를<br />
        {{ isApproved ? '승인했어요' : '거절했어요' }}
      </h1>
    </section>

    <Button
      class="w-full"
      label="확인"
      variant="guard-cta"
      size="guard-cta"
      @click="confirm"
    />
  </main>
</template>
