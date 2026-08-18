<script setup lang="ts">
import { LoaderCircle } from '@lucide/vue'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { wardPairingRequestStatusOptions } from '@/lib/query/pairing'
import { usePairingStore } from '@/stores/pairing.store'

const router = useRouter()
const pairingStore = usePairingStore()
const requestId = computed(() => pairingStore.pendingRequestId)
const statusQuery = useQuery(wardPairingRequestStatusOptions(requestId))
const isResolvingRequest = ref(false)

watch(
  () => statusQuery.data.value,
  (status) => {
    if (status === 'CONFIRMED') {
      isResolvingRequest.value = true
      pairingStore.markPaired()
      void router.replace({ name: 'ward-pairing-complete' })
      return
    }

    if (status === 'EXPIRED') {
      isResolvingRequest.value = true
      pairingStore.clearPendingRequest()
      pairingStore.errorMessage =
        '보호자 확인 시간이 만료되었습니다. 인증 코드를 다시 입력해 주세요.'
      void router.replace({ name: 'ward-pairing' })
    }
  },
  { immediate: true },
)

watch(
  () => pairingStore.pendingRequestId,
  (value) => {
    if (!value && !isResolvingRequest.value) {
      void router.replace({ name: 'ward-pairing' })
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="flex flex-1 flex-col items-center justify-center text-center">
    <span
      class="flex size-[72px] items-center justify-center rounded-full bg-primary-500/10 text-primary-500"
      aria-hidden="true"
    >
      <LoaderCircle class="size-9 animate-spin" :stroke-width="2" />
    </span>

    <h1 class="type-h1 mt-lg text-body">보호자 확인을 기다리고 있어요</h1>
    <p class="type-body mt-sm text-body-muted">
      보호자가 연결 요청을 수락하면<br />자동으로 다음 단계로 이동합니다.
    </p>

    <p
      v-if="statusQuery.isError.value"
      class="type-body-medium mt-xl text-error"
      role="alert"
    >
      상태를 확인하지 못했어요. 네트워크 연결을 확인해 주세요.
    </p>
  </div>
</template>
