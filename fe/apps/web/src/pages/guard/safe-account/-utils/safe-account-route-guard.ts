import type { NavigationGuard } from 'vue-router'

import { useSafeAccountStore } from '@/stores/safe-account.store'

export const requireSafeAccountDraft: NavigationGuard = (to) =>
  useSafeAccountStore().registrationDraft
    ? true
    : { name: 'guard-safe-account-add', query: to.query }
