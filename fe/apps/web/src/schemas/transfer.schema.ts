import { z } from 'zod'

import { apiResponseSchema } from '@/schemas/api-response.schema'

const bankCodeSchema = z.string().regex(/^\d{3}$/)
const accountNoSchema = z.string().regex(/^\d{8,16}$/)

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
