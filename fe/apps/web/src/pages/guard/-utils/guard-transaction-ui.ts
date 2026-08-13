import { PhCrown, PhDress, PhWallet } from '@phosphor-icons/vue'
import type { Component } from 'vue'

import type { GuardTransaction } from '@/mocks/guard-home.mock'

export const guardTransactionCategoryIcons: Record<
  GuardTransaction['category'],
  Component
> = {
  charge: PhWallet,
  transfer: PhCrown,
  payment: PhDress,
}

export const guardTransactionStatusLabels: Record<
  GuardTransaction['status'],
  string
> = {
  safe: '안전',
  warning: '주의',
  danger: '위험',
}

export const guardTransactionStatusClasses: Record<
  GuardTransaction['status'],
  string
> = {
  safe: 'bg-[#E6F7EE] text-[#1B8250]',
  warning: 'bg-[#FFF6DC] text-[#8A6300]',
  danger: 'bg-[#FFECEC] text-[#C93636]',
}
