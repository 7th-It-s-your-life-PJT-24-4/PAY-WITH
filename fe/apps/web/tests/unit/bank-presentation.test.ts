import { describe, expect, it } from 'vitest'

import { getBankPresentation } from '@/utils/bank-presentation'

describe('bank presentation', () => {
  it('은행 코드에 맞는 정적 이미지를 반환한다', () => {
    const presentation = getBankPresentation({
      bankCode: '004',
      bankName: 'KB국민은행',
    })

    expect(presentation.iconUrl).toContain('kb-kookmin-bank.svg')
    expect(presentation.brandClass).toBe('bg-[#FFF5D6]')
  })

  it('등록되지 않은 은행 코드는 기본 표현을 반환한다', () => {
    expect(
      getBankPresentation({ bankCode: '999', bankName: '테스트은행' }),
    ).toEqual({ brandClass: 'bg-primary-500' })
  })
})
