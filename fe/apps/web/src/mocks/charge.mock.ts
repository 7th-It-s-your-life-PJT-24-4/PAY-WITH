import type {
  ChargeAccount,
  ChargeResult,
  RegisterChargeAccountRequest,
} from '@/types/charge'

const wait = (milliseconds: number) =>
  new Promise((resolve) => setTimeout(resolve, milliseconds))

const initialAccounts = (): ChargeAccount[] => [
  {
    accountId: 7,
    bankCode: 'KB',
    bankName: 'KB국민은행',
    accountNumber: '12345612123456',
  },
  {
    accountId: 9,
    bankCode: 'NH',
    bankName: '농협은행',
    accountNumber: '2345634234567',
  },
]

let nextAccountId = 10
let nextTransactionId = 43
const accounts = initialAccounts()

const chargeResults = new Map<number, ChargeResult>()

export function resetMockChargeState() {
  nextAccountId = 10
  nextTransactionId = 43
  accounts.splice(0, accounts.length, ...initialAccounts())
  chargeResults.clear()
}

export async function getMockChargeAccounts() {
  await wait(150)
  return accounts.map((account) => ({ ...account }))
}

export async function registerMockChargeAccount(
  request: RegisterChargeAccountRequest,
) {
  await wait(300)
  const bankNames: Record<string, string> = {
    KB: 'KB국민은행',
    NH: '농협은행',
    SHINHAN: '신한은행',
    WOORI: '우리은행',
    HANA: '하나은행',
  }
  const account: ChargeAccount = {
    accountId: nextAccountId++,
    bankCode: request.bankCode,
    bankName: bankNames[request.bankCode] ?? request.bankCode,
    accountNumber: request.accountNumber,
  }
  accounts.unshift(account)
  return { ...account }
}

export async function submitMockCharge(account: ChargeAccount, amount: number) {
  await wait(500)
  const result: ChargeResult = {
    transactionId: nextTransactionId++,
    status: 'COMPLETED',
    chargedAmount: amount,
    balanceAfter: 100_000 + amount,
    bankName: account.bankName,
    accountNumber: account.accountNumber,
    createdAt: new Date().toISOString(),
  }
  chargeResults.set(result.transactionId, result)

  const usedIndex = accounts.findIndex(
    ({ accountId }) => accountId === account.accountId,
  )
  if (usedIndex > 0) {
    const [usedAccount] = accounts.splice(usedIndex, 1)
    if (usedAccount) accounts.unshift(usedAccount)
  }
  return { ...result }
}

export async function getMockChargeResult(transactionId: number) {
  await wait(100)
  const result = chargeResults.get(transactionId)
  if (!result) throw new Error('충전 내역을 찾을 수 없습니다.')
  return { ...result }
}
