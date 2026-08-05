import type { Bank } from '@/schemas/bank.schema'

const preferredBankCodes = [
  '004', // KB국민은행
  '088', // 신한은행
  '020', // 우리은행
  '081', // 하나은행
  '011', // NH농협은행
  '003', // IBK기업은행
  '090', // 카카오뱅크
  '092', // 토스뱅크
  '089', // 케이뱅크
] as const

export function prioritizeBanks(
  banks: Bank[],
  recommendedBanks: Bank[],
): Bank[] {
  const recommendedPriorityByBankCode = new Map(
    recommendedBanks.map(({ bankCode }, index) => [bankCode, index]),
  )
  const preferredPriorityByBankCode = new Map<string, number>(
    preferredBankCodes.map((bankCode, index) => [bankCode, index]),
  )
  const preferredPriorityOffset = recommendedBanks.length
  const defaultPriority = preferredPriorityOffset + preferredBankCodes.length

  function getPriority(bankCode: string) {
    const recommendedPriority = recommendedPriorityByBankCode.get(bankCode)
    if (recommendedPriority !== undefined) return recommendedPriority

    const preferredPriority = preferredPriorityByBankCode.get(bankCode)
    if (preferredPriority !== undefined) {
      return preferredPriorityOffset + preferredPriority
    }

    return defaultPriority
  }

  return [...banks].sort((left, right) => {
    return getPriority(left.bankCode) - getPriority(right.bankCode)
  })
}
