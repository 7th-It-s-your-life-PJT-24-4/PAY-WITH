export const signUpTerms = {
  serviceTerms: {
    label: '[필수] 서비스 이용약관',
    title: '서비스 이용약관',
    updatedAt: '2026년 7월 22일',
    sections: [
      {
        title: '제1조 목적',
        paragraphs: [
          '이 약관은 PayWith 서비스의 이용 조건과 절차, 회사와 이용자의 권리 및 의무를 정하는 것을 목적으로 합니다.',
        ],
      },
      {
        title: '제2조 서비스 이용',
        paragraphs: [
          '이용자는 관련 법령과 이 약관을 준수하여 서비스를 이용해야 합니다.',
          '회사는 서비스의 안정적인 제공을 위해 필요한 경우 서비스의 일부를 변경하거나 중단할 수 있습니다.',
        ],
      },
      {
        title: '제3조 이용자 의무',
        paragraphs: [
          '이용자는 본인의 계정과 비밀번호를 안전하게 관리해야 하며, 제3자에게 양도하거나 공유해서는 안 됩니다.',
        ],
      },
    ],
  },
  privacyTerms: {
    label: '[필수] 개인정보 수집 및 이용 동의',
    title: '개인정보 수집 및 이용 동의',
    updatedAt: '2026년 7월 22일',
    sections: [
      {
        title: '수집하는 개인정보',
        paragraphs: ['성함, 휴대폰 번호, 프로필 이미지(선택)를 수집합니다.'],
      },
      {
        title: '이용 목적',
        paragraphs: [
          '본인 확인, 회원 관리, 서비스 제공 및 안전한 결제 처리를 위해 이용합니다.',
        ],
      },
      {
        title: '보유 및 이용 기간',
        paragraphs: [
          '회원 탈퇴 시까지 보유하며, 관계 법령에서 보존을 요구하는 정보는 해당 기간 동안 보관합니다.',
        ],
      },
    ],
  },
  identifierTerms: {
    label: '[필수] 고유식별정보 처리 동의',
    title: '고유식별정보 처리 동의',
    updatedAt: '2026년 7월 22일',
    sections: [
      {
        title: '처리 목적',
        paragraphs: [
          '법령상 본인 확인과 부정 이용 방지 등 서비스 제공에 필요한 업무를 수행하기 위해 고유식별정보를 처리할 수 있습니다.',
        ],
      },
      {
        title: '처리 및 보유 기간',
        paragraphs: [
          '고유식별정보는 관련 법령에서 정한 목적과 기간 내에서만 처리 및 보관합니다.',
        ],
      },
      {
        title: '동의 거부 안내',
        paragraphs: [
          '동의를 거부할 수 있으나, 고유식별정보 처리가 필요한 서비스 이용이 제한될 수 있습니다.',
        ],
      },
    ],
  },
} as const

export type SignUpTermId = keyof typeof signUpTerms
