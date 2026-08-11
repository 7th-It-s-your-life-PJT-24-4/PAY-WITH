<script setup lang="ts">
import { ChevronRight, Landmark, Plus } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { registerWardSafeAccount } from '@/api/ward-safe-accounts'
import {
  wardSafeAccountKeys,
  wardSafeAccountsOptions,
} from '@/lib/query/ward/safe-account'
import {
  transferRecipientsOptions,
  wardTransferKeys,
} from '@/lib/query/ward/transfer'
import AddTransferContactModal from '@/pages/ward/transfer/-components/AddTransferContactModal.vue'
import TransferRecipientCard from '@/pages/ward/transfer/-components/TransferRecipientCard.vue'
import {
  toSafeAccountTransferRecipient,
  toTransferRecipient,
} from '@/pages/ward/transfer/-utils/transfer-recipient'
import {
  useTransferStore,
  type TransferRecipient,
} from '@/stores/transfer.store'

const router = useRouter()
const queryClient = useQueryClient()
const transferStore = useTransferStore()

onMounted(() => {
  transferStore.reset()
})
const activeTab = ref<'safe-accounts' | 'recent'>('safe-accounts')
const contactModalOpen = ref(false)
const pendingContact = ref<TransferRecipient | null>(null)
const contactErrorMessage = ref<string | null>(null)
const recentQuery = useQuery(
  transferRecipientsOptions({ sort: 'RECENT', size: 4 }),
)
const safeAccountsQuery = useQuery(wardSafeAccountsOptions())
const registerSafeAccountMutation = useMutation({
  mutationFn: registerWardSafeAccount,
  onSuccess: async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: wardSafeAccountKeys.all }),
      queryClient.invalidateQueries({ queryKey: wardTransferKeys.all }),
    ])
  },
})
const recentRecipients = computed(
  () => recentQuery.data.value?.map(toTransferRecipient) ?? [],
)
const allSafeAccounts = computed(
  () => safeAccountsQuery.data.value?.map(toSafeAccountTransferRecipient) ?? [],
)
const safeAccounts = computed(() => allSafeAccounts.value)
function isContact(recipient: TransferRecipient) {
  return allSafeAccounts.value.some(
    (contact) => contact.id === recipient.id && contact.isContact,
  )
}

function openContactModal(recipient: TransferRecipient) {
  pendingContact.value = recipient
  contactErrorMessage.value = null
  contactModalOpen.value = true
}

async function addContact(alias: string) {
  if (!pendingContact.value || isContact(pendingContact.value)) return

  contactErrorMessage.value = null

  try {
    await registerSafeAccountMutation.mutateAsync({
      recipientId: pendingContact.value.id,
      accountAlias: alias || undefined,
    })
    contactModalOpen.value = false
    pendingContact.value = null
  } catch (error) {
    contactErrorMessage.value = await getApiErrorMessage(
      error,
      '안심계좌를 추가하지 못했습니다.',
    )
  }
}

function selectRecipient(recipient: TransferRecipient) {
  transferStore.selectRecipient(recipient)
  router.push({ name: 'ward-transfer-amount' })
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <section>
      <h2 class="type-h1">누구에게 보낼까요?</h2>
      <p class="type-body-medium mt-xs text-body-secondary">
        송금할 분을 선택해 주세요.
      </p>
    </section>

    <Button
      class="type-h2 w-full whitespace-nowrap"
      label="계좌 번호 직접 입력하기"
      size="large"
      @click="router.push({ name: 'ward-transfer-account' })"
    >
      <template #leading>
        <Landmark :stroke-width="2.25" />
      </template>
      <template #trailing>
        <ChevronRight :stroke-width="2.25" />
      </template>
    </Button>

    <section aria-label="송금 대상 목록">
      <div
        class="grid grid-cols-2 rounded-large bg-surface-card p-xs shadow-card"
        role="tablist"
        aria-label="송금 대상 종류"
      >
        <button
          class="type-h4 rounded-medium px-md py-sm"
          :class="
            activeTab === 'safe-accounts'
              ? 'bg-primary-900 text-primary-500'
              : 'text-body-secondary'
          "
          type="button"
          role="tab"
          :aria-selected="activeTab === 'safe-accounts'"
          @click="activeTab = 'safe-accounts'"
        >
          안심계좌
        </button>
        <button
          class="type-h4 rounded-medium px-md py-sm"
          :class="
            activeTab === 'recent'
              ? 'bg-primary-900 text-primary-500'
              : 'text-body-secondary'
          "
          type="button"
          role="tab"
          :aria-selected="activeTab === 'recent'"
          @click="activeTab = 'recent'"
        >
          최근 보낸 사람
        </button>
      </div>

      <div v-if="activeTab === 'safe-accounts'" class="mt-md">
        <p
          v-if="safeAccountsQuery.isPending.value"
          class="type-body-medium text-body-muted"
          role="status"
        >
          안심계좌를 불러오고 있습니다.
        </p>
        <div
          v-else-if="safeAccountsQuery.isError.value"
          class="rounded-large bg-surface-card p-lg text-center shadow-card"
        >
          <p class="type-body-medium text-error" role="alert">
            안심계좌를 불러오지 못했습니다.
          </p>
          <Button
            class="mt-md"
            label="다시 시도"
            variant="outline-primary"
            @click="safeAccountsQuery.refetch()"
          />
        </div>
        <div
          v-else-if="safeAccounts.length"
          class="overflow-hidden rounded-large bg-surface-card shadow-card"
        >
          <TransferRecipientCard
            v-for="recipient in safeAccounts"
            :key="recipient.id"
            :recipient="recipient"
            @select="selectRecipient"
          />
        </div>
        <p v-else class="type-body-medium text-center text-body-muted">
          등록된 안심계좌가 없습니다.
        </p>
      </div>

      <div v-else class="mt-md">
        <p
          v-if="recentQuery.isPending.value"
          class="type-body-medium text-body-muted"
          role="status"
        >
          최근 송금 대상을 불러오고 있습니다.
        </p>
        <p
          v-else-if="recentQuery.isError.value"
          class="type-body-medium text-error"
          role="alert"
        >
          최근 송금 대상을 불러오지 못했습니다.
        </p>
        <div
          v-else-if="recentRecipients.length"
          class="overflow-hidden rounded-large bg-surface-card shadow-card"
        >
          <div v-for="recipient in recentRecipients" :key="recipient.id">
            <TransferRecipientCard
              :recipient="recipient"
              @select="selectRecipient"
            />
            <button
              v-if="!isContact(recipient)"
              class="type-body-medium flex min-h-touch-target w-full items-center justify-center gap-xs border-b border-border px-md py-sm text-primary-500"
              type="button"
              :aria-label="`${recipient.name} 안심계좌 추가`"
              @click="openContactModal(recipient)"
            >
              <Plus
                class="size-lg shrink-0"
                :stroke-width="2.5"
                aria-hidden="true"
              />
              안심계좌 추가
            </button>
          </div>
        </div>
        <p v-else class="type-body-medium text-center text-body-muted">
          최근 보낸 사람이 없습니다.
        </p>
      </div>
    </section>

    <AddTransferContactModal
      v-model:open="contactModalOpen"
      :recipient="pendingContact"
      :pending="registerSafeAccountMutation.isPending.value"
      :error-message="contactErrorMessage"
      @confirm="addContact"
    />
  </div>
</template>
