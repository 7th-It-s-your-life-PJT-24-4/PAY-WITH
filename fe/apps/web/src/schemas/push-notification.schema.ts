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
  }),
  z.object({
    type: z.literal('APPROVAL_RESULT'),
    refType: z.literal('TRANSACTION'),
    refId: refIdSchema,
  }),
])

export type PushNotificationData = z.infer<typeof pushNotificationDataSchema>
