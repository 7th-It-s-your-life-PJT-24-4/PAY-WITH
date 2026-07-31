export interface TransferBank {
  code: string
  name: string
}

export const transferBanks: TransferBank[] = [
  { code: '004', name: 'KB국민은행' },
  { code: '020', name: '우리은행' },
  { code: '081', name: '하나은행' },
  { code: '011', name: 'NH농협' },
  { code: '088', name: '신한은행' },
  { code: '090', name: '카카오뱅크' },
]

export function getTransferBankCode(bankName: string) {
  return (
    transferBanks.find(
      ({ name }) =>
        name === bankName ||
        name.replace('KB', '') === bankName ||
        name.replace('NH', '') === bankName,
    )?.code ?? null
  )
}
