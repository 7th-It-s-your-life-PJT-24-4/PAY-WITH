import { toValue } from 'vue'
import { describe, expect, it } from 'vitest'

import { wardPairingRequestStatusOptions } from '@/lib/query/pairing'

describe('pairing query options', () => {
  it('저장된 대기 요청 ID가 있으면 상태 확인을 계속 폴링한다', () => {
    const options = wardPairingRequestStatusOptions('pairing-request-21')

    expect(toValue(options.enabled)).toBe(true)
    expect(options.refetchInterval).toBe(2_000)
    expect(options.refetchIntervalInBackground).toBe(true)
  })
})
