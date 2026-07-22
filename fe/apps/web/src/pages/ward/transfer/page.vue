<script setup lang="ts">
import { Button, Input } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import AddTransferContactModal from '@/pages/ward/transfer/-components/AddTransferContactModal.vue'
import TransferRecipientCard from '@/pages/ward/transfer/-components/TransferRecipientCard.vue'
import {
  useTransferStore,
  type TransferRecipient,
} from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const search = ref('')
const contactModalOpen = ref(false)
const pendingContact = ref<TransferRecipient | null>(null)

const recipients: TransferRecipient[] = [
  {
    id: 1,
    name: '김민수',
    bank: '국민은행',
    accountNumber: '432102-01-234567',
  },
  { id: 2, name: '박지연', bank: '신한은행', accountNumber: '110-234-567890' },
  {
    id: 3,
    name: '이영희',
    bank: '국민은행',
    accountNumber: '432102-02-321633',
  },
  {
    id: 4,
    name: '최철수',
    bank: '국민은행',
    accountNumber: '432102-01-567890',
  },
  { id: 5, name: '정미숙', bank: '우리은행', accountNumber: '1002-456-234567' },
]

const contacts = ref<TransferRecipient[]>(
  recipients.filter((recipient) => recipient.id !== 2),
)

const filteredRecipients = computed(() => {
  const keyword = search.value.trim()
  if (!keyword) return contacts.value
  return contacts.value.filter((item) =>
    `${item.name}${item.bank}${item.accountNumber}`.includes(keyword),
  )
})

function isContact(recipient: TransferRecipient) {
  return contacts.value.some((contact) => contact.id === recipient.id)
}

function openContactModal(recipient: TransferRecipient) {
  pendingContact.value = recipient
  contactModalOpen.value = true
}

function addContact(alias: string) {
  if (!pendingContact.value || isContact(pendingContact.value)) return

  contacts.value.push({
    ...pendingContact.value,
    name: alias || pendingContact.value.name,
  })
  contactModalOpen.value = false
  pendingContact.value = null
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

    <section aria-labelledby="recent-transfer-title">
      <h3 id="recent-transfer-title" class="type-h4 mb-md">최근 보낸 사람</h3>
      <div class="flex snap-x gap-md overflow-x-auto pb-xs">
        <div
          v-for="recipient in recipients.slice(0, 4)"
          :key="recipient.id"
          class="flex w-32 shrink-0 snap-start flex-col items-center gap-sm"
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
            class="type-h4 flex min-h-touch-target w-full items-center justify-center gap-xs rounded-full border-2 border-primary-500 bg-surface-card px-sm text-primary-500 outline-none focus-visible:ring-2 focus-visible:ring-focus"
            type="button"
            :aria-label="`${recipient.name} 연락처 추가`"
            @click="openContactModal(recipient)"
          >
            <span class="type-h2 leading-none" aria-hidden="true">+</span>
            <span>연락처 추가</span>
          </button>
          <span v-else class="min-h-touch-target" aria-hidden="true" />
        </div>
      </div>
    </section>

    <Input
      v-model="search"
      label="연락처 검색"
      placeholder="이름 또는 계좌번호 입력"
      large
    />

    <Button
      class="type-h2 w-full"
      label="계좌 번호 직접 입력하기"
      size="large"
      @click="router.push({ name: 'ward-transfer-account' })"
    />

    <section aria-labelledby="contacts-title">
      <h3 id="contacts-title" class="type-h4 mb-md">전체 연락처</h3>
      <div class="overflow-hidden rounded-large bg-surface-card shadow-card">
        <TransferRecipientCard
          v-for="recipient in filteredRecipients"
          :key="recipient.id"
          :recipient="recipient"
          @select="selectRecipient"
        />
      </div>
    </section>

    <AddTransferContactModal
      v-model:open="contactModalOpen"
      :recipient="pendingContact"
      @confirm="addContact"
    />
  </div>
</template>
