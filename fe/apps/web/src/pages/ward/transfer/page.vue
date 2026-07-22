<script setup lang="ts">
import { Button, Input } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import TransferRecipientCard from '@/pages/ward/transfer/-components/TransferRecipientCard.vue'
import {
  useTransferStore,
  type TransferRecipient,
} from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const search = ref('')

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

const filteredRecipients = computed(() => {
  const keyword = search.value.trim()
  if (!keyword) return recipients
  return recipients.filter((item) =>
    `${item.name}${item.bank}${item.accountNumber}`.includes(keyword),
  )
})

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
      <div class="flex gap-md overflow-x-auto pb-xs">
        <button
          v-for="recipient in recipients.slice(0, 4)"
          :key="recipient.id"
          class="flex w-20 shrink-0 flex-col items-center gap-xs"
          type="button"
          @click="selectRecipient(recipient)"
        >
          <span
            class="type-h2 flex size-20 items-center justify-center rounded-full border-2 border-primary-500 bg-surface-card"
          >
            {{ recipient.name.slice(0, 1) }}
          </span>
          <span class="type-h4">{{ recipient.name }}</span>
        </button>
      </div>
    </section>

    <Input
      v-model="search"
      label="연락처 검색"
      placeholder="이름 또는 계좌번호 입력"
      large
    />

    <Button
      class="w-full"
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
  </div>
</template>
