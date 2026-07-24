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

  return { recipientName: '김준호', bank, accountNumber }
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
