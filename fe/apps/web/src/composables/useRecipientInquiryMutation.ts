import { useMutation } from '@tanstack/vue-query'

import { inquireTransferRecipient } from '@/api/transfers'

export function useRecipientInquiryMutation() {
  return useMutation({ mutationFn: inquireTransferRecipient })
}
