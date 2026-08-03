import { createRouter, createWebHistory } from 'vue-router'

import SignInPage from '@/pages/auth/sign-in/page.vue'
import KakaoCallbackPage from '@/pages/auth/kakao/callback/page.vue'
import SignUpPage from '@/pages/auth/sign-up/page.vue'
import SignUpDetailsPage from '@/pages/auth/sign-up/details/page.vue'
import SignUpTermsPage from '@/pages/auth/sign-up/terms/page.vue'
import GuardChargePage from '@/pages/guard/charge/page.vue'
import GuardChargeAccountPage from '@/pages/guard/charge/account/page.vue'
import GuardChargeBePage from '@/pages/guard/charge/be/page.vue'
import GuardChargeCompletePage from '@/pages/guard/charge/complete/page.vue'
import GuardChargePasswordPage from '@/pages/guard/charge/password/page.vue'
import GuardHistoryPage from '@/pages/guard/history/page.vue'
import GuardLayout from '@/pages/guard/layout.vue'
import GuardMyPage from '@/pages/guard/my/page.vue'
import GuardPage from '@/pages/guard/page.vue'
import GuardPairingCodePage from '@/pages/guard/pairing/code.vue'
import WardPairingCompletePage from '@/pages/ward/pairing/complete.vue'
import WardPairingPage from '@/pages/ward/pairing/page.vue'
import { requireCompletedPairing } from '@/pages/ward/pairing/-utils/pairing-route-guard'
import WardChargeAccountAddPage from '@/pages/ward/charge/account/add.vue'
import WardChargeAccountCompletePage from '@/pages/ward/charge/account/complete.vue'
import WardChargeCompletePage from '@/pages/ward/charge/complete.vue'
import WardChargePage from '@/pages/ward/charge/page.vue'
import WardLayout from '@/pages/ward/layout.vue'
import WardPage from '@/pages/ward/page.vue'
import WardTransactionDetailPage from '@/pages/ward/history/detail.vue'
import WardTransactionHistoryPage from '@/pages/ward/history/page.vue'
import WardPaymentCompletePage from '@/pages/ward/payment/complete.vue'
import WardPaymentHeldPage from '@/pages/ward/payment/held.vue'
import WardPaymentPage from '@/pages/ward/payment/page.vue'
import WardPaymentQrPage from '@/pages/ward/payment/qr.vue'
import {
  requireCompletedPayment,
  requireHeldPayment,
  requirePaymentQrSession,
} from '@/pages/ward/payment/-utils/payment-route-guard'
import WardTransferAccountPage from '@/pages/ward/transfer/account.vue'
import WardTransferAmountPage from '@/pages/ward/transfer/amount.vue'
import WardTransferBankPage from '@/pages/ward/transfer/bank.vue'
import WardTransferCompletePage from '@/pages/ward/transfer/complete.vue'
import WardTransferConfirmPage from '@/pages/ward/transfer/confirm.vue'
import WardTransferHeldPage from '@/pages/ward/transfer/held.vue'
import WardTransferPage from '@/pages/ward/transfer/page.vue'
import WardTransferPasswordPage from '@/pages/ward/transfer/password.vue'
import WardTransferProcessingPage from '@/pages/ward/transfer/processing.vue'
import WardTransferRejectedPage from '@/pages/ward/transfer/rejected.vue'
import WardTransferRestrictedPage from '@/pages/ward/transfer/restricted.vue'
import { requireWardTransaction } from '@/pages/ward/history/-utils/transaction-route-guard'
import {
  requireCompletedCharge,
  requireNewChargeAccount,
} from '@/pages/ward/charge/-utils/charge-route-guard'
import {
  requireCompletedTransfer,
  requireHeldTransfer,
  requirePendingTransfer,
  requireProcessingTransfer,
  requireRejectedTransfer,
  requireTransferAccount,
  requireTransferDraft,
  requireTransferIntent,
  requireTransferRecipient,
} from '@/pages/ward/transfer/-utils/transfer-route-guard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/auth/sign-in',
      name: 'auth-sign-in',
      component: SignInPage,
    },
    {
      path: '/auth/kakao/callback',
      name: 'auth-kakao-callback',
      component: KakaoCallbackPage,
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
          path: 'history',
          name: 'guard-history',
          component: GuardHistoryPage,
          meta: { activeNavigation: 'history' },
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
          path: 'home',
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
          path: 'payment/:paymentId/qr',
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
          path: 'payment/:transactionId/complete',
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
          path: 'payment/:transactionId/held',
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
      ],
    },
    {
      path: '/',
      redirect: '/ward/home',
    },
  ],
})

export default router
