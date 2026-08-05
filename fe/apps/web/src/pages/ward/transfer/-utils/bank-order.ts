import type { Bank } from '@/schemas/bank.schema'

export function prioritizeBanks(
  banks: Bank[],
  recommendedBanks: Bank[],
): Bank[] {
  const priorityByBankCode = new Map(
    recommendedBanks.map(({ bankCode }, index) => [bankCode, index]),
  )
  const defaultPriority = recommendedBanks.length

  return [...banks].sort((left, right) => {
    const leftPriority =
      priorityByBankCode.get(left.bankCode) ?? defaultPriority
    const rightPriority =
      priorityByBankCode.get(right.bankCode) ?? defaultPriority
    return leftPriority - rightPriority
  })
}
