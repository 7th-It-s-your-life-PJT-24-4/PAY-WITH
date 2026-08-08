import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import {
  getWardTransactionDetail,
  getWardTransactionHistory,
  type WardTransactionHistoryParams,
} from '@/api/transaction-history'
import type { WardTransaction } from '@/types/transaction'

export const transactionHistoryKeys = {
  all: ['transaction-history'] as const,
  ward: (params: WardTransactionHistoryParams) =>
    [...transactionHistoryKeys.all, 'ward', params] as const,
  wardDetail: (transactionId: number) =>
    [...transactionHistoryKeys.all, 'ward', 'detail', transactionId] as const,
}

export function wardTransactionHistoryOptions(
  params: MaybeRefOrGetter<WardTransactionHistoryParams>,
) {
  const resolvedParams = computed(() => toValue(params))

  return queryOptions({
    queryKey: computed(() => transactionHistoryKeys.ward(resolvedParams.value)),
    queryFn: () => getWardTransactionHistory(resolvedParams.value),
    select: (data) => ({
      ...data,
      transactions: data.transactions.filter((tx) => tx.status !== 'HELD'),
    }),
  })
}

export function wardTransactionDetailOptions(
  transactionId: MaybeRefOrGetter<number>,
) {
  const resolvedTransactionId = computed(() => toValue(transactionId))

  return queryOptions({
    queryKey: computed(() =>
      transactionHistoryKeys.wardDetail(resolvedTransactionId.value),
    ),
    queryFn: () => getWardTransactionDetail(resolvedTransactionId.value),
    enabled: computed(() => Number.isSafeInteger(resolvedTransactionId.value)),
    select: toWardTransaction,
  })
}

const riskReasonMessages: Record<string, string> = {
  BL_FRAUD_ACCOUNT: '사기 신고 이력이 있는 계좌예요.',
  BL_REJECTED_RECIPIENT: '보호자가 이전에 거절한 수취인 계좌예요.',
  DIVISION_TRANSFER: '짧은 시간 안에 여러 계좌로 나누어 송금했어요.',
  HIGH_AMOUNT_L1: '일반적인 생활 송금보다 큰 금액이에요.',
  HIGH_AMOUNT_L2: '평소보다 큰 금액을 송금했어요.',
  HIGH_AMOUNT_L3: '평소보다 매우 큰 금액을 송금했어요.',
  NEW_RECIPIENT: '처음 송금하는 상대에게 보낸 거래예요.',
  NIGHT_TIME_DEEP: '자정 이후 늦은 시간에 요청된 송금이에요.',
  NIGHT_TIME_LATE: '늦은 밤 시간대에 요청된 송금이에요.',
  PENDING_APPROVAL_EXISTS:
    '승인 대기 중인 송금이 있는 상태에서 추가 송금했어요.',
  REPEATED: '짧은 시간 안에 반복해서 송금했어요.',
  SAFE_ACCOUNT_CHECK: '등록된 안전계좌 여부가 위험도 판단에 반영됐어요.',
  SUSPICIOUS_MEMO: '거래 메모에 주의가 필요한 단어가 포함됐어요.',
}

function toWardTransaction(
  detail: Awaited<ReturnType<typeof getWardTransactionDetail>>,
): WardTransaction {
  const riskAnalysis = detail.riskAnalysis
  const title = detail.counterpartyName ?? '거래 상대'

  return {
    transactionId: detail.transactionId,
    type: detail.type,
    direction: detail.direction,
    title,
    amount: detail.amount,
    balanceAfter: detail.balanceAfter,
    occurredAt: detail.occurredAt,
    methodLabel: detail.bankName ?? getDefaultMethodLabel(detail.type),
    memo: detail.memo,
    status: detail.status,
    riskLevel: detail.riskLevel,
    riskScore: riskAnalysis?.riskScore ?? 0,
    riskSummary:
      riskAnalysis?.summary ??
      getDefaultRiskSummary(detail.riskLevel, detail.status),
    riskReasons: riskAnalysis?.reasons.map(formatRiskReason) ?? [],
    payment:
      detail.type === 'PAYMENT'
        ? {
            merchantName: title,
          }
        : undefined,
    transfer:
      detail.type === 'TRANSFER'
        ? {
            holderName: title,
            bankName: detail.bankName ?? '은행 정보 없음',
            accountNo: detail.accountNo ?? '',
          }
        : undefined,
  }
}

function getDefaultMethodLabel(type: string) {
  if (type === 'CHARGE') return '충전'
  if (type === 'PAYMENT') return '결제'
  return '송금'
}

function getDefaultRiskSummary(riskLevel: string | null, status: string) {
  if (status === 'BLOCKED') return '시스템이 차단한 거래입니다.'
  if (status === 'REJECTED') return '보호자가 거절한 거래입니다.'
  if (riskLevel === 'DANGER')
    return '위험한 거래로 판단되었으나 진행된 거래입니다.'
  if (riskLevel === 'CAUTION') return '주의가 필요한 거래로 분류됐어요.'
  if (riskLevel === 'SAFE') return '안전한 거래로 확인됐어요.'
  return '위험 평가 대상이 아닌 거래예요.'
}

function formatRiskReason(reason: string) {
  return (
    riskReasonMessages[reason] ?? '추가 확인이 필요한 거래 패턴이 감지됐어요.'
  )
}
