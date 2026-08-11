<script setup lang="ts">
import { Button, WardToast } from '@pay-with/ui'
import { useMutation, useQuery } from '@tanstack/vue-query'
import { ChevronLeft, ChevronRight } from '@lucide/vue'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { filterBanks } from '@/api/banks'
import { getApiErrorMessage } from '@/api/error'
import { inquireTransferRecipient } from '@/api/transfers'
import { useEnsureFocusedInputVisible } from '@/composables/useEnsureFocusedInputVisible'
import { banksOptions } from '@/lib/query/bank'
import TransferErrorModal from '@/pages/ward/transfer/-components/TransferErrorModal.vue'
import TransferLoadingModal from '@/pages/ward/transfer/-components/TransferLoadingModal.vue'
import { prioritizeBanks } from '@/pages/ward/transfer/-utils/bank-order'
import { getTransferApiError } from '@/pages/ward/transfer/-utils/transfer-api-error'
import { isValidTransferAccountNumber } from '@/pages/ward/transfer/-utils/transfer-route-guard'
import { useTransferStore } from '@/stores/transfer.store'
import type { Bank } from '@/schemas/bank.schema'
import { getBankPresentation } from '@/utils/bank-presentation'

const router = useRouter()
const transferStore = useTransferStore()
const banksQuery = useQuery(banksOptions())
const bankFilterMutation = useMutation({ mutationFn: filterBanks })
const recipientMutation = useMutation({ mutationFn: inquireTransferRecipient })
const accountNumberInput = ref<HTMLElement | null>(null)
const selectedBankCode = ref('')
const currentPage = ref(1)
const filteredBanks = ref<Bank[]>([])
const errorMessage = ref('')
const filterErrorMessage = ref('')
const toastOpen = ref(false)
const toastMessage = ref('')
const banksPerPage = 6
let filterTimer: ReturnType<typeof setTimeout> | undefined
let filterRequestId = 0

useEnsureFocusedInputVisible(accountNumberInput)

const banks = computed(() => prioritizeBanks(banksQuery.data.value ?? [], []))
const pageCount = computed(() =>
  Math.max(1, Math.ceil(banks.value.length / banksPerPage)),
)
const visibleBanks = computed(() => {
  const startIndex = (currentPage.value - 1) * banksPerPage
  return banks.value.slice(startIndex, startIndex + banksPerPage)
})
const canValidateAccount = computed(() =>
  isValidTransferAccountNumber(transferStore.accountNumber),
)

function handleAccountInput(event: Event) {
  const target = event.target as HTMLInputElement
  const digits = target.value.replace(/\D/g, '')
  transferStore.accountNumber = digits.slice(0, 16)
  target.value = transferStore.accountNumber
}

watch(
  () => transferStore.accountNumber,
  (accountNumber) => {
    selectedBankCode.value = ''
    currentPage.value = 1
    filterErrorMessage.value = ''
    filteredBanks.value = []
    filterRequestId += 1
    if (filterTimer) clearTimeout(filterTimer)
    if (!isValidTransferAccountNumber(accountNumber)) return

    const requestId = filterRequestId
    filterTimer = setTimeout(async () => {
      try {
        const banks = await bankFilterMutation.mutateAsync({
          accountNo: accountNumber,
        })
        if (requestId === filterRequestId) filteredBanks.value = banks
      } catch (error) {
        if (requestId !== filterRequestId) return
        filterErrorMessage.value = await getApiErrorMessage(
          error,
          '은행 정보를 불러오지 못했습니다. 다시 시도해 주세요.',
        )
      }
    }, 300)
  },
)

onBeforeUnmount(() => {
  if (filterTimer) clearTimeout(filterTimer)
})

function handleProceedClick() {
  if (!canValidateAccount.value || !selectedBankCode.value) {
    toastMessage.value = !canValidateAccount.value
      ? '계좌번호 8자리 이상을 입력해 주세요'
      : '은행을 선택해 주세요'
    toastOpen.value = true
    return
  }
  void proceed()
}

async function proceed() {
  if (
    !canValidateAccount.value ||
    !selectedBankCode.value ||
    recipientMutation.isPending.value
  )
    return
  errorMessage.value = ''
  const selectedBank = banks.value.find(
    ({ bankCode }) => bankCode === selectedBankCode.value,
  )
  if (!selectedBank) {
    errorMessage.value = '은행 정보를 확인하지 못했습니다.'
    return
  }
  try {
    const account = await recipientMutation.mutateAsync({
      bankCode: selectedBank.bankCode,
      accountNo: transferStore.accountNumber,
    })
    const resolvedBankName =
      account.bankName === '알 수 없는 은행'
        ? selectedBank.bankName
        : account.bankName
    transferStore.setVerifiedRecipient(
      account.holderName,
      resolvedBankName,
      account.bankCode,
    )
    await router.push({ name: 'ward-transfer-amount' })
  } catch (error) {
    errorMessage.value = (
      await getTransferApiError(error, '계좌를 확인하지 못했습니다.')
    ).message
  }
}
</script>

