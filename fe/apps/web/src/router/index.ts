import { createRouter, createWebHistory } from 'vue-router'

import { getUserIdFromAccessToken, tokenStorage } from '@/api/token-storage'
import { refreshAccessToken } from '@/api/token-refresh'
import { getUser } from '@/api/users'
import SignInPage from '@/pages/auth/sign-in/page.vue'
import SignUpPage from '@/pages/auth/sign-up/page.vue'
import SignUpDetailsPage from '@/pages/auth/sign-up/details/page.vue'
import SignUpTermsPage from '@/pages/auth/sign-up/terms/page.vue'
import GuardChargePage from '@/pages/guard/charge/page.vue'
import GuardChargeAccountPage from '@/pages/guard/charge/account/page.vue'
import GuardChargeBePage from '@/pages/guard/charge/be/page.vue'
import GuardChargeCompletePage from '@/pages/guard/charge/complete/page.vue'
import GuardChargeDetailPage from '@/pages/guard/charge/[id]/page.vue'
import GuardChargePasswordPage from '@/pages/guard/charge/password/page.vue'
import GuardTransactionDetailPage from '@/pages/guard/history/[id]/page.vue'
import GuardTransactionDecisionCompletePage from '@/pages/guard/history/[id]/decision-complete.vue'
import GuardHistoryPage from '@/pages/guard/history/page.vue'
import GuardLayout from '@/pages/guard/layout.vue'
import GuardMyPage from '@/pages/guard/my/page.vue'
import GuardPage from '@/pages/guard/page.vue'
import GuardPairingCodePage from '@/pages/guard/pairing/code.vue'
import GuardSafeAccountConfirmPage from '@/pages/guard/safe-account/confirm/page.vue'
import GuardSafeAccountPage from '@/pages/guard/safe-account/page.vue'
import WardPairingCompletePage from '@/pages/ward/pairing/complete/page.vue'
import WardPairingPage from '@/pages/ward/pairing/page.vue'
import { requireCompletedPairing } from '@/pages/ward/pairing/-utils/pairing-route-guard'
import WardChargeAccountAddPage from '@/pages/ward/charge/account/add/page.vue'
import WardChargeAccountCompletePage from '@/pages/ward/charge/account/complete/page.vue'
import WardChargeCompletePage from '@/pages/ward/charge/[transactionId]/complete/page.vue'
import WardChargePage from '@/pages/ward/charge/page.vue'
import WardLayout from '@/pages/ward/layout.vue'
import WardPage from '@/pages/ward/page.vue'
import WardTransactionDetailPage from '@/pages/ward/history/[transactionId]/page.vue'
import WardTransactionHistoryPage from '@/pages/ward/history/page.vue'
import WardPaymentCompletePage from '@/pages/ward/payment/complete/[transactionId]/page.vue'
import WardPaymentHeldPage from '@/pages/ward/payment/held/[transactionId]/page.vue'
import WardPaymentPage from '@/pages/ward/payment/page.vue'
import WardPaymentQrPage from '@/pages/ward/payment/qr/[paymentId]/page.vue'
import {
  requireCompletedPayment,
  requireHeldPayment,
  requirePaymentQrSession,
} from '@/pages/ward/payment/-utils/payment-route-guard'
import WardTransferAccountPage from '@/pages/ward/transfer/account/page.vue'
import WardTransferAmountPage from '@/pages/ward/transfer/amount/page.vue'
import WardTransferBankPage from '@/pages/ward/transfer/bank/page.vue'
import WardTransferCanceledPage from '@/pages/ward/transfer/[transactionId]/canceled/page.vue'
import WardTransferCompletePage from '@/pages/ward/transfer/[transactionId]/complete/page.vue'
import WardTransferConfirmPage from '@/pages/ward/transfer/confirm/page.vue'
import WardTransferExpiredPage from '@/pages/ward/transfer/[transactionId]/expired/page.vue'
import WardTransferFailedPage from '@/pages/ward/transfer/[transactionId]/failed/page.vue'
import WardTransferHeldPage from '@/pages/ward/transfer/[transactionId]/held/page.vue'
import WardTransferPage from '@/pages/ward/transfer/page.vue'
import WardTransferPasswordPage from '@/pages/ward/transfer/password/page.vue'
import WardTransferProcessingPage from '@/pages/ward/transfer/processing/page.vue'
import WardTransferRejectedPage from '@/pages/ward/transfer/[transactionId]/rejected/page.vue'
import WardTransferRestrictedPage from '@/pages/ward/transfer/[transactionId]/restricted/page.vue'
import { requireWardTransaction } from '@/pages/ward/history/-utils/transaction-route-guard'
import {
  requireCompletedCharge,
  requireNewChargeAccount,
} from '@/pages/ward/charge/-utils/charge-route-guard'
import {
  requireCanceledTransfer,
  requireCompletedTransfer,
  requireExpiredTransfer,
  requireFailedTransfer,
  requireHeldTransfer,
  requirePendingTransfer,
  requireProcessingTransfer,
  requireRejectedTransfer,
  requireTransferAccount,
  requireTransferDraft,
  requireTransferIntent,
  requireTransferRecipient,
} from '@/pages/ward/transfer/-utils/transfer-route-guard'
import { getRoleHomePath } from '@/router/auth-navigation'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/auth/sign-in',
      name: 'auth-sign-in',
      component: SignInPage,
    },
    {
      path: '/auth/sign-up',
      name: 'auth-sign-up',
      component: SignUpPage,
    },
    {
      path: '/auth/sign-up/details',
      name: 'auth-sign-up-details',
      component: SignUpDetailsPage,
    },
    {
      path: '/auth/sign-up/terms/:termId',
      name: 'auth-sign-up-terms',
      component: SignUpTermsPage,
    },
    {
      path: '/guard',
      component: GuardLayout,
      children: [
        {
          path: '',
          name: 'guard-home',
          component: GuardPage,
          meta: { activeNavigation: 'home' },
        },
        {
          path: 'charge',
          name: 'guard-charge',
          component: GuardChargePage,
          meta: { activeNavigation: 'charge' },
        },
        {
          path: 'charge/be',
          name: 'guard-charge-be',
          component: GuardChargeBePage,
          meta: {
            activeNavigation: 'charge',
            showBottomNavigation: false,
          },
        },
        {
          path: 'charge/account',
          name: 'guard-charge-account',
          component: GuardChargeAccountPage,
          meta: {
            activeNavigation: 'charge',
            showBottomNavigation: false,
          },
        },
        {
          path: 'charge/password',
          name: 'guard-charge-password',
          component: GuardChargePasswordPage,
          meta: {
            activeNavigation: 'charge',
            showBottomNavigation: false,
          },
        },
        {
          path: 'charge/complete',
          name: 'guard-charge-complete',
          component: GuardChargeCompletePage,
          meta: {
            activeNavigation: 'charge',
            showBottomNavigation: false,
          },
        },
        {
          path: 'charge/:chargeId',
          name: 'guard-charge-detail',
          component: GuardChargeDetailPage,
          meta: {
            activeNavigation: 'charge',
            showBottomNavigation: false,
          },
        },
        {
          path: 'history',
          name: 'guard-history',
          component: GuardHistoryPage,
          meta: { activeNavigation: 'history' },
        },
        {
          path: 'safe-account',
          name: 'guard-safe-account',
          component: GuardSafeAccountPage,
          meta: {
            activeNavigation: 'home',
            showBottomNavigation: false,
          },
        },
        {
          path: 'safe-account/confirm',
          name: 'guard-safe-account-confirm',
          component: GuardSafeAccountConfirmPage,
          meta: {
            activeNavigation: 'home',
            showBottomNavigation: false,
          },
        },
        {
          path: 'history/:transactionId/decision-complete',
          name: 'guard-transaction-decision-complete',
          component: GuardTransactionDecisionCompletePage,
          meta: {
            activeNavigation: 'history',
            showBottomNavigation: false,
          },
        },
        {
          path: 'history/:transactionId',
          name: 'guard-transaction-detail',
          component: GuardTransactionDetailPage,
          meta: {
            activeNavigation: 'history',
            showBottomNavigation: false,
          },
        },
        {
          path: 'my',
          name: 'guard-my',
          component: GuardMyPage,
          meta: { activeNavigation: 'my' },
        },
      ],
    },
    {
      path: '/guard/pairing/code',
      name: 'guard-pairing-code',
      component: GuardPairingCodePage,
    },
    {
      path: '/ward',
      component: WardLayout,
      children: [
        {
          path: '',
          name: 'ward-home',
          component: WardPage,
          meta: { title: 'PayWith', activeNavigation: 'home' },
        },
        {
          path: 'pairing',
          name: 'ward-pairing',
          component: WardPairingPage,
          meta: {
            title: '인증 코드 입력',
            activeNavigation: 'payment',
            showBottomNavigation: false,
            backRouteName: 'ward-home',
          },
        },
        {
          path: 'pairing/complete',
          name: 'ward-pairing-complete',
          component: WardPairingCompletePage,
          beforeEnter: requireCompletedPairing,
          meta: {
            title: '연결 완료',
            activeNavigation: 'payment',
            showBottomNavigation: false,
            backRouteName: 'ward-home',
          },
        },
        {
          path: 'payment',
          name: 'ward-payment',
          component: WardPaymentPage,
          meta: {
            title: '비밀번호 입력',
            activeNavigation: 'payment',
            showBottomNavigation: false,
          },
        },
        {
          path: 'payment/qr/:paymentId',
          name: 'ward-payment-qr',
          component: WardPaymentQrPage,
          beforeEnter: requirePaymentQrSession,
          meta: {
            title: 'QR 결제',
            activeNavigation: 'payment',
            showBottomNavigation: false,
          },
        },
        {
          path: 'charge',
          name: 'ward-charge',
          component: WardChargePage,
          meta: {
            title: '충전하기',
            activeNavigation: 'payment',
          },
        },
        {
          path: 'charge/account/add',
          name: 'ward-charge-account-add',
          component: WardChargeAccountAddPage,
          meta: {
            title: '새 계좌 추가',
            activeNavigation: 'payment',
          },
        },
        {
          path: 'charge/account/complete',
          name: 'ward-charge-account-complete',
          component: WardChargeAccountCompletePage,
          beforeEnter: requireNewChargeAccount,
          meta: {
            title: '계좌 추가 완료',
            activeNavigation: 'payment',
            backRouteName: 'ward-charge',
          },
        },
        {
          path: 'charge/:transactionId/complete',
          name: 'ward-charge-complete',
          component: WardChargeCompletePage,
          beforeEnter: requireCompletedCharge,
          meta: {
            title: '충전 완료',
            activeNavigation: 'payment',
            backRouteName: 'ward-home',
          },
        },
        {
          path: 'history',
          name: 'ward-transaction-history',
          component: WardTransactionHistoryPage,
          meta: { title: '거래 내역' },
        },
        {
          path: 'history/:transactionId',
          name: 'ward-transaction-detail',
          component: WardTransactionDetailPage,
          beforeEnter: requireWardTransaction,
          meta: {
            title: '거래 내역 상세',
            backRouteName: 'ward-transaction-history',
          },
        },
        {
          path: 'payment/complete/:transactionId',
          name: 'ward-payment-complete',
          component: WardPaymentCompletePage,
          beforeEnter: requireCompletedPayment,
          meta: {
            title: '결제 완료',
            activeNavigation: 'payment',
            showBottomNavigation: false,
            backRouteName: 'ward-home',
          },
        },
        {
          path: 'payment/held/:transactionId',
          name: 'ward-payment-held',
          component: WardPaymentHeldPage,
          beforeEnter: requireHeldPayment,
          meta: {
            title: '결제 승인 대기',
            activeNavigation: 'payment',
            showBottomNavigation: false,
            backRouteName: 'ward-home',
          },
        },
        {
          path: 'transfer',
          name: 'ward-transfer',
          component: WardTransferPage,
          meta: { title: '송금 대상 선택', activeNavigation: 'transfer' },
        },
        {
          path: 'transfer/account',
          name: 'ward-transfer-account',
          component: WardTransferAccountPage,
          meta: {
            title: '계좌 번호 입력',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
        {
          path: 'transfer/bank',
          name: 'ward-transfer-bank',
          component: WardTransferBankPage,
          beforeEnter: requireTransferAccount,
          meta: {
            title: '은행 선택',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
        {
          path: 'transfer/amount',
          name: 'ward-transfer-amount',
          component: WardTransferAmountPage,
          beforeEnter: requireTransferRecipient,
          meta: {
            title: '송금 금액 입력',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
        {
          path: 'transfer/confirm',
          name: 'ward-transfer-confirm',
          component: WardTransferConfirmPage,
          beforeEnter: requireTransferDraft,
          meta: {
            title: '송금 확인',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
        {
          path: 'transfer/password',
          name: 'ward-transfer-password',
          component: WardTransferPasswordPage,
          beforeEnter: requireTransferIntent,
          meta: {
            title: '비밀번호 입력',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
        {
          path: 'transfer/processing',
          name: 'ward-transfer-processing',
          component: WardTransferProcessingPage,
          beforeEnter: requireProcessingTransfer,
          meta: {
            title: '송금 처리',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
        {
          path: 'transfer/:transactionId/held',
          name: 'ward-transfer-held',
          component: WardTransferHeldPage,
          beforeEnter: requireHeldTransfer,
          meta: {
            title: '이상 거래 알림',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
            backRouteName: 'ward-home',
          },
        },
        {
          path: 'transfer/:transactionId/restricted',
          name: 'ward-transfer-restricted',
          component: WardTransferRestrictedPage,
          beforeEnter: requirePendingTransfer,
          meta: {
            title: '거래 제한 안내',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
            backRouteName: 'ward-home',
          },
        },
        {
          path: 'transfer/:transactionId/rejected',
          name: 'ward-transfer-rejected',
          component: WardTransferRejectedPage,
          beforeEnter: requireRejectedTransfer,
          meta: {
            title: '거래 거절 안내',
            activeNavigation: 'transfer',
            backRouteName: 'ward-home',
          },
        },
        {
          path: 'transfer/:transactionId/complete',
          name: 'ward-transfer-complete',
          component: WardTransferCompletePage,
          beforeEnter: requireCompletedTransfer,
          meta: {
            title: '송금 완료',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
        {
          path: 'transfer/:transactionId/expired',
          name: 'ward-transfer-expired',
          component: WardTransferExpiredPage,
          beforeEnter: requireExpiredTransfer,
          meta: {
            title: '송금 만료',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
        {
          path: 'transfer/:transactionId/failed',
          name: 'ward-transfer-failed',
          component: WardTransferFailedPage,
          beforeEnter: requireFailedTransfer,
          meta: {
            title: '송금 실패',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
        {
          path: 'transfer/:transactionId/canceled',
          name: 'ward-transfer-canceled',
          component: WardTransferCanceledPage,
          beforeEnter: requireCanceledTransfer,
          meta: {
            title: '송금 취소',
            activeNavigation: 'transfer',
            showBottomNavigation: false,
          },
        },
      ],
    },
    {
      path: '/',
      redirect: '/auth/sign-in',
    },
  ],
})

async function getAuthenticatedUserId() {
  const accessToken = tokenStorage.getAccessToken()
  const userId = accessToken ? getUserIdFromAccessToken(accessToken) : null
  if (userId) return userId

  if (!tokenStorage.getRefreshToken()) return null

  try {
    const refreshedAccessToken = await refreshAccessToken()
    return getUserIdFromAccessToken(refreshedAccessToken)
  } catch {
    return null
  }
}

router.beforeEach(async (to) => {
  const isAuthRoute = to.path.startsWith('/auth')
  const userId = await getAuthenticatedUserId()

  if (!userId) {
    tokenStorage.clearTokens()
    return isAuthRoute ? true : { name: 'auth-sign-in' }
  }

  if (!isAuthRoute) return true

  try {
    const user = await getUser(userId)
    return getRoleHomePath(user.role)
  } catch {
    tokenStorage.clearTokens()
    return to.name === 'auth-sign-in' ? true : { name: 'auth-sign-in' }
  }
})

export default router
