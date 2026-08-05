import citiBankIconUrl from '@pay-with/ui/svg/banks/citi-bank.svg'
import hanaBankIconUrl from '@pay-with/ui/svg/banks/hana-bank.svg'
import ibkBankIconUrl from '@pay-with/ui/svg/banks/ibk-bank.svg'
import kBankIconUrl from '@pay-with/ui/svg/banks/k-bank.svg'
import kakaoBankIconUrl from '@pay-with/ui/svg/banks/kakao-bank.svg'
import kbKookminBankIconUrl from '@pay-with/ui/svg/banks/kb-kookmin-bank.svg'
import nhBankIconUrl from '@pay-with/ui/svg/banks/nh-bank.svg'
import postOfficeIconUrl from '@pay-with/ui/svg/banks/post-office.svg'
import scFirstBankIconUrl from '@pay-with/ui/svg/banks/sc-first-bank.svg'
import shinhanBankIconUrl from '@pay-with/ui/svg/banks/shinhan-bank.svg'
import tossBankIconUrl from '@pay-with/ui/svg/banks/toss-bank.svg'
import wooriBankIconUrl from '@pay-with/ui/svg/banks/woori-bank.svg'

import type { Bank } from '@/schemas/bank.schema'

export interface BankPresentation {
  iconUrl?: string
  brandClass: string
}

const presentationByBankCode: Record<string, BankPresentation> = {
  '003': { iconUrl: ibkBankIconUrl, brandClass: 'bg-[#EAF4FF]' },
  '004': { iconUrl: kbKookminBankIconUrl, brandClass: 'bg-[#FFF5D6]' },
  '011': { iconUrl: nhBankIconUrl, brandClass: 'bg-[#FFF0CC]' },
  '020': { iconUrl: wooriBankIconUrl, brandClass: 'bg-[#E5F5FF]' },
  '023': { iconUrl: scFirstBankIconUrl, brandClass: 'bg-[#EAF8F1]' },
  '027': { iconUrl: citiBankIconUrl, brandClass: 'bg-[#EEF3FF]' },
  '071': { iconUrl: postOfficeIconUrl, brandClass: 'bg-[#FFECE8]' },
  '081': { iconUrl: hanaBankIconUrl, brandClass: 'bg-[#E5F8F5]' },
  '088': { iconUrl: shinhanBankIconUrl, brandClass: 'bg-[#0046FF]' },
  '089': { iconUrl: kBankIconUrl, brandClass: 'bg-[#FFF0F4]' },
  '090': { iconUrl: kakaoBankIconUrl, brandClass: 'bg-[#FFE812]' },
  '092': { iconUrl: tossBankIconUrl, brandClass: 'bg-[#E9F2FF]' },
}

export function getBankPresentation(bank: Bank): BankPresentation {
  return (
    presentationByBankCode[bank.bankCode] ?? {
      brandClass: 'bg-primary-500',
    }
  )
}
