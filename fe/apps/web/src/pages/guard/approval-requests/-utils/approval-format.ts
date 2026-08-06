export const approvalMoneyFormatter = new Intl.NumberFormat('ko-KR')

const approvalDateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: 'long',
  day: 'numeric',
})

const approvalDateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

export function formatApprovalMoney(amount: number) {
  return `${approvalMoneyFormatter.format(amount)}원`
}

export function formatApprovalListAmount(amount: number) {
  return `-${approvalMoneyFormatter.format(amount)}원`
}

export function formatApprovalDate(value: string) {
  return approvalDateFormatter.format(new Date(value))
}

export function formatApprovalDateTime(value: string) {
  return approvalDateTimeFormatter.format(new Date(value))
}

export function maskApprovalAccountNumber(accountNo: string | null) {
  if (!accountNo) return '-'
  const digits = accountNo.replaceAll(/\D/g, '')
  if (digits.length <= 4) return accountNo
  return `•••• ${digits.slice(-4)}`
}
