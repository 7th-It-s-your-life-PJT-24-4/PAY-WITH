import { describe, expect, it } from 'vitest'

import {
  extractPushNotificationData,
  resolvePushNotificationDestination,
} from '@/lib/push-notification'

describe('푸시 알림 목적지', () => {
  it.each([
    [
      { type: 'APPROVAL_REQUEST', refType: 'APPROVAL', refId: '7' },
      'GUARD',
      '/guard/approval-requests/7?source=push',
    ],
    [
      {
        type: 'ANOMALY',
        refType: 'TRANSACTION',
        refId: '8',
        wardId: '12',
      },
      'GUARD',
      '/guard/history/8?source=push&wardId=12',
    ],
    [
      { type: 'APPROVAL_RESULT', refType: 'TRANSACTION', refId: '9' },
      'WARD',
      '/ward/history/9?source=push',
    ],
  ])('허용된 payload %j를 역할별 경로로 변환한다', (data, role, path) => {
    expect(resolvePushNotificationDestination(data)).toMatchObject({
      data,
      expectedRole: role,
      path,
    })
  })

  it.each([
    null,
    {},
    { type: 'UNKNOWN', refType: 'TRANSACTION', refId: '1' },
    { type: 'APPROVAL_REQUEST', refType: 'TRANSACTION', refId: '1' },
    { type: 'ANOMALY', refType: 'TRANSACTION', refId: 'not-a-number' },
    {
      type: 'ANOMALY',
      refType: 'TRANSACTION',
      refId: '1',
      wardId: '0',
    },
    { type: 'APPROVAL_RESULT', refType: 'TRANSACTION', refId: '0' },
  ])('알 수 없거나 잘못된 payload %j를 무시한다', (data) => {
    expect(resolvePushNotificationDestination(data)).toBeNull()
  })

  it('wardId 추가 전에 발송된 ANOMALY는 보호자 홈으로 이동한다', () => {
    expect(
      resolvePushNotificationDestination({
        type: 'ANOMALY',
        refType: 'TRANSACTION',
        refId: '1',
      }),
    ).toMatchObject({
      expectedRole: 'GUARD',
      path: '/guard?source=push',
    })
  })
})

describe('서비스 워커 알림 클릭 데이터', () => {
  const data = {
    type: 'APPROVAL_RESULT',
    refType: 'TRANSACTION',
    refId: '9',
  }

  it('직접 저장한 data-only 알림 데이터를 읽는다', () => {
    expect(extractPushNotificationData(data)).toEqual(data)
  })

  it('Firebase가 자동 생성한 알림의 FCM_MSG.data를 읽는다', () => {
    expect(extractPushNotificationData({ FCM_MSG: { data } })).toEqual(data)
  })

  it('알 수 없는 알림 데이터는 무시한다', () => {
    expect(extractPushNotificationData({ FCM_MSG: null })).toBeNull()
  })
})
