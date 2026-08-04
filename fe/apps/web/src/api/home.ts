import { apiClient } from '@/api/client'
import {
  wardApprovalDetailResponseSchema,
  wardHomeResponseSchema,
  type WardApprovalDetail,
  type WardHome,
} from '@/schemas/home.schema'

export async function getWardHome(): Promise<WardHome> {
  const response = await apiClient.get('/ward/home', wardHomeResponseSchema)
  return response.data
}

export async function getWardApprovalDetail(
  approvalId: number,
): Promise<WardApprovalDetail> {
  const response = await apiClient.get(
    `/ward/approval-requests/${approvalId}`,
    wardApprovalDetailResponseSchema,
  )
  return response.data
}
