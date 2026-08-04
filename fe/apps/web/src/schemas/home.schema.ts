import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'
import { walletBalanceSchema } from '@/schemas/wallet.schema'

export const pendingApprovalItemSchema = z.object({
  approvalId: z.number().int().positive(),
  transactionId: z.number().int().positive(),
  type: z.literal('TRANSFER_OUT'),
  amount: z.number().int().positive(),
  holderName: z.string().min(1),
  bankName: z.string().min(1),
  accountNo: z.string().min(1),
  riskLevel: z.enum(['SAFE', 'CAUTION', 'DANGER']).nullable(),
  requestedAt: z.string().min(1),
  expiredAt: z.string().min(1),
})

export const wardApprovalDetailSchema = pendingApprovalItemSchema.extend({
  memo: z.string().nullable(),
})

export const wardHomeSchema = z
  .object({
    userName: z.string().min(1),
    wallet: walletBalanceSchema,
    pendingApprovalCount: z.number().int().nonnegative(),
    pendingApprovals: z.array(pendingApprovalItemSchema),
  })
  .refine(
    ({ pendingApprovalCount, pendingApprovals }) =>
      pendingApprovalCount === pendingApprovals.length,
    { message: '승인 대기 건수와 목록 길이가 일치하지 않습니다.' },
  )

export const wardHomeResponseSchema = apiResponseSchema(wardHomeSchema)
export const wardApprovalDetailResponseSchema = apiResponseSchema(
  wardApprovalDetailSchema,
)

export type PendingApprovalItem = z.infer<typeof pendingApprovalItemSchema>
export type WardHome = z.infer<typeof wardHomeSchema>
export type WardApprovalDetail = z.infer<typeof wardApprovalDetailSchema>
