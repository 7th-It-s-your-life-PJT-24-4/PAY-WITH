import type { RecipientHistoryItem } from '@/schemas/transfer.schema'
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
