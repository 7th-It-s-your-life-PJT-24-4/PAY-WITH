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
    const approvalOptions = wardApprovalDetailOptions(7)

    expect(toValue(wardHomeOptions().queryKey)).toEqual(wardHomeKeys.all)
    expect(toValue(approvalOptions.queryKey)).toEqual(wardHomeKeys.approval(7))
    expect(approvalOptions.refetchOnWindowFocus).toBe(true)
    expect(approvalOptions.refetchInterval).toBeTypeOf('function')
  })

  it('지갑 잔액에 공용 query key를 사용하고 진입 시 최신 값을 조회한다', () => {
    const options = wardWalletOptions()

    expect(toValue(options.queryKey)).toEqual(wardWalletKeys.all)
    expect(options.staleTime).toBeUndefined()
  })
})
