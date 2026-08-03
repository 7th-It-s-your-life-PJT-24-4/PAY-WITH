import type {
  TransferCancelResult,
  TransferDetail,
  TransferStatus,
} from '@/types/transfer'

const mockDelay = 500

const banks = [
  'KB국민은행',
  '우리은행',
  '하나은행',
  'NH농협',
  '신한은행',
  '카카오뱅크',
]

const bankCodes: Record<string, string> = {
  KB국민은행: '004',
  우리은행: '020',
  하나은행: '081',
  NH농협: '011',
  신한은행: '088',
  카카오뱅크: '090',
  국민은행: '004',
}

function wait() {
  return new Promise<void>((resolve) => window.setTimeout(resolve, mockDelay))
}

const initialMockTransfers = (): TransferDetail[] => [
  {
    transactionId: 73,
    status: 'COMPLETED',
    holderName: '김민수',
    bankCode: '004',
    bankName: '국민은행',
    accountNo: '432102-01-234567',
    amount: 50_000,
    memo: null,
    requestedAt: '2026-07-24T14:30:00+09:00',
    expiredAt: '2026-07-24T14:40:00+09:00',
    respondedAt: '2026-07-24T14:32:00+09:00',
    completedAt: '2026-07-24T14:32:01+09:00',
    balanceAfter: 1_200_000,
    riskAnalysis: { riskScore: 0, reasons: [] },
    failureCode: null,
    failureMessage: null,
  },
  {
    transactionId: 74,
    status: 'HELD',
    holderName: '김민수',
    bankCode: '004',
    bankName: '국민은행',
    accountNo: '432102-01-234567',
    amount: 50_000,
    memo: null,
    requestedAt: '2026-07-24T14:30:00+09:00',
    expiredAt: '2026-07-24T14:40:00+09:00',
    respondedAt: null,
    completedAt: null,
    balanceAfter: null,
    riskAnalysis: {
      riskScore: 80,
      reasons: [
        {
          ruleCode: 'HIGH_AMOUNT',
          description: '평소보다 큰 금액의 송금입니다.',
          score: 80,
        },
      ],
    },
    failureCode: null,
    failureMessage: null,
  },
  {
    transactionId: 75,
    status: 'REJECTED',
    holderName: '이지혜',
    bankCode: '004',
    bankName: 'KB국민은행',
    accountNo: '123123890123',
    amount: 500_000,
    memo: null,
    requestedAt: '2026-07-24T14:30:00+09:00',
    expiredAt: '2026-07-24T14:40:00+09:00',
    respondedAt: '2026-07-24T14:36:00+09:00',
    completedAt: null,
    balanceAfter: null,
    riskAnalysis: {
      riskScore: 80,
      reasons: [
        {
          ruleCode: 'NEW_RECIPIENT',
          description: '처음 송금하는 수취인입니다.',
          score: 80,
        },
      ],
    },
    failureCode: null,
    failureMessage: null,
  },
  {
    transactionId: 76,
    status: 'HELD',
    holderName: '박지연',
    bankCode: '088',
    bankName: '신한은행',
    accountNo: '110-234-567890',
    amount: 30_000,
    memo: null,
    requestedAt: '2026-07-24T15:00:00+09:00',
    expiredAt: '2026-07-24T15:10:00+09:00',
    respondedAt: null,
    completedAt: null,
    balanceAfter: null,
    riskAnalysis: {
      riskScore: 70,
      reasons: [
        {
          ruleCode: 'NEW_RECIPIENT',
          description: '처음 송금하는 수취인입니다.',
          score: 70,
        },
      ],
    },
    failureCode: null,
    failureMessage: null,
  },
  {
    transactionId: 77,
    status: 'EXPIRED',
    holderName: '박지연',
    bankCode: '088',
    bankName: '신한은행',
    accountNo: '110-234-567890',
    amount: 30_000,
    memo: null,
    requestedAt: '2026-07-24T15:00:00+09:00',
    expiredAt: '2026-07-24T15:10:00+09:00',
    respondedAt: null,
    completedAt: null,
    balanceAfter: null,
    riskAnalysis: null,
    failureCode: null,
    failureMessage: null,
  },
  {
    transactionId: 78,
    status: 'FAILED',
    holderName: '박지연',
    bankCode: '088',
    bankName: '신한은행',
    accountNo: '110-234-567890',
    amount: 30_000,
    memo: null,
    requestedAt: '2026-07-24T15:00:00+09:00',
    expiredAt: null,
    respondedAt: '2026-07-24T15:01:00+09:00',
    completedAt: null,
    balanceAfter: null,
    riskAnalysis: null,
    failureCode: 'INSUFFICIENT_BALANCE',
    failureMessage: '송금 가능한 잔액이 부족합니다.',
  },
  {
    transactionId: 79,
    status: 'CANCELED',
    holderName: '박지연',
    bankCode: '088',
    bankName: '신한은행',
    accountNo: '110-234-567890',
    amount: 30_000,
    memo: null,
    requestedAt: '2026-07-24T15:00:00+09:00',
    expiredAt: null,
    respondedAt: '2026-07-24T15:01:00+09:00',
    completedAt: null,
    balanceAfter: null,
    riskAnalysis: null,
    failureCode: null,
    failureMessage: null,
  },
]

