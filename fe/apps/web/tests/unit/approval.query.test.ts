import { toValue } from 'vue'
import { describe, expect, it } from 'vitest'

import {
  guardApprovalHistoryOptions,
  guardApprovalKeys,
  guardApprovalListOptions,
  guardApprovalResultOptions,
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

  it('종결 상세는 승인요청 ID별 query key를 사용한다', () => {
    expect(toValue(guardApprovalResultOptions(3).queryKey)).toEqual(
      guardApprovalKeys.result(3),
    )
  })
})
