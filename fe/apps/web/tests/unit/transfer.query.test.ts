import { toValue } from 'vue'
import { describe, expect, it } from 'vitest'

import {
  transferRecipientsOptions,
  wardTransferKeys,
} from '@/lib/query/ward/transfer'
import {
  wardSafeAccountKeys,
  wardSafeAccountsOptions,
} from '@/lib/query/ward/safe-account'

describe('transfer query options', () => {
  it('수취인 조회 조건을 query key에 포함한다', () => {
    const params = { keyword: '김', sort: 'NAME' as const, size: 20 }
    const options = transferRecipientsOptions(params)

    expect(toValue(options.queryKey)).toEqual(
      wardTransferKeys.recipients(params),
    )
  })

  it('안심계좌 목록 query key를 사용한다', () => {
    const options = wardSafeAccountsOptions()

    expect(toValue(options.queryKey)).toEqual(wardSafeAccountKeys.list())
  })
})
