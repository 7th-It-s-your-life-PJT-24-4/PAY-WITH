import { describe, expect, it } from 'vitest'

import {
  getPostSignUpPath,
  getRoleHomePath,
  getSafePostLoginPath,
  getUnauthenticatedSignInQuery,
} from '@/router/auth-navigation'

describe('getPostSignUpPath', () => {
  it('보호자는 가입 직후 푸시 알림 안내로 이동한다', () => {
    expect(getPostSignUpPath('guardian')).toBe(
      '/guard/onboarding/push-notifications',
    )
  })

  it('시니어는 기존처럼 홈으로 이동한다', () => {
    expect(getPostSignUpPath('senior')).toBe('/ward')
  })
})

describe('getRoleHomePath', () => {
  it('보호자는 guard 홈으로 이동한다', () => {
    expect(getRoleHomePath('GUARD')).toBe('/guard')
  })

  it('시니어는 ward 홈으로 이동한다', () => {
    expect(getRoleHomePath('WARD')).toBe('/ward')
  })
})

describe('getSafePostLoginPath', () => {
  it('allows safe authenticated pages', () => {
    expect(getSafePostLoginPath('/ward?tab=recent', 'WARD')).toBe(
      '/ward?tab=recent',
    )
    expect(getSafePostLoginPath('/ward/history', 'WARD')).toBe('/ward/history')
    expect(getSafePostLoginPath('/guard/history', 'GUARD')).toBe(
      '/guard/history',
    )
  })

  it('rejects auth, external, other-role, and transaction flow paths', () => {
    expect(getSafePostLoginPath('//example.com', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/auth/sign-in', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/guard', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/ward/payment/qr/1', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/ward/transfer/processing', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/guard/charge', 'GUARD')).toBeNull()
  })
})

describe('getUnauthenticatedSignInQuery', () => {
  it('로그아웃 상태의 푸시 목적지를 로그인 후 복구하도록 보존한다', () => {
    const destination = '/guard?source=push'

    expect(getUnauthenticatedSignInQuery(destination, 'push', false)).toEqual({
      redirect: destination,
    })
  })

  it('세션 만료는 사유와 목적지를 함께 보존한다', () => {
    expect(
      getUnauthenticatedSignInQuery('/ward/history/9', undefined, true),
    ).toEqual({
      reason: 'session-expired',
      redirect: '/ward/history/9',
    })
  })

  it('일반 비로그인 페이지는 기존처럼 목적지를 남기지 않는다', () => {
    expect(
      getUnauthenticatedSignInQuery('/ward', undefined, false),
    ).toBeUndefined()
  })
})
