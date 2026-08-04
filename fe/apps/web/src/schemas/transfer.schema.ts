import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

const bankCodeSchema = z.string().regex(/^\d{3}$/)
const accountNoSchema = z.string().regex(/^\d{8,16}$/)

export const recipientHistoryParamsSchema = z.object({
  keyword: z.string().trim().min(1).optional(),
  sort: z.enum(['RECENT', 'NAME']).optional(),
  size: z.number().int().min(1).max(50).optional(),
})

export const recipientHistoryItemSchema = z.object({
  recipientId: z.number().int().positive(),
  holderName: z.string().min(1),
  bankCode: bankCodeSchema,
  bankName: z.string().min(1),
  accountNo: accountNoSchema,
  lastSentAt: z.string().min(1),
  sendCount: z.number().int().positive(),
  isRegisteredSafe: z.boolean(),
  safeAccountId: z.number().int().positive().nullable(),
  accountAlias: z.string().nullable(),
})

export const recipientHistoryResponseSchema = apiResponseSchema(
  z.object({ recipients: z.array(recipientHistoryItemSchema) }),
)

export const recipientInquiryRequestSchema = z.object({
  bankCode: bankCodeSchema,
  accountNo: accountNoSchema,
})

const recipientInquiryApiDataSchema = z.object({
  bankCode: bankCodeSchema,
  bankName: z.string().min(1),
  accountNo: accountNoSchema,
  recipientName: z.string().min(1),
})

export const recipientInquiryResponseSchema = apiResponseSchema(
  recipientInquiryApiDataSchema,
)

export const createTransferRequestSchema = z.object({
  bankCode: bankCodeSchema,
  accountNo: accountNoSchema,
  amount: z.number().int().positive(),
  memo: z.string().max(50).nullable(),
  transferPin: z.string().regex(/^\d{6}$/),
})

const completedTransferSchema = z.object({
  transactionId: z.number().int().positive(),
  status: z.literal('COMPLETED'),
  holderName: z.string().min(1),
  bankCode: bankCodeSchema,
  bankName: z.string().min(1),
  accountNo: accountNoSchema,
  amount: z.number().int().positive(),
  memo: z.string().nullable(),
  completedAt: z.string().min(1),
  balanceAfter: z.number().int().nonnegative(),
})

const heldTransferSchema = z.object({
  transactionId: z.number().int().positive(),
  status: z.literal('HELD'),
  holderName: z.string().nullish(),
  bankCode: z.string().nullish(),
  bankName: z.string().nullish(),
  accountNo: z.string().nullish(),
  amount: z.number().nullish(),
  memo: z.string().nullish(),
  completedAt: z.string().nullish(),
  balanceAfter: z.number().nullish(),
})

export const transferResultSchema = z.discriminatedUnion('status', [
  completedTransferSchema,
  heldTransferSchema,
])
export const transferResultResponseSchema =
  apiResponseSchema(transferResultSchema)

export const storedTransferDetailSchema = z.object({
  transactionId: z.number().int().positive(),
  status: z.enum([
    'HELD',
    'COMPLETED',
    'REJECTED',
    'CANCELED',
    'EXPIRED',
    'FAILED',
  ]),
  holderName: z.string(),
  bankCode: z.string(),
  bankName: z.string(),
  accountNo: z.string(),
  amount: z.number().nonnegative(),
  memo: z.string().nullable(),
  requestedAt: z.string().min(1),
  expiredAt: z.string().nullable(),
  respondedAt: z.string().nullable(),
  completedAt: z.string().nullable(),
  balanceAfter: z.number().nonnegative().nullable(),
  riskAnalysis: z
    .object({
      riskScore: z.number(),
      reasons: z.array(
        z.object({
          ruleCode: z.string(),
          description: z.string(),
          score: z.number(),
        }),
      ),
    })
    .nullable(),
  failureCode: z.string().nullable(),
  failureMessage: z.string().nullable(),
})

export type RecipientInquiryRequest = z.infer<
  typeof recipientInquiryRequestSchema
>
export type RecipientInquiryApiData = z.infer<
  typeof recipientInquiryApiDataSchema
>
export type RecipientInquiry = Omit<
  RecipientInquiryApiData,
  'recipientName'
> & {
  holderName: string
}
export type CreateTransferRequest = z.infer<typeof createTransferRequestSchema>
export type TransferResult = z.infer<typeof transferResultSchema>
export type RecipientHistoryParams = z.infer<
  typeof recipientHistoryParamsSchema
>
export type RecipientHistoryItem = z.infer<typeof recipientHistoryItemSchema>
