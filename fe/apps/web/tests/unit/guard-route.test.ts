import { describe, expect, it } from 'vitest'

import {
  parsePositiveRouteId,
  withGuardWardId,
} from '@/pages/guard/-utils/guard-route'
import router from '@/router'

describe('guard route utilities', () => {
  it('양의 정수 ID만 파싱한다', () => {
    expect(parsePositiveRouteId('12')).toBe(12)
    expect(parsePositiveRouteId(['13'])).toBe(13)
    expect(parsePositiveRouteId('0')).toBeNull()
    expect(parsePositiveRouteId('-1')).toBeNull()
    expect(parsePositiveRouteId('ward')).toBeNull()
    expect(parsePositiveRouteId(undefined)).toBeNull()
  })

  it('기존 query를 보존하며 wardId를 갱신한다', () => {
    expect(withGuardWardId({ status: 'approved' }, 13)).toEqual({
      status: 'approved',
      wardId: 13,
    })
  })
})

describe('guard detail routes', () => {
  it.each([
    ['/guard/charge/41', 'guard-charge-detail'],
    ['/guard/history/42', 'guard-transaction-detail'],
    ['/guard/approval-requests/43', 'guard-approval-request-detail'],
    [
      '/guard/approval-requests/43/complete',
      'guard-approval-decision-complete',
    ],
  ])('%s는 숫자 id 상세 라우트와 매칭된다', (path, name) => {
    const resolved = router.resolve(path)

    expect(resolved.name).toBe(name)
    expect(resolved.params.id).toBe(path.match(/\d+/)?.[0])
  })

  it.each([
    '/guard/charge/not-a-number',
    '/guard/history/not-a-number',
    '/guard/approval-requests/not-a-number',
    '/guard/approval-requests/not-a-number/complete',
  ])('%s는 상세 라우트와 매칭되지 않는다', (path) => {
    expect(router.resolve(path).matched).toHaveLength(0)
  })

  it('비로그인 푸시 상세 진입은 로그인 후 복구할 목적지를 보존한다', async () => {
    localStorage.clear()
    const destination = '/guard/history/42?source=push&wardId=7'

    await router.push(destination)

    expect(router.currentRoute.value).toMatchObject({
      name: 'auth-sign-in',
      query: { redirect: destination },
    })
  })
})
