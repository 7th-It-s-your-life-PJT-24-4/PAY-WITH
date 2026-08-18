<script setup lang="ts">
import { ConfirmModal } from '@pay-with/ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { confirmPairingRequest } from '@/api/pairing'
import { guardHomeKeys } from '@/lib/query/guard/home'
import { pairingKeys, pendingPairingRequestOptions } from '@/lib/query/pairing'
import GuardBottomNavigation, {
  type GuardNavigationValue,
} from '@/pages/guard/-components/GuardBottomNavigation.vue'
import { useGuardWardSelection } from '@/pages/guard/-composables/useGuardWardSelection'
import { withGuardWardId } from '@/pages/guard/-utils/guard-route'

const route = useRoute()
const router = useRouter()
const queryClient = useQueryClient()
const pendingPairingRequestQuery = useQuery(pendingPairingRequestOptions())
const dismissedRequestId = ref<string | null>(null)
const pendingPairingRequest = computed(
  () => pendingPairingRequestQuery.data.value,
)
const isPairingRequestModalOpen = computed(
  () =>
    Boolean(pendingPairingRequest.value) &&
    pendingPairingRequest.value?.requestId !== dismissedRequestId.value,
)
const confirmPairingMutation = useMutation({
  mutationFn: confirmPairingRequest,
  onSuccess: async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: pairingKeys.pendingRequest() }),
      queryClient.invalidateQueries({ queryKey: guardHomeKeys.all }),
    ])
  },
})

const { activeWardId } = useGuardWardSelection()

const showBottomNavigation = computed(
  () => route.meta.showBottomNavigation !== false,
)
const activeNavigation = computed<GuardNavigationValue>(() => {
  const value = route.meta.activeNavigation
  if (
    value === 'charge' ||
    value === 'history' ||
    value === 'my' ||
    value === 'home'
  ) {
    return value
  }

  return 'home'
})

function handleNavigate(value: GuardNavigationValue) {
  const query = withGuardWardId({}, activeWardId.value)

  if (value === 'home') router.push({ name: 'guard-home', query })
  if (value === 'charge') router.push({ name: 'guard-charge', query })
  if (value === 'history') router.push({ name: 'guard-history', query })
  if (value === 'my') router.push({ name: 'guard-my' })
}
watch(pendingPairingRequest, (request) => {
  if (!request || request.requestId !== dismissedRequestId.value) {
    dismissedRequestId.value = null
  }
})

function dismissPairingRequest() {
  dismissedRequestId.value = pendingPairingRequest.value?.requestId ?? null
}

async function confirmPendingPairingRequest() {
  const requestId = pendingPairingRequest.value?.requestId
  if (!requestId) return
  await confirmPairingMutation.mutateAsync(requestId)
}
</script>

<template>
  <div class="min-h-screen bg-gray-900 px-0 sm:px-md">
    <div
      class="mx-auto min-h-screen w-full max-w-[390px] bg-white text-body shadow-card"
    >
      <RouterView />

      <ConfirmModal
        :open="isPairingRequestModalOpen"
        :title="`${pendingPairingRequest?.wardName ?? '시니어'}님(${pendingPairingRequest?.wardPhoneMasked ?? ''})의 연결 요청을 수락할까요?`"
        cancel-label="나중에"
        :confirm-label="
          confirmPairingMutation.isPending.value ? '수락 중' : '수락'
        "
        :confirm-disabled="confirmPairingMutation.isPending.value"
        @cancel="dismissPairingRequest"
        @confirm="confirmPendingPairingRequest"
      />

      <GuardBottomNavigation
        v-if="showBottomNavigation"
        :active="activeNavigation"
        @navigate="handleNavigate"
      />
    </div>
  </div>
</template>
