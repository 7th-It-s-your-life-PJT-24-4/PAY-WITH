<script setup lang="ts">
import { ChevronRight, Landmark, Plus } from '@lucide/vue'
import { Button, Input } from '@pay-with/ui'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, ref } from 'vue'
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
const search = ref('')
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
const safeAccounts = computed(() =>
  allSafeAccounts.value.filter((recipient) => {
    const keyword = search.value.trim()
    if (!keyword) return true
    return `${recipient.name}${recipient.holderName ?? ''}${recipient.accountNumber}`.includes(
      keyword,
    )
  }),
)
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

    <section
      v-if="
        recentQuery.isPending.value ||
        recentQuery.isError.value ||
        recentRecipients.length
      "
      aria-labelledby="recent-transfer-title"
    >
      <h3 id="recent-transfer-title" class="type-h4 mb-md">최근 보낸 사람</h3>
      <div
        v-if="recentRecipients.length"
        class="flex snap-x gap-md overflow-x-auto pb-xs"
      >
        <div
          v-for="recipient in recentRecipients"
          :key="recipient.id"
          class="flex w-36 shrink-0 snap-start flex-col items-center gap-sm"
        >
          <button
            class="flex w-full flex-col items-center gap-xs rounded-medium outline-none focus-visible:ring-2 focus-visible:ring-focus"
            type="button"
            :aria-label="`${recipient.name}에게 송금`"
            @click="selectRecipient(recipient)"
          >
            <span
              class="type-h2 flex size-20 items-center justify-center rounded-full border-2 border-primary-500 bg-surface-card"
            >
              {{ recipient.name.slice(0, 1) }}
            </span>
            <span class="type-h4">{{ recipient.name }}</span>
          </button>
          <button
            v-if="!isContact(recipient)"
            class="type-h4 flex min-h-touch-target w-full items-center justify-center gap-xs whitespace-nowrap rounded-full border-2 border-primary-500 bg-surface-card px-xs text-primary-500 outline-none focus-visible:ring-2 focus-visible:ring-focus"
            type="button"
            :aria-label="`${recipient.name} 안심계좌 추가`"
            @click="openContactModal(recipient)"
          >
            <Plus
              class="size-lg shrink-0"
              :stroke-width="2.5"
              aria-hidden="true"
            />
            <span class="whitespace-nowrap">안심계좌 추가</span>
          </button>
          <span v-else class="min-h-touch-target" aria-hidden="true" />
        </div>
      </div>
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
    </section>

    <Input
      v-model="search"
      label="안심계좌 검색"
      placeholder="이름 또는 계좌번호 입력"
      large
    />

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

    <section aria-labelledby="safe-accounts-title">
      <h3 id="safe-accounts-title" class="type-h4 mb-md">안심계좌</h3>
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
        {{
          search.trim()
            ? '검색 결과가 없습니다.'
            : '등록된 안심계좌가 없습니다.'
        }}
      </p>
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
