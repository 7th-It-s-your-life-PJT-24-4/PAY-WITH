<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { useMutation, useQuery } from '@tanstack/vue-query'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { inquireTransferRecipient } from '@/api/transfers'
import { banksOptions } from '@/lib/query/bank'
import TransferErrorModal from '@/pages/ward/transfer/-components/TransferErrorModal.vue'
import TransferLoadingModal from '@/pages/ward/transfer/-components/TransferLoadingModal.vue'
import { prioritizeBanks } from '@/pages/ward/transfer/-utils/bank-order'
import { getTransferApiError } from '@/pages/ward/transfer/-utils/transfer-api-error'
import { isValidTransferAccountNumber } from '@/pages/ward/transfer/-utils/transfer-route-guard'
import { useTransferStore } from '@/stores/transfer.store'
import { getBankPresentation } from '@/utils/bank-presentation'

const router = useRouter()
const transferStore = useTransferStore()
const banksQuery = useQuery(banksOptions())
const recipientMutation = useMutation({ mutationFn: inquireTransferRecipient })
const selectedBankCode = ref('')
const banks = computed(() =>
  prioritizeBanks(banksQuery.data.value ?? [], transferStore.recommendedBanks),
)
const errorMessage = ref('')
const canValidateAccount = computed(() =>
  isValidTransferAccountNumber(transferStore.accountNumber),
)

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
      accountNo: transferStore.accountNumber.replaceAll('-', ''),
    })
    transferStore.setVerifiedRecipient(
      account.holderName,
      account.bankName,
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
  <div class="flex flex-col gap-xl">
    <section class="text-center">
      <h2 class="type-h1">은행 선택</h2>
      <p class="type-h4 mt-xs text-body-secondary">
        거래하실 은행을 선택해 주세요.
      </p>
    </section>

    <Button
      class="w-full"
      label="계좌번호 다시 입력"
      variant="outline-primary"
      size="default"
      @click="router.push({ name: 'ward-transfer-account' })"
    />

    <p
      v-if="banksQuery.isPending.value"
      class="type-body-medium py-xl text-center text-body-muted"
      role="status"
    >
      은행 목록을 불러오고 있습니다.
    </p>

    <section
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
    </section>

    <section
      v-else-if="banks.length"
      class="grid grid-cols-2 gap-md"
      aria-label="은행 목록"
    >
      <button
        v-for="bank in banks"
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
          <span v-else class="type-h3 text-white">
            {{ bank.bankName.slice(0, 1) }}
          </span>
        </span>
        {{ bank.bankName }}
      </button>
    </section>

    <section
      v-else
      class="rounded-large bg-surface-card p-xl text-center shadow-card"
      role="status"
    >
      <p class="type-h4">선택 가능한 은행이 없습니다.</p>
      <p class="type-body-medium mt-xs text-body-secondary">
        잠시 후 다시 시도해 주세요.
      </p>
    </section>

    <p class="type-h2 text-center">1/2</p>

    <Button
      class="w-full"
      size="large"
      :label="recipientMutation.isPending.value ? '계좌 확인 중' : '다음으로'"
      :disabled="
        !canValidateAccount ||
        banksQuery.isPending.value ||
        banksQuery.isError.value ||
        !selectedBankCode ||
        recipientMutation.isPending.value
      "
      @click="proceed"
    />

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
