import { unregisterFcmToken } from '@/api/fcm-token'
import { deleteCurrentFcmToken } from '@/lib/firebase'
import { pushTokenStorage } from '@/lib/push-token-storage'
import { resetPushTokenSyncState } from '@/lib/push-token-sync-state'

export async function unregisterPushNotifications(): Promise<void> {
  const storedToken = pushTokenStorage.get()

  if (storedToken) {
    try {
      await unregisterFcmToken(storedToken.token)
    } catch (error) {
      console.warn('FCM 토큰을 서버에서 해제하지 못했습니다.', error)
    }
  }

  await clearLocalPushSubscription()
}

export async function clearLocalPushSubscription(): Promise<void> {
  try {
    await deleteCurrentFcmToken()
  } catch (error) {
    console.warn('브라우저의 FCM 구독을 해제하지 못했습니다.', error)
  } finally {
    pushTokenStorage.clear()
    resetPushTokenSyncState()
  }
}
