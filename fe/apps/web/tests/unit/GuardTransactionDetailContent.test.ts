import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GuardTransactionDetailContent from '@/pages/guard/-components/GuardTransactionDetailContent.vue'
import {
  createGuardTransactionDetailView,
  defaultTransferFailureReason,
  getGuardTransactionTitlePresentation,
  maskGuardAccountNumber,
  type GuardTransactionDetailView,
} from '@/pages/guard/-utils/guard-transaction-detail'
import type { GuardTransactionDetail } from '@/schemas/transaction.schema'

const completedDangerDetail: GuardTransactionDetail = {
  transactionId: 41,
  type: 'TRANSFER',
  direction: 'OUT',
  status: 'COMPLETED',
  riskLevel: 'DANGER',
  counterpartyName: '박수취',
  bankName: '신한은행',
  accountNo: '110234567890',
  amount: 35_000,
  memo: '생활비',
  balanceAfter: 120_000,
  riskAnalysis: {
    riskScore: 87,
    summary: '평소와 다른 고액 송금이에요.',
    reasons: ['SUSPICIOUS_MEMO'],
  },
  occurredAt: '2026-08-05T09:20:01',
}

describe('GuardTransactionDetailContent', () => {
  it('송금 상세의 라벨과 계좌 마스킹, AI 분석을 한 형식으로 표시한다', () => {
    const wrapper = mount(GuardTransactionDetailContent, {
      props: {
        detail: createGuardTransactionDetailView(completedDangerDetail),
      },
    })

    expect(wrapper.get('h2').text()).toBe('승인되어 송금이 완료된 거래에요')
    expect(wrapper.text()).toContain('받는 분')
    expect(wrapper.text()).toContain('박수취')
    expect(wrapper.text()).toContain('받는 계좌')
    expect(wrapper.text()).toContain('신한은행 110-***-7890')
    expect(wrapper.text()).not.toContain('출금처')
    expect(wrapper.text()).toContain('평소와 다른 고액 송금이에요.')
    expect(wrapper.text()).toContain('메모에서 위험 키워드가 감지됐어요.')
  })

  it('서버 거래 상태가 FAILED일 때만 실패 안내와 사유를 표시한다', () => {
    const failedDetail: GuardTransactionDetail = {
      ...completedDangerDetail,
      status: 'FAILED',
    }
    const wrapper = mount(GuardTransactionDetailContent, {
      props: {
        detail: createGuardTransactionDetailView(
          failedDetail,
          '송금 가능한 잔액이 부족합니다.',
        ),
      },
    })

    expect(wrapper.get('h2').text()).toBe('거래를 승인했지만 송금에 실패했어요')
    expect(wrapper.get('[role="alert"]').text()).toContain('송금 실패')
    expect(wrapper.text()).toContain('송금 가능한 잔액이 부족합니다.')
  })

  it('충전 계좌는 피보호자명과 은행을 유지하고 계좌번호에만 말줄임을 적용한다', () => {
    const chargeDetail: GuardTransactionDetail = {
      ...completedDangerDetail,
      transactionId: 40,
      type: 'CHARGE',
      direction: 'IN',
      riskLevel: null,
      counterpartyName: '194 테스트 피보호자',
      bankName: 'KB국민은행',
      accountNo: '12345678901234',
      riskAnalysis: null,
    }
    const wrapper = mount(GuardTransactionDetailContent, {
      props: {
        detail: createGuardTransactionDetailView(chargeDetail),
      },
    })

    const holderName = wrapper.get('[data-testid="charge-holder-name"]')
    const bankName = wrapper.get('[data-testid="charge-bank-name"]')
    const accountNo = wrapper.get('[data-testid="charge-account-number"]')
    const accountDetail = accountNo.element.parentElement

    expect(holderName.text()).toBe('194 테스트 피보호자님')
    expect(bankName.text()).toBe('KB국민은행')
    expect(accountNo.text()).toBe('12345678901234')
    expect(wrapper.text()).not.toContain('***')
    expect(holderName.classes()).not.toContain('truncate')
    expect(bankName.classes()).not.toContain('truncate')
    expect(accountNo.classes()).toContain('truncate')
    expect(accountDetail?.classList.contains('flex-1')).toBe(true)
    expect(accountDetail?.classList.contains('overflow-hidden')).toBe(true)
    expect(wrapper.text()).not.toContain('(KB국민은행')
  })
})

describe('guard transaction detail presentation', () => {
  it('계좌번호 앞자리와 끝 4자리를 동일한 정책으로 마스킹한다', () => {
    expect(maskGuardAccountNumber('110234567890')).toBe('110-***-7890')
    expect(maskGuardAccountNumber(null)).toBe('-')
  })

  it('URL 상태가 아니라 상세 데이터의 status와 riskLevel로 제목을 결정한다', () => {
    const detail = createGuardTransactionDetailView(completedDangerDetail)

    expect(getGuardTransactionTitlePresentation(detail).title).toBe(
      '승인되어 송금이 완료된 거래에요',
    )
    expect(
      getGuardTransactionTitlePresentation({
        ...detail,
        status: 'REJECTED',
      }).title,
    ).toBe('거절된 이상 거래에요')
  })

  it('직접 진입한 FAILED 상세에는 안전한 기본 실패 사유를 제공한다', () => {
    const detail = createGuardTransactionDetailView({
      ...completedDangerDetail,
      status: 'FAILED',
    })

    expect(detail.failureReason).toBe(defaultTransferFailureReason)
  })

  it('공통 뷰 타입이 승인 대기 상태를 표현할 수 있다', () => {
    const detail: GuardTransactionDetailView = {
      ...createGuardTransactionDetailView(completedDangerDetail),
      status: 'HELD',
    }

    expect(getGuardTransactionTitlePresentation(detail).title).toBe(
      '이상 거래가 발생했어요',
    )
  })
})
