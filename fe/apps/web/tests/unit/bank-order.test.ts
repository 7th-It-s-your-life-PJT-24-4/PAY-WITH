import { describe, expect, it } from 'vitest'

import { prioritizeBanks } from '@/pages/ward/transfer/-utils/bank-order'

const banks = [
  { bankCode: '023', bankName: 'SC제일은행' },
  { bankCode: '003', bankName: 'IBK기업은행' },
  { bankCode: '004', bankName: 'KB국민은행' },
  { bankCode: '020', bankName: '우리은행' },
  { bankCode: '081', bankName: '하나은행' },
  { bankCode: '088', bankName: '신한은행' },
  { bankCode: '027', bankName: '한국씨티은행' },
]

describe('bank order', () => {
  it('추천 은행을 우선순위대로 앞에 두고 전체 은행을 유지한다', () => {
    expect(
      prioritizeBanks(banks, [
        { bankCode: '081', bankName: '하나은행' },
        { bankCode: '020', bankName: '우리은행' },
      ]).map(({ bankCode }) => bankCode),
    ).toEqual(['081', '020', '004', '088', '003', '023', '027'])
  })

  it('추천 은행이 없으면 FE 기본 우선순위를 적용한다', () => {
    expect(prioritizeBanks(banks, []).map(({ bankCode }) => bankCode)).toEqual([
      '004',
      '088',
      '020',
      '081',
      '003',
      '023',
      '027',
    ])
  })

  it('기본 우선순위에 없는 은행은 API 응답 순서를 유지한다', () => {
    expect(
      prioritizeBanks(
        [
          { bankCode: '023', bankName: 'SC제일은행' },
          { bankCode: '027', bankName: '한국씨티은행' },
          { bankCode: '031', bankName: '대구은행' },
        ],
        [],
      ).map(({ bankCode }) => bankCode),
    ).toEqual(['023', '027', '031'])
  })
})
