import { describe, expect, it } from 'vitest'

import { prioritizeBanks } from '@/pages/ward/transfer/-utils/bank-order'

const banks = [
  { bankCode: '004', bankName: 'KB국민은행' },
  { bankCode: '020', bankName: '우리은행' },
  { bankCode: '081', bankName: '하나은행' },
  { bankCode: '088', bankName: '신한은행' },
]

describe('bank order', () => {
  it('추천 은행을 우선순위대로 앞에 두고 전체 은행을 유지한다', () => {
    expect(
      prioritizeBanks(banks, [
        { bankCode: '081', bankName: '하나은행' },
        { bankCode: '020', bankName: '우리은행' },
      ]).map(({ bankCode }) => bankCode),
    ).toEqual(['081', '020', '004', '088'])
  })

  it('추천 은행이 없으면 전체 은행 순서를 유지한다', () => {
    expect(prioritizeBanks(banks, [])).toEqual(banks)
  })
})
