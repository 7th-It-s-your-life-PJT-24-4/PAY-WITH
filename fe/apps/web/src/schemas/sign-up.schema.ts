import { z } from 'zod'

export const signUpRoleSchema = z.object({
  role: z.enum(['senior', 'guardian'], {
    error: '가입 유형을 선택해 주세요.',
  }),
})

export type SignUpRole = z.infer<typeof signUpRoleSchema>['role']

const signUpDetailsFields = {
  fullName: z.string().trim().min(1, '성함을 입력해 주세요.'),
  loginPassword: z
    .string()
    .min(8, '비밀번호는 숫자와 영문을 포함해 8자 이상이어야 합니다.')
    .regex(/[A-Za-z]/, '비밀번호에 영문을 포함해 주세요.')
    .regex(/\d/, '비밀번호에 숫자를 포함해 주세요.'),
  paymentPassword: z
    .string()
    .regex(/^\d{6}$/, '결제 비밀번호 6자리를 입력해 주세요.'),
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
  phoneNumber: z
    .string()
    .regex(/^01[016789]-?\d{3,4}-?\d{4}$/, '휴대폰 번호를 확인해 주세요.'),
})

export const kakaoSignUpDetailsSchema = z.object({
  ...signUpDetailsFields,
  phoneNumber: z.string(),
})

export type SignUpDetails = z.infer<typeof signUpDetailsSchema>
export type SignUpDetailsForm = z.input<typeof kakaoSignUpDetailsSchema>
