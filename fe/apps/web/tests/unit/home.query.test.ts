import { toValue } from 'vue'
import { describe, expect, it } from 'vitest'

import {
  wardApprovalDetailOptions,
  wardHomeKeys,
  wardHomeOptions,
} from '@/lib/query/ward/home'
import { wardWalletKeys, wardWalletOptions } from '@/lib/query/ward/wallet'

describe('ward home query options', () => {
  it('홈과 승인 상세 query key를 구분한다', () => {
    expect(toValue(wardHomeOptions().queryKey)).toEqual(wardHomeKeys.all)
    expect(toValue(wardApprovalDetailOptions(7).queryKey)).toEqual(
      wardHomeKeys.approval(7),
    )
  })

  it('지갑 잔액에 공용 query key를 사용한다', () => {
    expect(toValue(wardWalletOptions().queryKey)).toEqual(wardWalletKeys.all)
  })
})
