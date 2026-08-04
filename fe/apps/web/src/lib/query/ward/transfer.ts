import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import { getTransferRecipients } from '@/api/transfers'
import type { RecipientHistoryParams } from '@/schemas/transfer.schema'

export const wardTransferKeys = {
  all: ['ward-transfer'] as const,
  recipients: (params: RecipientHistoryParams) =>
    [...wardTransferKeys.all, 'recipients', params] as const,
}

export function transferRecipientsOptions(
  params: MaybeRefOrGetter<RecipientHistoryParams> = {},
) {
  const resolvedParams = computed(() => toValue(params))

  return queryOptions({
    queryKey: computed(() => wardTransferKeys.recipients(resolvedParams.value)),
    queryFn: () => getTransferRecipients(resolvedParams.value),
  })
}
