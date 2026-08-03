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
  category: 'charge' | 'transfer' | 'payment'
  status: 'safe' | 'warning' | 'danger'
}

export interface GuardTransactionDetail {
  amount: number
  merchantName: string
  withdrawalAccountLabel: string
  occurredAt: string
  riskScore: number
  analysisResults: string[]
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
    amount: '+50,000원',
    description: '국민 3700 충전',
    category: 'charge',
    status: 'safe',
  },
  {
    id: 'tx-2',
    date: '7월 28일',
    amount: '-30,000원',
    description: '수이 계좌 → 안유진',
    category: 'transfer',
    status: 'safe',
  },
  {
    id: 'tx-3',
    date: '7월 28일',
    amount: '-35,000원',
    description: '무신사 스탠다드',
    category: 'payment',
    status: 'safe',
  },
  {
    id: 'tx-4',
    date: '7월 25일',
    amount: '-30,000원',
    description: '수이 계좌 → 장원영',
    category: 'transfer',
    status: 'safe',
  },
  {
    id: 'tx-5',
    date: '7월 25일',
    amount: '-30,000원',
    description: '수이 계좌 → 안유진',
    category: 'transfer',
    status: 'warning',
  },
  {
    id: 'tx-6',
    date: '7월 25일',
    amount: '-35,000원',
    description: '무신사 스탠다드',
    category: 'payment',
    status: 'danger',
  },
  {
    id: 'tx-7',
    date: '7월 24일',
    amount: '-30,000원',
    description: '수이 계좌 → 안유진',
    category: 'transfer',
    status: 'warning',
  },
  {
    id: 'tx-8',
    date: '7월 24일',
    amount: '-35,000원',
    description: '무신사 스탠다드',
    category: 'payment',
    status: 'danger',
  },
]

const transactionDetailsByStatus: Record<
  GuardTransaction['status'],
  GuardTransactionDetail
> = {
  danger: {
    amount: 30_000,
    merchantName: '토끼정 타임스퀘어점',
    withdrawalAccountLabel: '국민 3700',
    occurredAt: '2026년 7월 29일 10:18',
    riskScore: 87,
    analysisResults: [
      '평소 자주 이용하지 않던 사용처에요.',
      '최근 평균보다 큰 금액의 거래에요.',
      '평소와 다른 패턴으로 감지되었어요.',
    ],
  },
  warning: {
    amount: 30_000,
    merchantName: '토끼정 타임스퀘어점',
    withdrawalAccountLabel: '국민 3700',
    occurredAt: '2026년 7월 29일 10:18',
    riskScore: 50,
    analysisResults: [
      '처음 이용하는 사용처에요.',
      '최근 평균보다 비슷한 금액의 거래에요.',
      '평소와 비슷한 패턴으로 감지되었어요.',
    ],
  },
  safe: {
    amount: 30_000,
    merchantName: '토끼정 타임스퀘어점',
    withdrawalAccountLabel: '국민 3700',
    occurredAt: '2026년 7월 29일 10:18',
    riskScore: 5,
    analysisResults: [
      '평소 자주 이용하던 사용처에요.',
      '최근 평균과 비슷한 거래에요.',
      '평소와 비슷한 패턴으로 감지되었어요.',
    ],
  },
}

export function getMockGuardTransaction(transactionId: string) {
  return mockGuardTransactions.find(({ id }) => id === transactionId)
}

export function getMockGuardTransactionDetail(transactionId: string) {
  const transaction = getMockGuardTransaction(transactionId)
  if (!transaction) return null

  return transactionDetailsByStatus[transaction.status]
}
