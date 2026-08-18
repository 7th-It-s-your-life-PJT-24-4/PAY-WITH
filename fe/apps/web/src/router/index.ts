import { createRouter, createWebHistory } from 'vue-router'

import { expireAuthenticationSession } from '@/api/auth-session'
import { isUnauthorizedApiError } from '@/api/error'
import {
  refreshAccessToken,
  scheduleAccessTokenRefresh,
} from '@/api/token-refresh'
import {
  getAccessTokenExpiresAt,
  getUserIdFromAccessToken,
  isAccessTokenExpiring,
  tokenStorage,
} from '@/api/token-storage'
import { getUser } from '@/api/users'
import SignInPage from '@/pages/auth/sign-in/page.vue'
import SignUpPage from '@/pages/auth/sign-up/page.vue'
import SignUpDetailsPage from '@/pages/auth/sign-up/details/page.vue'
import SignUpTermsPage from '@/pages/auth/sign-up/terms/page.vue'
import { requireSignUpRole } from '@/pages/auth/sign-up/-utils/sign-up-route-guard'
import GuardChargePage from '@/pages/guard/charge/page.vue'
import GuardChargeAccountPage from '@/pages/guard/charge/account/page.vue'
import GuardChargeBePage from '@/pages/guard/charge/be/page.vue'
import GuardChargeCompletePage from '@/pages/guard/charge/complete/page.vue'
import GuardChargeDetailPage from '@/pages/guard/charge/[id]/page.vue'
import GuardChargePasswordPage from '@/pages/guard/charge/password/page.vue'
import GuardApprovalDecisionCompletePage from '@/pages/guard/approval-requests/[id]/decision-complete.vue'
import GuardApprovalRequestDetailPage from '@/pages/guard/approval-requests/[id]/page.vue'
import GuardApprovalRequestsPage from '@/pages/guard/approval-requests/page.vue'
import GuardTransactionDetailPage from '@/pages/guard/history/[id]/page.vue'
import GuardHistoryPage from '@/pages/guard/history/page.vue'
import GuardLayout from '@/pages/guard/layout.vue'
import GuardMyPage from '@/pages/guard/my/page.vue'
import GuardMyPushNotificationsPage from '@/pages/guard/my/push-notifications/page.vue'
import GuardMyProfileEditPage from '@/pages/guard/my/profile/edit.vue'
import GuardMyProfilePage from '@/pages/guard/my/profile/page.vue'
import GuardMySeniorsPage from '@/pages/guard/my/seniors/page.vue'
import GuardPage from '@/pages/guard/page.vue'
import GuardPushNotificationOnboardingPage from '@/pages/guard/onboarding/push-notifications/page.vue'
import GuardPairingCodePage from '@/pages/guard/pairing/code.vue'
import GuardSafeAccountConfirmPage from '@/pages/guard/safe-account/confirm/page.vue'
import GuardSafeAccountAddPage from '@/pages/guard/safe-account/add/page.vue'
import GuardSafeAccountPage from '@/pages/guard/safe-account/page.vue'
import { requireSafeAccountDraft } from '@/pages/guard/safe-account/-utils/safe-account-route-guard'
import WardPairingCompletePage from '@/pages/ward/pairing/complete/page.vue'
import WardPairingPendingPage from '@/pages/ward/pairing/pending/page.vue'
import WardPairingPage from '@/pages/ward/pairing/page.vue'
import WardApprovalRequestDetailPage from '@/pages/ward/approval-requests/[approvalId]/page.vue'
import WardPendingTransactionsPage from '@/pages/ward/pending-transactions/page.vue'
import { requireCompletedPairing } from '@/pages/ward/pairing/-utils/pairing-route-guard'
import WardChargeAccountAddPage from '@/pages/ward/charge/account/add/page.vue'
import WardChargeAccountCompletePage from '@/pages/ward/charge/account/complete/page.vue'
import WardChargeCompletePage from '@/pages/ward/charge/[transactionId]/complete/page.vue'
import WardChargePage from '@/pages/ward/charge/page.vue'
import WardLayout from '@/pages/ward/layout.vue'
import WardMyGuardianPage from '@/pages/ward/my/guardian/page.vue'
import WardMyPage from '@/pages/ward/my/page.vue'
import WardMyProfilePage from '@/pages/ward/my/profile/page.vue'
import WardPage from '@/pages/ward/page.vue'
import WardTransactionDetailPage from '@/pages/ward/history/[transactionId]/page.vue'
import WardTransactionHistoryPage from '@/pages/ward/history/page.vue'
import WardPaymentCompletePage from '@/pages/ward/payment/complete/[transactionId]/page.vue'
import WardPaymentFailedPage from '@/pages/ward/payment/failed/[paymentId]/page.vue'
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
  requireTransferDraft,
  requireTransferIntent,
  requireTransferRecipient,
} from '@/pages/ward/transfer/-utils/transfer-route-guard'
import {
  getRoleHomePath,
  getUnauthenticatedSignInQuery,
} from '@/router/auth-navigation'

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
      beforeEnter: requireSignUpRole,
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
          path: 'onboarding/push-notifications',
          name: 'guard-push-notification-onboarding',
          component: GuardPushNotificationOnboardingPage,
          meta: {
            activeNavigation: 'home',
            showBottomNavigation: false,
          },
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
          path: 'charge/:id(\\d+)',
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
          path: 'approval-requests',
          name: 'guard-approval-requests',
          component: GuardApprovalRequestsPage,
          meta: { activeNavigation: 'home' },
        },
        {
          path: 'approval-requests/:id(\\d+)/complete',
          name: 'guard-approval-decision-complete',
          component: GuardApprovalDecisionCompletePage,
          meta: {
            activeNavigation: 'home',
            showBottomNavigation: false,
          },
        },
        {
          path: 'approval-requests/:id(\\d+)',
          name: 'guard-approval-request-detail',
          component: GuardApprovalRequestDetailPage,
          meta: {
            activeNavigation: 'home',
            showBottomNavigation: false,
          },
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
          beforeEnter: requireSafeAccountDraft,
          meta: {
            activeNavigation: 'home',
            showBottomNavigation: false,
          },
        },
        {
          path: 'safe-account/add',
          name: 'guard-safe-account-add',
          component: GuardSafeAccountAddPage,
          meta: {
            activeNavigation: 'home',
            showBottomNavigation: false,
          },
        },
        {
          path: 'history/:id(\\d+)',
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
        {
          path: 'my/profile',
          name: 'guard-my-profile',
          component: GuardMyProfilePage,
          meta: { activeNavigation: 'my', showBottomNavigation: false },
        },
        {
          path: 'my/profile/edit',
          name: 'guard-my-profile-edit',
          component: GuardMyProfileEditPage,
          meta: { activeNavigation: 'my', showBottomNavigation: false },
        },
        {
          path: 'my/seniors',
          name: 'guard-my-seniors',
          component: GuardMySeniorsPage,
          meta: { activeNavigation: 'my', showBottomNavigation: false },
        },
        {
          path: 'my/push-notifications',
          name: 'guard-my-push-notifications',
          component: GuardMyPushNotificationsPage,
          meta: { activeNavigation: 'my' },
        },
        {
          path: 'my/terms/:termId',
          name: 'guard-my-terms',
          component: SignUpTermsPage,
          meta: { activeNavigation: 'my', showBottomNavigation: false },
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
          meta: {
            title: 'PayWith',
            activeNavigation: 'home',
            showBack: false,
          },
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
          path: 'approval-requests/:approvalId',
          name: 'ward-approval-request-detail',
          component: WardApprovalRequestDetailPage,
          meta: {
            title: '승인 대기 송금',
            activeNavigation: 'home',
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
          path: 'pairing/pending',
          name: 'ward-pairing-pending',
          component: WardPairingPendingPage,
          meta: {
            title: '연결 요청',
            activeNavigation: 'payment',
            showBottomNavigation: false,
            backRouteName: 'ward-home',
          },
        },
        {
          path: 'payment',
          name: 'ward-payment',
          component: WardPaymentPage,
          beforeEnter: requireCompletedPairing,
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
          path: 'my',
          name: 'ward-my',
          component: WardMyPage,
          meta: {
            title: '마이페이지',
            activeNavigation: 'home',
          },
        },
        {
          path: 'my/profile',
          name: 'ward-my-profile',
          component: WardMyProfilePage,
          meta: {
            title: '내 정보',
            activeNavigation: 'home',
            backRouteName: 'ward-my',
          },
        },
        {
          path: 'my/guardian',
          name: 'ward-my-guardian',
          component: WardMyGuardianPage,
          meta: {
            title: '보호자 관리',
            activeNavigation: 'home',
            backRouteName: 'ward-my',
          },
        },
        {
          path: 'my/terms/:termId',
          name: 'ward-my-terms',
          component: SignUpTermsPage,
          meta: {
            title: '약관 상세',
            activeNavigation: 'home',
            showBottomNavigation: false,
            backRouteName: 'ward-my',
            hideInnerHeader: true,
          },
        },
        {
          path: 'pending-transactions',
          name: 'ward-pending-transactions',
          component: WardPendingTransactionsPage,
          meta: {
            title: '승인 대기 거래',
            activeNavigation: 'home',
            backRouteName: 'ward-home',
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
          path: 'payment/failed/:paymentId',
          name: 'ward-payment-failed',
          component: WardPaymentFailedPage,
          meta: {
            title: '결제 실패',
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
          beforeEnter: requireCompletedPairing,
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
      // 가맹점 스캐너는 로그인 계정이 없는 제3 액터라 인증 없이 접근한다.
      // QR 디코더가 무거워 이 라우트에서만 지연 로딩한다.
      path: '/scanner',
      name: 'scanner',
      component: () => import('@/pages/scanner/page.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      redirect: '/auth/sign-in',
    },
  ],
})

async function resolveAuthentication() {
  const accessToken = tokenStorage.getAccessToken()
  const userId = accessToken ? getUserIdFromAccessToken(accessToken) : null
  const accessTokenExpiresAt = accessToken
    ? getAccessTokenExpiresAt(accessToken)
    : null
  const hasUsableAccessToken =
    userId !== null &&
    accessTokenExpiresAt !== null &&
    accessTokenExpiresAt > Date.now()

  if (
    accessToken &&
    hasUsableAccessToken &&
    (!isAccessTokenExpiring(accessToken) || !tokenStorage.getRefreshToken())
  ) {
    scheduleAccessTokenRefresh()
    return { userId, sessionExpired: false }
  }

  if (!tokenStorage.getRefreshToken()) {
    return {
      userId: null,
      sessionExpired:
        accessTokenExpiresAt !== null && accessTokenExpiresAt <= Date.now(),
    }
  }

  try {
    const refreshedAccessToken = await refreshAccessToken()
    return {
      userId: getUserIdFromAccessToken(refreshedAccessToken),
      sessionExpired: false,
    }
  } catch (error) {
    const sessionExpired = isUnauthorizedApiError(error)
    return {
      // 일시 장애라면 서버가 최종 인증을 판단하도록 현재 화면 접근은 유지한다.
      // 실제 만료(401)일 때만 인증 사용자 정보를 폐기한다.
      userId: sessionExpired ? null : userId,
      sessionExpired,
    }
  }
}

router.beforeEach(async (to) => {
  // 공개 라우트는 인증 해석 자체를 건너뛴다 — 스캐너는 토큰이 없는 것이 정상 상태다
  if (to.meta.public) return true

  const isAuthRoute = to.path.startsWith('/auth')
  const { userId, sessionExpired } = await resolveAuthentication()

  if (!userId) {
    if (sessionExpired) expireAuthenticationSession()
    if (isAuthRoute) return true

    return {
      name: 'auth-sign-in',
      query: getUnauthenticatedSignInQuery(
        to.fullPath,
        to.query.source,
        sessionExpired,
      ),
    }
  }

  if (!isAuthRoute) {
    if (to.query.source !== 'push') return true

    try {
      const user = await getUser(userId)
      const expectedRole =
        to.path === '/guard' || to.path.startsWith('/guard/')
          ? 'GUARD'
          : to.path === '/ward' || to.path.startsWith('/ward/')
            ? 'WARD'
            : null
      return expectedRole && user.role !== expectedRole
        ? getRoleHomePath(user.role)
        : true
    } catch (error) {
      if (!isUnauthorizedApiError(error)) return true

      expireAuthenticationSession()
      return {
        name: 'auth-sign-in',
        query: { reason: 'session-expired', redirect: to.fullPath },
      }
    }
  }

  try {
    const user = await getUser(userId)
    return getRoleHomePath(user.role)
  } catch (error) {
    if (!isUnauthorizedApiError(error)) return true

    expireAuthenticationSession()
    return {
      name: 'auth-sign-in',
      query: { reason: 'session-expired' },
    }
  }
})

export default router
