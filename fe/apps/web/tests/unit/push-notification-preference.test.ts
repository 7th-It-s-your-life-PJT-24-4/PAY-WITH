import { beforeEach, describe, expect, it } from 'vitest'

import { pushNotificationPreference } from '@/lib/push-notification-preference'

describe('푸시 알림 사용자 설정', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('저장된 설정이 없으면 알림 수신을 기본값으로 사용한다', () => {
    expect(pushNotificationPreference.isEnabled(7)).toBe(true)
  })

  it('사용자별로 알림 수신 설정을 저장한다', () => {
    pushNotificationPreference.setEnabled(7, false)
    pushNotificationPreference.setEnabled(8, true)

    expect(pushNotificationPreference.isEnabled(7)).toBe(false)
    expect(pushNotificationPreference.isEnabled(8)).toBe(true)
  })
})
