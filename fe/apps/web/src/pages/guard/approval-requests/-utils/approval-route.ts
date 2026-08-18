const approvalFilters = [
  'pending',
  'approved',
  'rejected',
  'canceled',
  'expired',
] as const

export type ApprovalFilter = (typeof approvalFilters)[number]
export type ApprovalDecision = 'approved' | 'rejected'
export type ApprovalTransferStatus = 'completed' | 'failed'

function firstQueryValue(value: unknown) {
  return Array.isArray(value) ? value[0] : value
}

export function parseApprovalFilter(value: unknown): ApprovalFilter {
  const normalized = firstQueryValue(value)
  return approvalFilters.includes(normalized as ApprovalFilter)
    ? (normalized as ApprovalFilter)
    : 'pending'
}

export function parseApprovalDecision(value: unknown): ApprovalDecision | null {
  const normalized = firstQueryValue(value)
  return normalized === 'approved' || normalized === 'rejected'
    ? normalized
    : null
}

export function parseApprovalTransferStatus(
  value: unknown,
): ApprovalTransferStatus | null {
  const normalized = firstQueryValue(value)
  return normalized === 'completed' || normalized === 'failed'
    ? normalized
    : null
}

export function parseOptionalQueryText(value: unknown): string | null {
  const normalized = firstQueryValue(value)
  return typeof normalized === 'string' && normalized.length > 0
    ? normalized
    : null
}
