import type { RecipientHistoryItem } from '@/schemas/transfer.schema'
import type { WardSafeAccount } from '@/schemas/ward-safe-account.schema'
import type { TransferRecipient } from '@/stores/transfer.store'

export function toTransferRecipient(
  recipient: RecipientHistoryItem,
): TransferRecipient {
  return {
    id: recipient.recipientId,
    name: recipient.accountAlias || recipient.holderName,
    holderName: recipient.holderName,
    bankCode: recipient.bankCode,
    bank: recipient.bankName,
    accountNumber: recipient.accountNo,
    isContact: recipient.isRegisteredSafe,
  }
}

export function toSafeAccountTransferRecipient(
  safeAccount: WardSafeAccount,
): TransferRecipient {
  return {
    id: safeAccount.recipientId ?? safeAccount.safeAccountId,
    name: safeAccount.accountAlias || safeAccount.holderName,
    holderName: safeAccount.holderName,
    bankCode: safeAccount.bankCode,
    bank: safeAccount.bankName,
    accountNumber: safeAccount.accountNo,
    isContact: true,
  }
}
