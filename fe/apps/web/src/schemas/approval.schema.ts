import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

export const approvalRiskLevelSchema = z.enum(['SAFE', 'CAUTION', 'DANGER'])

export const approvalRequestSummarySchema = z.object({
  approvalId: z.number().int().positive(),
  wardId: z.number().int().positive(),
  wardName: z.string(),
  amount: z.number().nonnegative(),
  holderName: z.string().nullable(),
  bankName: z.string().nullable(),
  riskLevel: approvalRiskLevelSchema.nullable(),
  requestedAt: z.string().min(1),
  expiredAt: z.string().min(1),
})

export const approvalRuleHitSchema = z.object({
  ruleCode: z.string(),
  description: z.string(),
})

export const approvalRequestDetailSchema = z.object({
  approvalId: z.number().int().positive(),
  wardId: z.number().int().positive(),
  wardName: z.string(),
  amount: z.number().nonnegative(),
  memo: z.string().nullable(),
  holderName: z.string().nullable(),
  bankName: z.string().nullable(),
  accountNo: z.string().nullable(),
  riskLevel: approvalRiskLevelSchema.nullable(),
  totalScore: z.number().int().nonnegative().nullable(),
  requestedAt: z.string().min(1),
  expiredAt: z.string().min(1),
  ruleHits: z.array(approvalRuleHitSchema),
})

export const approvalTransferResultSchema = z.object({
  status: z.enum(['COMPLETED', 'FAILED']),
  failureReason: z.string().nullable(),
  completedAt: z.string().nullable(),
  balanceAfter: z.number().int().nonnegative().nullable(),
})

export const approvalDecisionSchema = z.object({
  approvalId: z.number().int().positive(),
  transactionId: z.number().int().positive(),
  status: z.enum(['APPROVED', 'REJECTED']),
  respondedAt: z.string().min(1),
  transfer: approvalTransferResultSchema.nullable(),
})

export const approvalRequestListResponseSchema = apiResponseSchema(
  z.array(approvalRequestSummarySchema),
)
export const approvalRequestDetailResponseSchema = apiResponseSchema(
  approvalRequestDetailSchema,
)
export const approvalDecisionResponseSchema = apiResponseSchema(
  approvalDecisionSchema,
)

export const approvalDecisionSnapshotSchema = z.object({
  detail: approvalRequestDetailSchema,
  decision: approvalDecisionSchema,
})

export type ApprovalRequestSummary = z.infer<
  typeof approvalRequestSummarySchema
>
export type ApprovalRequestDetail = z.infer<typeof approvalRequestDetailSchema>
export type ApprovalDecision = z.infer<typeof approvalDecisionSchema>
export type ApprovalDecisionSnapshot = z.infer<
  typeof approvalDecisionSnapshotSchema
>
