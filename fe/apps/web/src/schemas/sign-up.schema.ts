import { z } from 'zod'

import { phoneNumberSchema } from '@/schemas/auth.schema'

export const signUpRoleSchema = z.object({
  role: z.enum(['senior', 'guardian'], {
    error: '가입 유형을 선택해 주세요.',
  }),
})

export type SignUpRole = z.infer<typeof signUpRoleSchema>['role']

function isValidBirthDate(value: string) {
  const year = Number(value.slice(0, 4))
  const month = Number(value.slice(4, 6))
  const day = Number(value.slice(6, 8))
  const currentDate = new Date()
  const today = [
    currentDate.getFullYear(),
    String(currentDate.getMonth() + 1).padStart(2, '0'),
    String(currentDate.getDate()).padStart(2, '0'),
  ].join('')
  const date = new Date(Date.UTC(year, month - 1, day))

  return (
    year >= 1900 &&
    value <= today &&
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  )
}

export const nameSchema = z
  .string()
  .min(2, '성함은 2자 이상 입력해 주세요.')
  .max(30, '성함은 30자 이하로 입력해 주세요.')
  .regex(/^[\p{L}]+$/u, '성함에는 문자만 입력해 주세요.')

export const loginPasswordSchema = z
  .string()
  .min(8, '비밀번호는 숫자와 영문을 포함해 8자 이상이어야 합니다.')
  .max(64, '비밀번호는 64자 이하로 입력해 주세요.')
  .regex(/[A-Za-z]/, '비밀번호에 영문을 포함해 주세요.')
  .regex(/\d/, '비밀번호에 숫자를 포함해 주세요.')
  .regex(/^\S+$/, '비밀번호에는 공백을 사용할 수 없습니다.')

export const paymentPasswordSchema = z
  .string()
  .regex(/^\d{6}$/, '결제 비밀번호 6자리를 입력해 주세요.')

export const birthDateSchema = z
  .string()
  .transform((value) => value.replace(/\D/g, ''))
  .pipe(z.string().regex(/^\d{8}$/, '생년월일 8자리를 입력해 주세요.'))
  .refine(isValidBirthDate, '올바른 생년월일을 입력해 주세요.')

const signUpDetailsFields = {
  fullName: nameSchema,
  loginPassword: loginPasswordSchema,
  paymentPassword: paymentPasswordSchema,
  avatarId: z.number().int().min(1).max(6).nullable(),
  serviceTerms: z.boolean().refine((value) => value, {
    error: '필수 약관에 모두 동의해 주세요.',
  }),
  privacyTerms: z.boolean().refine((value) => value, {
    error: '필수 약관에 모두 동의해 주세요.',
  }),
  identifierTerms: z.boolean().refine((value) => value, {
    error: '필수 약관에 모두 동의해 주세요.',
  }),
}

export const signUpDetailsSchema = z.object({
  ...signUpDetailsFields,
  phoneNumber: phoneNumberSchema,
  birthDate: birthDateSchema,
  gender: z.enum(['남', '여'], {
    error: '성별을 선택해 주세요.',
  }),
})

export type SignUpDetails = z.infer<typeof signUpDetailsSchema>
export type SignUpDetailsForm = z.input<typeof signUpDetailsSchema>
