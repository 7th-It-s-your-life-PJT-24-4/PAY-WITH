let syncedSessionKey = ''

export function isPushTokenSessionSynced(sessionKey: string): boolean {
  return syncedSessionKey === sessionKey
}

export function markPushTokenSessionSynced(sessionKey: string): void {
  syncedSessionKey = sessionKey
}

export function resetPushTokenSyncState(): void {
  syncedSessionKey = ''
}
