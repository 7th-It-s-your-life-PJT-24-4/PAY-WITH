import type { ApprovalRequestDetail } from '@/schemas/approval.schema'
import type {
  GuardTransactionDetail,
  TransactionRiskLevel,
} from '@/schemas/transaction.schema'

export type GuardTransactionStatus = GuardTransactionDetail['status']

export interface GuardTransactionDetailView {
  type: GuardTransactionDetail['type']
  status: GuardTransactionStatus
  riskLevel: TransactionRiskLevel | null
  counterpartyName: string | null
  bankName: string | null
  accountNo: string | null
  amount: number
  occurredAt: string
  riskScore: number
  summary: string | null
  reasons: string[]
  failureReason: string | null
}

export interface GuardTransactionTitlePresentation {
  title: string
  titleClass: string
}

export const defaultTransferFailureReason =
  '송금이 완료되지 않았어요. 피보호자의 잔액과 거래 상태를 확인해 주세요.'

const defaultFailureReasonByType: Record<
  GuardTransactionDetail['type'],
  string
> = {
  TRANSFER: defaultTransferFailureReason,
  PAYMENT: '결제가 완료되지 않았어요. 피보호자의 거래 상태를 확인해 주세요.',
  CHARGE: '충전이 완료되지 않았어요. 계좌와 거래 상태를 확인해 주세요.',
}

const riskReasonLabels: Record<string, string> = {
  BL_FRAUD_ACCOUNT: '사기 신고 이력이 있는 계좌예요.',
  BL_REJECTED_RECIPIENT: '보호자가 이전에 거절한 수취인 계좌예요.',
  DIVISION_TRANSFER: '짧은 시간에 나누어 송금한 패턴이 감지됐어요.',
  HIGH_AMOUNT_L1: '평소보다 큰 금액의 송금이에요.',
  HIGH_AMOUNT_L2: '고액 송금 패턴이 감지됐어요.',
  HIGH_AMOUNT_L3: '매우 큰 금액의 송금이에요.',
  NEW_RECIPIENT: '처음 송금하는 수취인이에요.',
  NIGHT_TIME_DEEP: '심야 시간대의 송금이에요.',
  NIGHT_TIME_LATE: '늦은 시간대의 송금이에요.',
  PENDING_APPROVAL_EXISTS: '승인 대기 중에 추가로 요청한 송금이에요.',
  REPEATED: '짧은 시간에 반복된 송금이에요.',
  SAFE_ACCOUNT_CHECK: '등록한 안전계좌로 보내는 송금이에요.',
  SUSPICIOUS_MEMO: '메모에서 위험 키워드가 감지됐어요.',
  PAY_SPLIT_PAYMENT: '짧은 시간 안에 여러 번 나누어 결제했어요.',
  PAY_HIGH_AMOUNT_L3: '평소보다 매우 큰 금액의 결제예요.',
  PAY_HIGH_AMOUNT_L2: '평소보다 큰 금액의 결제예요.',
  PAY_HIGH_AMOUNT_L1: '일반적인 생활 결제보다 큰 금액이에요.',
  PAY_RISKY_CATEGORY: '환금성이 높은 위험 업종에서 결제했어요.',
  PAY_PENDING_APPROVAL: '승인 대기 중인 송금이 있는 상태에서 결제했어요.',
  PAY_GIFT_CARD_AMOUNT: '상품권 의심 단위 금액으로 결제했어요.',
  PAY_NIGHT_DEEP: '자정 이후 늦은 시간에 결제했어요.',
  PAY_NIGHT_LATE: '늦은 밤 시간대에 결제했어요.',
  PAY_IMPOSSIBLE_TRAVEL: '물리적으로 이동하기 어려운 위치에서 결제했어요.',
}

export function maskGuardAccountNumber(accountNo: string | null) {
  if (!accountNo) return '-'

  const digits = accountNo.replaceAll(/\D/g, '')
  if (digits.length <= 4) return digits || accountNo

  const prefixLength = digits.length >= 7 ? 3 : Math.max(1, digits.length - 4)
  return `${digits.slice(0, prefixLength)}-***-${digits.slice(-4)}`
}

export function createGuardTransactionDetailView(
  detail: GuardTransactionDetail,
  failureReason: string | null = null,
): GuardTransactionDetailView {
  return {
    type: detail.type,
    status: detail.status,
    riskLevel: detail.riskLevel,
    counterpartyName: detail.counterpartyName,
    bankName: detail.bankName,
    accountNo: detail.accountNo,
    amount: detail.amount,
    occurredAt: detail.occurredAt,
    riskScore: detail.riskAnalysis?.riskScore ?? 0,
    summary: detail.riskAnalysis?.summary ?? null,
    reasons: (detail.riskAnalysis?.reasons ?? []).map(
      (reason) => riskReasonLabels[reason] ?? reason,
    ),
    failureReason:
      detail.status === 'FAILED'
        ? failureReason || defaultFailureReasonByType[detail.type]
        : null,
  }
}

export function createApprovalTransactionDetailView(
  detail: ApprovalRequestDetail,
): GuardTransactionDetailView {
  return {
    type: 'TRANSFER',
    status: 'HELD',
    riskLevel: detail.riskLevel,
    counterpartyName: detail.holderName,
    bankName: detail.bankName,
    accountNo: detail.accountNo,
    amount: detail.amount,
    occurredAt: detail.requestedAt,
    riskScore: detail.totalScore ?? 0,
    summary: null,
    reasons: detail.ruleHits.map(({ description }) => description),
    failureReason: null,
  }
}

export function getGuardTransactionTitlePresentation(
  detail: GuardTransactionDetailView,
): GuardTransactionTitlePresentation {
  if (detail.status === 'FAILED') {
    const title =
      detail.type === 'TRANSFER'
        ? '거래를 승인했지만 송금에 실패했어요'
        : detail.type === 'PAYMENT'
          ? '결제에 실패했어요'
          : '충전에 실패했어요'
    return {
      title,
      titleClass: 'text-error',
    }
  }
  if (detail.status === 'REJECTED') {
    return { title: '거절된 이상 거래에요', titleClass: 'text-error' }
  }
  if (detail.status === 'CANCELED') {
    return { title: '취소된 이상 거래에요', titleClass: 'text-error' }
  }
  if (detail.status === 'BLOCKED') {
    return { title: '차단된 이상 거래에요', titleClass: 'text-error' }
  }
  if (detail.riskLevel === 'DANGER' && detail.status === 'COMPLETED') {
    return {
      title:
        detail.type === 'TRANSFER'
          ? '승인되어 송금이 완료된 거래에요'
          : '위험 거래가 완료됐어요',
      titleClass: 'text-primary-500',
    }
  }
  if (
    detail.riskLevel === 'DANGER' &&
    (detail.status === 'APPROVED' || detail.status === 'PROCESSING')
  ) {
    return {
      title:
        detail.type === 'TRANSFER'
          ? '승인되어 송금을 처리하고 있어요'
          : '거래를 처리하고 있어요',
      titleClass: 'text-primary-500',
    }
  }
  if (detail.riskLevel === 'CAUTION') {
    return { title: '주의가 필요한 거래에요', titleClass: 'text-warning' }
  }
  if (detail.riskLevel === 'SAFE') {
    return { title: '안심할 수 있는 거래에요', titleClass: 'text-success' }
  }
  return { title: '이상 거래가 발생했어요', titleClass: 'text-error' }
}
