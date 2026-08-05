import { PhCrown, PhDress, PhWallet } from '@phosphor-icons/vue'
import type { Component } from 'vue'

export type GuardTransactionCategory = 'charge' | 'transfer' | 'payment'
export type GuardTransactionRisk = 'safe' | 'warning' | 'danger'

export const guardTransactionCategoryIcons: Record<
  GuardTransactionCategory,
  Component
> = {
  charge: PhWallet,
  transfer: PhCrown,
  payment: PhDress,
}

export const guardTransactionStatusLabels: Record<
  GuardTransactionRisk,
  string
> = {
  safe: '안전',
  warning: '주의',
  danger: '위험',
}

export const guardTransactionStatusClasses: Record<
  GuardTransactionRisk,
  string
> = {
  safe: 'bg-[#d5ffd8] text-success',
  warning: 'bg-[#fffcc8] text-warning',
  danger: 'bg-[#fff3f3] text-error',
}
