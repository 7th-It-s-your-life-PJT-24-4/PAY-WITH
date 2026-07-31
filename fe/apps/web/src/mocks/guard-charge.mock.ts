import kbKookminBankIconUrl from '@pay-with/ui/svg/banks/kb-kookmin-bank.svg'
import kakaoBankIconUrl from '@pay-with/ui/svg/banks/kakao-bank.svg'
import nhBankIconUrl from '@pay-with/ui/svg/banks/nh-bank.svg'
import tossBankIconUrl from '@pay-with/ui/svg/banks/toss-bank.svg'
import shinhanBankIconUrl from '@pay-with/ui/svg/banks/shinhan-bank.svg'
import wooriBankIconUrl from '@pay-with/ui/svg/banks/woori-bank.svg'
import hanaBankIconUrl from '@pay-with/ui/svg/banks/hana-bank.svg'
import ibkBankIconUrl from '@pay-with/ui/svg/banks/ibk-bank.svg'
import kBankIconUrl from '@pay-with/ui/svg/banks/k-bank.svg'
import scFirstBankIconUrl from '@pay-with/ui/svg/banks/sc-first-bank.svg'
import citiBankIconUrl from '@pay-with/ui/svg/banks/citi-bank.svg'
import postOfficeIconUrl from '@pay-with/ui/svg/banks/post-office.svg'

export interface GuardChargeAccount {
  id: string
  bankName: string
  accountSuffix: string
  balance: number
  iconUrl: string
  brandClass: string
}

export interface GuardChargeBank {
  id: string
  name: string
  iconUrl: string
  brandClass: string
}

export const mockGuardChargeAccounts: GuardChargeAccount[] = [
  {
    id: 'kb-3700',
    bankName: '국민',
    accountSuffix: '3700',
    balance: 500_000,
    iconUrl: kbKookminBankIconUrl,
    brandClass: 'bg-[#FFF5D6]',
  },
  {
    id: 'toss-3700',
    bankName: '토스',
    accountSuffix: '3700',
    balance: 500_000,
    iconUrl: tossBankIconUrl,
    brandClass: 'bg-[#E9F2FF]',
  },
  {
    id: 'hana-3700',
    bankName: '하나',
    accountSuffix: '3700',
    balance: 500_000,
    iconUrl: hanaBankIconUrl,
    brandClass: 'bg-[#E5F8F5]',
  },
  {
    id: 'woori-3700',
    bankName: '우리',
    accountSuffix: '3700',
    balance: 500_000,
    iconUrl: wooriBankIconUrl,
    brandClass: 'bg-[#E5F5FF]',
  },
]

export const mockGuardChargeBanks: GuardChargeBank[] = [
  {
    id: 'kakao',
    name: '카카오뱅크',
    iconUrl: kakaoBankIconUrl,
    brandClass: 'bg-[#FFE812]',
  },
  {
    id: 'kb',
    name: '국민',
    iconUrl: kbKookminBankIconUrl,
    brandClass: 'bg-[#FFF5D6]',
  },
  {
    id: 'nh',
    name: '농협',
    iconUrl: nhBankIconUrl,
    brandClass: 'bg-[#FFF0CC]',
  },
  {
    id: 'toss',
    name: '토스뱅크',
    iconUrl: tossBankIconUrl,
    brandClass: 'bg-[#E9F2FF]',
  },
  {
    id: 'shinhan',
    name: '신한',
    iconUrl: shinhanBankIconUrl,
    brandClass: 'bg-[#0046FF]',
  },
  {
    id: 'woori',
    name: '우리',
    iconUrl: wooriBankIconUrl,
    brandClass: 'bg-[#E5F5FF]',
  },
  {
    id: 'hana',
    name: '하나',
    iconUrl: hanaBankIconUrl,
    brandClass: 'bg-[#E5F8F5]',
  },
  {
    id: 'ibk',
    name: '기업',
    iconUrl: ibkBankIconUrl,
    brandClass: 'bg-[#EAF4FF]',
  },
  {
    id: 'k-bank',
    name: '케이뱅크',
    iconUrl: kBankIconUrl,
    brandClass: 'bg-[#FFF0F4]',
  },
  {
    id: 'sc',
    name: 'SC제일',
    iconUrl: scFirstBankIconUrl,
    brandClass: 'bg-[#EAF8F1]',
  },
  {
    id: 'citi',
    name: '씨티',
    iconUrl: citiBankIconUrl,
    brandClass: 'bg-[#EEF3FF]',
  },
  {
    id: 'post',
    name: '우체국',
    iconUrl: postOfficeIconUrl,
    brandClass: 'bg-[#FFECE8]',
  },
]
