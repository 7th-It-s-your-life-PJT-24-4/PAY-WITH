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
import kdbBankIconUrl from '@pay-with/ui/svg/banks/kdb-bank.svg'
import bnkBankIconUrl from '@pay-with/ui/svg/banks/bnk-kyongnam-bank.svg' // kdb 경남, kdb 부산
import suhyupBankIconUrl from '@pay-with/ui/svg/banks/suhyup-bank.svg'
import mgSaemaeulBankIconUrl from '@pay-with/ui/svg/banks/mg-community-credit.svg'
import cuBankIconUrl from '@pay-with/ui/svg/banks/cu-bank.svg'
import jbBankIconUrl from '@pay-with/ui/svg/banks/gwangju-bank.svg' // 광주은행, 전북은행
import imDaeguBankIconUrl from '@pay-with/ui/svg/banks/im-daegu-bank.svg'

import type { Bank } from '@/schemas/bank.schema'

export interface BankPresentation {
  iconUrl?: string
  brandClass: string
}

const presentationByBankCode: Record<string, BankPresentation> = {
  '002': { iconUrl: kdbBankIconUrl, brandClass: 'bg-[#E8F0FC]' },
  '003': { iconUrl: ibkBankIconUrl, brandClass: 'bg-[#EAF4FF]' },
  '004': { iconUrl: kbKookminBankIconUrl, brandClass: 'bg-[#FFF5D6]' },
  '007': { iconUrl: suhyupBankIconUrl, brandClass: 'bg-[#EAF4FF]' },
  '011': { iconUrl: nhBankIconUrl, brandClass: 'bg-[#FFF0CC]' },
  '020': { iconUrl: wooriBankIconUrl, brandClass: 'bg-[#E5F5FF]' },
  '023': { iconUrl: scFirstBankIconUrl, brandClass: 'bg-[#EAF8F1]' },
  '027': { iconUrl: citiBankIconUrl, brandClass: 'bg-[#EEF3FF]' },
  '031': { iconUrl: imDaeguBankIconUrl, brandClass: 'bg-[#EAF4FF]' },
  '032': { iconUrl: bnkBankIconUrl, brandClass: 'bg-[#FFECE8]' },
  '034': { iconUrl: jbBankIconUrl, brandClass: 'bg-[#EAF4FF]' },
  '035': { iconUrl: shinhanBankIconUrl, brandClass: 'bg-[#0046FF]' },
  '037': { iconUrl: jbBankIconUrl, brandClass: 'bg-[#EAF4FF]' },
  '039': { iconUrl: bnkBankIconUrl, brandClass: 'bg-[#FFECE8]' },
  '045': { iconUrl: mgSaemaeulBankIconUrl, brandClass: 'bg-[#EAF4FF]' },
  '048': { iconUrl: cuBankIconUrl, brandClass: 'bg-[#EAF4FF]' },
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
