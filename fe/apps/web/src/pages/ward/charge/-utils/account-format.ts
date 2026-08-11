export function formatCompactAccount(
  bankName?: string | null,
  accountNo?: string | null,
): string {
  const bank = bankName?.trim() || ''
  const account = accountNo?.trim() || ''

  if (!bank && !account) return ''
  if (!account) return bank

  const digits = account.replace(/\D/g, '')
  const last4 = digits.length >= 4 ? digits.slice(-4) : account

  if (!bank) return last4
  return `${bank}(${last4})`
}
