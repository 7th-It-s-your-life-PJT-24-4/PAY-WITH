import {
  approvalDecisionSnapshotSchema,
  type ApprovalDecision,
  type ApprovalDecisionSnapshot,
  type ApprovalRequestDetail,
} from '@/schemas/approval.schema'

const storageKey = (approvalId: number) =>
  `pay-with:guard-approval:${approvalId}`

export function saveApprovalDecisionSnapshot(
  detail: ApprovalRequestDetail,
  decision: ApprovalDecision,
) {
  if (typeof sessionStorage === 'undefined') return
  sessionStorage.setItem(
    storageKey(detail.approvalId),
    JSON.stringify({ detail, decision }),
  )
}

export function getApprovalDecisionSnapshot(
  approvalId: number,
): ApprovalDecisionSnapshot | null {
  if (typeof sessionStorage === 'undefined') return null

  const key = storageKey(approvalId)
  try {
    const raw = sessionStorage.getItem(key)
    if (!raw) return null
    const parsed = approvalDecisionSnapshotSchema.safeParse(JSON.parse(raw))
    if (!parsed.success || parsed.data.detail.approvalId !== approvalId) {
      sessionStorage.removeItem(key)
      return null
    }
    return parsed.data
  } catch {
    sessionStorage.removeItem(key)
    return null
  }
}

export function getApprovalDecisionSnapshots(): ApprovalDecisionSnapshot[] {
  if (typeof sessionStorage === 'undefined') return []

  const snapshots: ApprovalDecisionSnapshot[] = []
  for (let index = 0; index < sessionStorage.length; index += 1) {
    const key = sessionStorage.key(index)
    if (!key?.startsWith('pay-with:guard-approval:')) continue

    const approvalId = Number(key.slice('pay-with:guard-approval:'.length))
    if (!Number.isSafeInteger(approvalId) || approvalId <= 0) continue
    const snapshot = getApprovalDecisionSnapshot(approvalId)
    if (snapshot) snapshots.push(snapshot)
  }

  return snapshots.sort(
    (a, b) =>
      new Date(b.decision.respondedAt).getTime() -
      new Date(a.decision.respondedAt).getTime(),
  )
}
