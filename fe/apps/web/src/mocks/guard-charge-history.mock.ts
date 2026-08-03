export interface GuardChargeHistory {
  id: string
  date: string
  amount: number
  wardName: string
  bankName: string
  accountSuffix: string
  createdAt: string
}

export const mockGuardChargeHistoriesBySeniorId: Record<
  string,
  GuardChargeHistory[]
> = {
  sui: [
    {
      id: 'charge-1',
      date: '7월 28일',
      amount: 30_000,
      wardName: '수이',
      bankName: '국민',
      accountSuffix: '3700',
      createdAt: '2026-07-29T10:18:00+09:00',
    },
    {
      id: 'charge-2',
      date: '7월 28일',
      amount: 30_000,
      wardName: '수이',
      bankName: '국민',
      accountSuffix: '3700',
      createdAt: '2026-07-28T15:42:00+09:00',
    },
    {
      id: 'charge-3',
      date: '7월 25일',
      amount: 30_000,
      wardName: '수이',
      bankName: '국민',
      accountSuffix: '3700',
      createdAt: '2026-07-25T11:08:00+09:00',
    },
  ],
  woni: [],
}

export function getMockGuardChargeHistory(chargeId: string) {
  return Object.values(mockGuardChargeHistoriesBySeniorId)
    .flat()
    .find(({ id }) => id === chargeId)
}
