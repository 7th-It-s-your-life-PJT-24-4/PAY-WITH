import { apiClient } from '@/api/client'
import {
  fcmTokenRequestSchema,
  fcmTokenResponseSchema,
} from '@/schemas/fcm-token.schema'

export async function registerFcmToken(fcmToken: string): Promise<void> {
  const request = fcmTokenRequestSchema.parse({ fcmToken })
  await apiClient.put('/users/me/fcm-token', fcmTokenResponseSchema, request)
}

export async function unregisterFcmToken(fcmToken: string): Promise<void> {
  const request = fcmTokenRequestSchema.parse({ fcmToken })
  await apiClient.delete('/users/me/fcm-token', fcmTokenResponseSchema, request)
}
