import { toValue } from 'vue'
import { describe, expect, it } from 'vitest'

import {
  guardApprovalHistoryOptions,
  guardApprovalKeys,
  guardApprovalListOptions,
} from '@/lib/query/guard/approval'

describe('guard approval query options', () => {
  it('대기와 상태별 이력 목록 query key를 구분한다', () => {
    expect(toValue(guardApprovalListOptions(12).queryKey)).toEqual(
      guardApprovalKeys.pendingList(12),
    )
    expect(
      toValue(guardApprovalHistoryOptions('CANCELED', 12).queryKey),
    ).toEqual(guardApprovalKeys.historyList('CANCELED', 12))
    expect(
      toValue(guardApprovalHistoryOptions('EXPIRED', null).queryKey),
    ).toEqual(guardApprovalKeys.historyList('EXPIRED', null))
  })
})
