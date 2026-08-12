const PUSH_NOTIFICATION_PREFERENCE_KEY = 'payWithPushNotificationEnabled'

function getPreferenceKey(userId: number): string {
  return `${PUSH_NOTIFICATION_PREFERENCE_KEY}:${userId}`
}

export const pushNotificationPreference = {
  isEnabled(userId: number): boolean {
    return localStorage.getItem(getPreferenceKey(userId)) !== 'false'
  },
  setEnabled(userId: number, enabled: boolean): void {
    localStorage.setItem(getPreferenceKey(userId), String(enabled))
  },
}
