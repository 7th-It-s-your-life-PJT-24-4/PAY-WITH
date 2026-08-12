import { describe, expect, it } from 'vitest'

import { resolvePushNotificationDestination } from '@/lib/push-notification'

describe('푸시 알림 목적지', () => {
  it.each([
    [
      { type: 'APPROVAL_REQUEST', refType: 'APPROVAL', refId: '7' },
      'GUARD',
      '/guard/approval-requests/7?source=push',
    ],
    [
      { type: 'ANOMALY', refType: 'TRANSACTION', refId: '8' },
      'GUARD',
      '/guard/history/8?source=push',
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
    { type: 'APPROVAL_RESULT', refType: 'TRANSACTION', refId: '0' },
  ])('알 수 없거나 잘못된 payload %j를 무시한다', (data) => {
    expect(resolvePushNotificationDestination(data)).toBeNull()
  })
})
