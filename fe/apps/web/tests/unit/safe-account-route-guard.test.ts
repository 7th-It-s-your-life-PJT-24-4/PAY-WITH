import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { requireSafeAccountDraft } from '@/pages/guard/safe-account/-utils/safe-account-route-guard'
import { useSafeAccountStore } from '@/stores/safe-account.store'

describe('안전계좌 확인 라우트 가드', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('등록 초안이 없으면 wardId를 유지한 채 입력 단계로 복구한다', () => {
    expect(
      requireSafeAccountDraft(
        { query: { wardId: '12' } } as never,
        {} as never,
        () => undefined,
      ),
    ).toEqual({
      name: 'guard-safe-account-add',
      query: { wardId: '12' },
    })
  })

  it('등록 가능한 초안이 있으면 확인 단계 접근을 허용한다', () => {
    const store = useSafeAccountStore()
    store.selectBank({ code: '004', name: 'KB국민은행' })
    store.setAccountNumber('11012300006781')

    expect(
      requireSafeAccountDraft({ query: {} } as never, {} as never, () => {}),
    ).toBe(true)
  })
})
