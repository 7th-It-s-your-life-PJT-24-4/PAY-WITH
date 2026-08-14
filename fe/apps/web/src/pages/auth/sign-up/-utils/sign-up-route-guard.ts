import type { NavigationGuard } from 'vue-router'

import { useSignUpStore } from '@/stores/sign-up.store'

const signUpRoleStep = { name: 'auth-sign-up' }

export const requireSignUpRole: NavigationGuard = () =>
  useSignUpStore().role ? true : signUpRoleStep
