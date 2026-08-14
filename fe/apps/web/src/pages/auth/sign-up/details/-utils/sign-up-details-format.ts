export function digitsOnly(value: string, maxLength: number) {
  return value.replace(/\D/g, '').slice(0, maxLength)
}

export function formatPhoneNumber(value: string) {
  const digits = digitsOnly(value, 11)

  if (digits.length <= 3) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`

  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

export function formatBirthDate(value: string) {
  const digits = digitsOnly(value, 8)

  if (digits.length <= 4) return digits
  if (digits.length <= 6) return `${digits.slice(0, 4)}.${digits.slice(4)}`

  return `${digits.slice(0, 4)}.${digits.slice(4, 6)}.${digits.slice(6)}`
}
