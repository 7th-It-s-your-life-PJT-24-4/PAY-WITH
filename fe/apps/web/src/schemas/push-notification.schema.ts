import { z } from 'zod'

const refIdSchema = z.string().regex(/^[1-9]\d*$/)

export const pushNotificationDataSchema = z.discriminatedUnion('type', [
  z.object({
    type: z.literal('APPROVAL_REQUEST'),
    refType: z.literal('APPROVAL'),
    refId: refIdSchema,
  }),
  z.object({
    type: z.literal('ANOMALY'),
    refType: z.literal('TRANSACTION'),
    refId: refIdSchema,
    // wardId 추가 전 발송되어 FCM에 대기 중인 알림도 안전한 홈 fallback으로 처리한다.
    wardId: refIdSchema.optional(),
  }),
  z.object({
    type: z.literal('APPROVAL_RESULT'),
    refType: z.literal('TRANSACTION'),
    refId: refIdSchema,
  }),
])

export type PushNotificationData = z.infer<typeof pushNotificationDataSchema>