<template>
  <div class="flex flex-col gap-lg">
    <section class="text-center">
      <h2 class="type-h1">계좌 번호 입력</h2>
    </section>

    <div class="flex flex-col gap-xs">
      <label for="transfer-account-number" class="sr-only">계좌 번호</label>
      <input
        id="transfer-account-number"
        ref="accountNumberInput"
        class="h-[72px] w-full rounded-medium border border-border-strong bg-surface-card px-md text-body outline-none transition-colors placeholder:font-sans placeholder:text-[20px] placeholder:font-semibold placeholder:leading-none placeholder:tracking-[-0.4px] placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
        :class="
          transferStore.accountNumber
            ? 'type-numeric-input-large font-number leading-none'
            : ''
        "
        type="text"
        inputmode="numeric"
        maxlength="16"
        placeholder="숫자만 입력"
        :value="transferStore.accountNumber"
        @input="handleAccountInput"
      />
    </div>

    <p
      v-if="filterErrorMessage"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ filterErrorMessage }}
    </p>

    <section
      v-if="canValidateAccount"
      class="h-14"
      aria-label="계좌번호로 찾은 추천 은행"
    >
      <div class="flex h-full snap-x items-center gap-sm overflow-x-auto pb-xs">
        <button
          v-for="bank in filteredBanks"
          :key="bank.bankCode"
          class="type-body-medium flex min-h-touch-target shrink-0 snap-start items-center gap-xs rounded-full border bg-surface-card px-md py-sm"
          :class="
            selectedBankCode === bank.bankCode
              ? 'border-primary-500 bg-primary-900 text-primary-500 ring-2 ring-primary-500/20'
              : 'border-border text-body-secondary'
          "
          type="button"
          :aria-label="`${bank.bankName} 추천 은행`"
          @click="selectedBankCode = bank.bankCode"
        >
          <span
            :class="[
              'flex size-6 items-center justify-center overflow-hidden rounded-full',
              getBankPresentation(bank).brandClass,
            ]"
            aria-hidden="true"
          >
            <img
              v-if="getBankPresentation(bank).iconUrl"
              class="size-4 object-contain"
              :src="getBankPresentation(bank).iconUrl"
              alt=""
            />
            <span v-else class="type-caption text-white">
              {{ bank.bankName.slice(0, 1) }}
            </span>
          </span>
          {{ bank.bankName }}
        </button>
      </div>
    </section>

    <section v-if="canValidateAccount" aria-label="은행 목록">
      <div
        v-if="banksQuery.isPending.value"
        class="type-body-medium py-xl text-center text-body-muted"
        role="status"
      >
        은행 목록을 불러오고 있습니다.
      </div>
      <div
        v-else-if="banksQuery.isError.value"
        class="rounded-large bg-surface-card p-xl text-center shadow-card"
      >
        <p class="type-body-medium text-error" role="alert">
          은행 목록을 불러오지 못했습니다.
        </p>
        <Button
          class="mt-md"
          label="다시 시도"
          variant="outline-primary"
          @click="banksQuery.refetch()"
        />
      </div>
      <section
        v-else-if="banks.length"
        class="grid h-[calc(21rem+var(--spacing-md)+var(--spacing-md))] grid-cols-2 content-start gap-md"
      >
        <button
          v-for="bank in visibleBanks"
          :key="bank.bankCode"
          class="type-h4 flex h-28 flex-col items-center justify-center gap-sm rounded-large border bg-surface-card shadow-card"
          :class="
            selectedBankCode === bank.bankCode
              ? 'border-primary-500 ring-2 ring-primary-500/20'
              : 'border-border'
          "
          type="button"
          :disabled="recipientMutation.isPending.value"
          @click="selectedBankCode = bank.bankCode"
        >
          <span
            :class="[
              'flex size-12 items-center justify-center overflow-hidden rounded-full',
              getBankPresentation(bank).brandClass,
            ]"
            aria-hidden="true"
          >
            <img
              v-if="getBankPresentation(bank).iconUrl"
              class="size-8 object-contain"
              :src="getBankPresentation(bank).iconUrl"
              alt=""
            />
            <span v-else class="type-h3 text-white">{{
              bank.bankName.slice(0, 1)
            }}</span>
          </span>
          {{ bank.bankName }}
        </button>
      </section>
      <p
        v-else
        class="rounded-large bg-surface-card p-xl text-center type-h4"
        role="status"
      >
        선택 가능한 은행이 없습니다.
      </p>

      <nav
        v-if="banks.length"
        class="mt-md flex w-full items-center justify-between gap-md"
        aria-label="은행 목록 페이지"
      >
        <button
          class="flex size-12 items-center justify-center rounded-full border-2 border-primary-500 bg-surface-card text-primary-500 disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
          type="button"
          aria-label="이전 은행 목록"
          :disabled="currentPage === 1"
          @click="currentPage -= 1"
        >
          <ChevronLeft class="size-xl" aria-hidden="true" />
        </button>
        <p class="type-h2 min-w-12 text-center" aria-live="polite">
          {{ currentPage }}/{{ pageCount }}
        </p>
        <button
          class="flex size-12 items-center justify-center rounded-full border-2 border-primary-500 bg-surface-card text-primary-500 disabled:cursor-not-allowed disabled:opacity-[var(--opacity-disabled)]"
          type="button"
          aria-label="다음 은행 목록"
          :disabled="currentPage === pageCount"
          @click="currentPage += 1"
        >
          <ChevronRight class="size-xl" aria-hidden="true" />
        </button>
      </nav>
    </section>

    <p
      v-if="errorMessage"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>
    <Button
      class="w-full"
      size="large"
      :label="recipientMutation.isPending.value ? '계좌 확인 중' : '다음으로'"
      :disabled="
        !canValidateAccount ||
        !selectedBankCode ||
        banksQuery.isPending.value ||
        banksQuery.isError.value ||
        recipientMutation.isPending.value
      "
      @click="handleProceedClick"
    />

    <WardToast v-model:open="toastOpen" :message="toastMessage" />
    <TransferLoadingModal
      :open="recipientMutation.isPending.value"
      title="계좌를 확인하고 있습니다"
      description="선택한 은행과 계좌번호를 확인하고 있습니다."
    />
    <TransferErrorModal
      :open="Boolean(errorMessage)"
      :description="errorMessage"
      @retry="proceed"
      @cancel="errorMessage = ''"
    />
  </div>
</template>
