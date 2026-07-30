export interface GuardSeniorAvatar {
  id: string
  name: string
  imageUrl?: string
}

export interface GuardTransaction {
  id: string
  date: string
  amount: string
  description: string
  category: 'transfer' | 'payment'
  status: 'safe'
}

export const mockGuardSeniors: GuardSeniorAvatar[] = [
  {
    id: 'sui',
    name: '수이',
    imageUrl: '/images/mocks/sui.png',
  },
  {
    id: 'woni',
    name: '원이',
    imageUrl: '/images/mocks/one.png',
  },
]

export const mockGuardTransactions: GuardTransaction[] = [
  {
    id: 'tx-1',
    date: '7월 28일',
    amount: '-30,000원',
    description: '수이 계좌 → 안유진',
    category: 'transfer',
    status: 'safe',
  },
  {
    id: 'tx-2',
    date: '7월 28일',
    amount: '-35,000원',
    description: '무신사 스탠다드',
    category: 'payment',
    status: 'safe',
  },
  {
    id: 'tx-3',
    date: '7월 25일',
    amount: '-30,000원',
    description: '수이 계좌 → 장원영',
    category: 'transfer',
    status: 'safe',
  },
]
