import { describe, expect, it } from 'vitest'

import {
  toSafeAccountTransferRecipient,
  toTransferRecipient,
} from '@/pages/ward/transfer/-utils/transfer-recipient'

describe('toTransferRecipient', () => {
  it('안심계좌 별칭과 API 계좌 정보를 화면 모델로 변환한다', () => {
    expect(
      toTransferRecipient({
        recipientId: 1,
        holderName: '김민수',
        bankCode: '004',
        bankName: 'KB국민은행',
        accountNo: '1234567890123',
        lastSentAt: '2026-08-03T10:00:00',
        sendCount: 3,
        isRegisteredSafe: true,
        safeAccountId: 1,
        accountAlias: '민수 형',
      }),
    ).toMatchObject({
      id: 1,
      name: '민수 형',
      holderName: '김민수',
      bankCode: '004',
      bank: 'KB국민은행',
      accountNumber: '1234567890123',
      isContact: true,
    })
  })

  it('안심계좌를 화면 모델로 변환한다', () => {
    expect(
      toSafeAccountTransferRecipient({
        safeAccountId: 10,
        recipientId: null,
        holderName: '박지연',
        bankCode: '088',
        bankName: '신한은행',
        accountNo: '110234567890',
        accountAlias: null,
        isVerified: true,
        createdAt: '2026-08-03T10:00:00',
      }),
    ).toMatchObject({
      id: 10,
      name: '박지연',
      holderName: '박지연',
      bankCode: '088',
      bank: '신한은행',
      accountNumber: '110234567890',
      isContact: true,
    })
  })
})
