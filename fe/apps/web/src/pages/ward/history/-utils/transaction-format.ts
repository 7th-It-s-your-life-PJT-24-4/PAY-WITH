import type {
  TransactionDirection,
  TransactionRiskLevel,
  TransactionStatus,
  TransactionType,
} from '@/types/transaction'

const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
})

const dateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

const timeFormatter = new Intl.DateTimeFormat('ko-KR', {
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

export function formatTransactionDate(value: string) {
  return dateFormatter.format(new Date(value))
}

export function formatTransactionDateTime(value: string) {
  return dateTimeFormatter.format(new Date(value))
}

export function formatTransactionTime(value: string) {
  return timeFormatter.format(new Date(value))
}

export function formatTransactionAmount(
  amount: number,
  direction: TransactionDirection | 'IN' | 'OUT',
) {
  const sign = direction === 'CREDIT' || direction === 'IN' ? '+' : '-'
  return `${sign}${amount.toLocaleString('ko-KR')}원`
}

export function formatTransactionBalance(balance: number) {
  return `${balance.toLocaleString('ko-KR')}원`
}

export function getTransactionTypeLabel(
  type: TransactionType,
  direction: TransactionDirection,
) {
  if (type === 'CHARGE') return '충전'
  if (type === 'PAYMENT') return '결제'
  return direction === 'CREDIT' || direction === 'IN' ? '받은 돈' : '보낸 돈'
}

export const transactionRiskLabel: Record<TransactionRiskLevel, string> = {
  SAFE: '안전',
  CAUTION: '주의',
  DANGER: '위험',
}

export function getTransactionRiskLabel(
  riskLevel: TransactionRiskLevel | null,
  status: TransactionStatus,
) {
  if (!riskLevel) return '위험 평가 없음'
  return status === 'BLOCKED'
    ? '시스템 차단됨'
    : transactionRiskLabel[riskLevel]
}
