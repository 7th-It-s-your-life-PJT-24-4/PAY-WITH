<script setup lang="ts">
import { ChevronDown, Landmark } from '@lucide/vue'
import { Button, WardToast } from '@pay-with/ui'
import { useMutation, useQuery } from '@tanstack/vue-query'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { filterBanks } from '@/api/banks'
import { getApiErrorMessage } from '@/api/error'
import { inquireTransferRecipient } from '@/api/transfers'
import { useEnsureFocusedInputVisible } from '@/composables/useEnsureFocusedInputVisible'
import { banksOptions } from '@/lib/query/bank'
import TransferBankBottomSheet from '@/pages/ward/transfer/-components/TransferBankBottomSheet.vue'
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
const filteredBanks = ref<Bank[]>([])
const bankBottomSheetOpen = ref(false)
const errorMessage = ref('')
const filterErrorMessage = ref('')
const toastOpen = ref(false)
const toastMessage = ref('')

let filterTimer: ReturnType<typeof setTimeout> | undefined
let filterRequestId = 0

useEnsureFocusedInputVisible(accountNumberInput)

const allBanks = computed(() =>
  prioritizeBanks(banksQuery.data.value ?? [], []),
)

const selectedBank = computed(() =>
  allBanks.value.find(({ bankCode }) => bankCode === selectedBankCode.value),
)

const canValidateAccount = computed(() =>
  isValidTransferAccountNumber(transferStore.accountNumber),
)

function handleAccountInput(event: Event) {
  const target = event.target as HTMLInputElement
  const digits = target.value.replace(/\D/g, '')
  transferStore.accountNumber = digits.slice(0, 16)
  target.value = transferStore.accountNumber
}

function handleSelectBank(bank: Bank) {
  selectedBankCode.value = bank.bankCode
}

watch(
  () => transferStore.accountNumber,
  (accountNumber) => {
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
        if (requestId === filterRequestId) {
          filteredBanks.value = banks
        }
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
  const bank = selectedBank.value
  if (!bank) {
    errorMessage.value = '은행 정보를 확인하지 못했습니다.'
    return
  }
  try {
    const account = await recipientMutation.mutateAsync({
      bankCode: bank.bankCode,
      accountNo: transferStore.accountNumber,
    })
    const resolvedBankName =
      account.bankName === '알 수 없는 은행' ? bank.bankName : account.bankName
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
  <div class="flex flex-1 flex-col justify-between gap-lg">
    <section class="text-center">
      <h2 class="type-h1">계좌 번호 입력</h2>
    </section>

    <!-- 입력 섹션 (계좌번호 입력 -> 은행 선택 버튼 -> 추천 은행 칩) -->
    <div class="flex flex-col gap-md">
      <!-- 1. 계좌번호 입력 -->
      <div class="flex flex-col gap-xxs">
        <label for="transfer-account-number" class="type-h4 text-body">
          계좌 번호
        </label>
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

      <!-- 2. 은행 선택 트리거 버튼 -->
      <div>
        <label class="type-h4 mb-xxs block text-body">은행</label>
        <button
          class="flex h-16 w-full items-center justify-between rounded-medium border bg-surface-card px-md shadow-card outline-none transition-all hover:border-primary-500 focus-visible:border-focus focus-visible:ring-2 focus-visible:ring-focus/20"
          :class="
            selectedBank
              ? 'border-border-strong text-body'
              : 'border-border-strong text-body-muted'
          "
          type="button"
          aria-label="은행 선택"
          aria-haspopup="dialog"
          :aria-expanded="bankBottomSheetOpen"
          @click="bankBottomSheetOpen = true"
        >
          <span v-if="selectedBank" class="flex items-center gap-sm">
            <span
              :class="[
                'flex size-9 items-center justify-center overflow-hidden rounded-full',
                getBankPresentation(selectedBank).brandClass,
              ]"
              aria-hidden="true"
            >
              <img
                v-if="getBankPresentation(selectedBank).iconUrl"
                class="size-6 object-contain"
                :src="getBankPresentation(selectedBank).iconUrl"
                alt=""
              />
              <span v-else class="type-caption font-bold text-white">
                {{ selectedBank.bankName.slice(0, 1) }}
              </span>
            </span>
            <span class="type-h3 font-semibold text-body">
              {{ selectedBank.bankName }}
            </span>
          </span>
          <span v-else class="flex items-center gap-sm text-body-muted">
            <span
              class="flex size-9 items-center justify-center rounded-full bg-disabled/60 text-primary-500"
              aria-hidden="true"
            >
              <Landmark class="size-lg" :stroke-width="2.25" />
            </span>
            <span class="type-h3 text-body-muted">은행을 선택해 주세요</span>
          </span>

          <ChevronDown
            class="size-xl text-body-secondary transition-transform"
            aria-hidden="true"
          />
        </button>
      </div>

      <!-- 3. 추천 은행 칩 (후보가 있을 때만 은행 선택칸 밑에 깔끔하게 노출) -->
      <div
        v-if="filteredBanks.length"
        class="flex flex-wrap gap-xs"
        role="group"
        aria-label="추천 은행 목록"
      >
        <button
          v-for="bank in filteredBanks"
          :key="bank.bankCode"
          class="type-body-medium flex min-h-touch-target items-center gap-xs rounded-full border bg-surface-card px-md py-sm transition-all focus-visible:ring-2 focus-visible:ring-focus"
          :class="
            selectedBankCode === bank.bankCode
              ? 'border-primary-500 bg-primary-900 text-primary-500 ring-2 ring-primary-500/20 font-semibold'
              : 'border-border text-body hover:border-border-strong'
          "
          type="button"
          :aria-label="`${bank.bankName} 추천 은행`"
          @click="handleSelectBank(bank)"
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
            <span v-else class="type-caption text-white font-bold">
              {{ bank.bankName.slice(0, 1) }}
            </span>
          </span>
          {{ bank.bankName }}
        </button>
      </div>

      <!-- 에러 메시지 -->
      <p
        v-if="filterErrorMessage"
        class="type-body-medium text-center text-error"
        role="alert"
      >
        {{ filterErrorMessage }}
      </p>
    </div>

    <!-- 하단 액션 버튼 및 모달 -->
    <div class="mt-auto flex flex-col gap-sm pt-md">
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
    </div>

    <!-- 전체 은행 바텀시트 -->
    <TransferBankBottomSheet
      v-model:open="bankBottomSheetOpen"
      :banks="allBanks"
      :selected-bank-code="selectedBankCode"
      @select="handleSelectBank"
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
