import { toValue } from 'vue'
import { describe, expect, it } from 'vitest'

import {
  guardHomeKeys,
  guardHomeOptions,
  guardPairingStatusOptions,
} from '@/lib/query/guard/home'

describe('guard home query options', () => {
  it('선택한 시니어별로 query key를 구분한다', () => {
    expect(toValue(guardHomeOptions().queryKey)).toEqual(
      guardHomeKeys.detail(null),
    )
    expect(toValue(guardHomeOptions(12).queryKey)).toEqual(
      guardHomeKeys.detail(12),
    )
  })

  it('페어링 상태 조회는 활성화된 동안 1초마다 갱신한다', () => {
    const options = guardPairingStatusOptions(true)

    expect(toValue(options.queryKey)).toEqual(guardHomeKeys.detail(null))
    expect(toValue(options.refetchInterval)).toBe(1_000)
    expect(options.retry).toBe(1)
    expect(options.retryDelay).toBe(1_000)
  })
})
