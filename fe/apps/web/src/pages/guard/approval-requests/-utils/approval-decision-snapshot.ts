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
