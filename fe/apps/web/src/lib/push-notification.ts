import {
  pushNotificationDataSchema,
  type PushNotificationData,
} from '@/schemas/push-notification.schema'
import type { User } from '@/schemas/user.schema'

export type PushNotificationDestination = {
  data: PushNotificationData
  expectedRole: User['role']
  path: string
}

export function resolvePushNotificationDestination(
  value: unknown,
): PushNotificationDestination | null {
  const result = pushNotificationDataSchema.safeParse(value)
  if (!result.success) return null

  const data = result.data
  if (data.type === 'APPROVAL_REQUEST') {
    return {
      data,
      expectedRole: 'GUARD',
      path: `/guard/approval-requests/${data.refId}?source=push`,
    }
  }
  if (data.type === 'ANOMALY') {
    return {
      data,
      expectedRole: 'GUARD',
      path: `/guard/history/${data.refId}?source=push`,
    }
  }
  return {
    data,
    expectedRole: 'WARD',
    path: `/ward/history/${data.refId}?source=push`,
  }
}
