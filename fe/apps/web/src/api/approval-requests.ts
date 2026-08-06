import { apiClient } from '@/api/client'
import {
  approvalDecisionResponseSchema,
  approvalRequestDetailResponseSchema,
  approvalRequestListResponseSchema,
  type ApprovalDecision,
  type ApprovalHistoryStatus,
  type ApprovalRequestDetail,
  type ApprovalRequestSummary,
} from '@/schemas/approval.schema'

export async function getApprovalRequests(
  wardId?: number,
): Promise<ApprovalRequestSummary[]> {
  const query = wardId === undefined ? '' : `?wardId=${wardId}`
  const response = await apiClient.get(
    `/approval-requests${query}`,
    approvalRequestListResponseSchema,
  )
  return response.data
}

export async function getApprovalRequestDetail(
  approvalId: number,
): Promise<ApprovalRequestDetail> {
  const response = await apiClient.get(
    `/approval-requests/${approvalId}`,
    approvalRequestDetailResponseSchema,
  )
  return response.data
}

export async function getApprovalRequestHistory(
  status: ApprovalHistoryStatus,
  wardId?: number,
): Promise<ApprovalRequestSummary[]> {
  const query = new URLSearchParams({ status })
  if (wardId !== undefined) query.set('wardId', String(wardId))

  const response = await apiClient.get(
    `/approval-requests/history?${query.toString()}`,
    approvalRequestListResponseSchema,
  )
  return response.data
}

export async function approveApprovalRequest(
  approvalId: number,
): Promise<ApprovalDecision> {
  const response = await apiClient.post(
    `/approval-requests/${approvalId}/approve`,
    approvalDecisionResponseSchema,
    {},
  )
  return response.data
}

export async function rejectApprovalRequest(
  approvalId: number,
): Promise<ApprovalDecision> {
  const response = await apiClient.post(
    `/approval-requests/${approvalId}/reject`,
    approvalDecisionResponseSchema,
    {},
  )
  return response.data
}