const mockTransfers = new Map(
  initialMockTransfers().map((transfer) => [transfer.transactionId, transfer]),
)

export function setMockTransferDetail(detail: TransferDetail) {
  mockTransfers.set(detail.transactionId, structuredClone(detail))
}

export function resetMockTransferDetails() {
  mockTransfers.clear()
  for (const detail of initialMockTransfers())
    mockTransfers.set(detail.transactionId, detail)
}

export function getMockHeldTransfers() {
  return [...mockTransfers.values()]
    .filter((transfer) => transfer.status === 'HELD')
    .map((transfer) => structuredClone(transfer))
}

export async function getMockTransferDetail(transactionId: number) {
  await wait()
  const detail = mockTransfers.get(transactionId)
  if (!detail) throw new Error('송금 거래를 찾을 수 없습니다.')
  return structuredClone(detail)
}

export async function advanceMockTransferStatus(
  transactionId: number,
  status: TransferStatus,
) {
  const detail = mockTransfers.get(transactionId)
  if (!detail) throw new Error('송금 거래를 찾을 수 없습니다.')

  const respondedAt =
    status === 'COMPLETED' || status === 'REJECTED'
      ? '2026-07-24T14:36:00+09:00'
      : detail.respondedAt
  const next: TransferDetail = {
    ...detail,
    status,
    respondedAt,
    completedAt:
      status === 'COMPLETED' ? '2026-07-24T14:36:01+09:00' : detail.completedAt,
    balanceAfter: status === 'COMPLETED' ? 1_250_000 - detail.amount : null,
  }
  setMockTransferDetail(next)
  return structuredClone(next)
}

export async function cancelMockTransfer(
  transactionId: number,
): Promise<TransferCancelResult> {
  await wait()
  const detail = mockTransfers.get(transactionId)
  if (!detail) throw new Error('송금 거래를 찾을 수 없습니다.')
  if (detail.status !== 'HELD') {
    const error = new Error('승인 대기 중인 송금만 취소할 수 있습니다.')
    error.name = 'TRANSFER_008'
    throw error
  }

  setMockTransferDetail({ ...detail, status: 'CANCELED' })
  return { transactionId, status: 'CANCELED' }
}

export async function findMockBankCandidates(accountNumber: string) {
  await wait()
  if (accountNumber.endsWith('00000000'))
    throw new Error('은행을 찾지 못했습니다. 계좌번호를 다시 확인해 주세요.')

  const preferredIndex = Number(accountNumber.at(-1) ?? 0) % banks.length
  return [
    banks[preferredIndex],
    ...banks.filter((_, index) => index !== preferredIndex),
  ]
}

export function getMockBankCode(bankName: string) {
  return bankCodes[bankName] ?? '000'
}

export async function validateMockTransferAccount(
  bank: string,
  accountNumber: string,
) {
  await wait()
  if (accountNumber.endsWith('11111111'))
    throw new Error('계좌번호와 은행을 다시 확인해 주세요.')

  return { holderName: '김준호', bank, accountNo: accountNumber }
}

export async function submitMockTransfer(pin: string, idempotencyKey: string) {
  await wait()
  if (pin === '111111') throw new Error('비밀번호가 올바르지 않습니다.')
  if (pin === '000000') return { status: 'unknown' as const }
  if (pin === '222222')
    return {
      transactionId: 74,
      status: 'HELD' as const,
      idempotencyKey,
    }
  return {
    transactionId: 73,
    status: 'COMPLETED' as const,
    idempotencyKey,
  }
}

export async function confirmMockTransferStatus(idempotencyKey: string) {
  await wait()
  return {
    transactionId: 73,
    status: 'COMPLETED' as const,
    idempotencyKey,
  }
}
