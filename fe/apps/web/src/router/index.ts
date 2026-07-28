import { createRouter, createWebHistory } from 'vue-router'

import SignInPage from '@/pages/auth/sign-in/page.vue'
import KakaoCallbackPage from '@/pages/auth/kakao/callback/page.vue'
import SignUpPage from '@/pages/auth/sign-up/page.vue'
import SignUpDetailsPage from '@/pages/auth/sign-up/details/page.vue'
import SignUpTermsPage from '@/pages/auth/sign-up/terms/page.vue'
import WardLayout from '@/pages/ward/layout.vue'
import WardPage from '@/pages/ward/page.vue'
import WardPaymentCompletePage from '@/pages/ward/payment/complete.vue'
import WardPaymentPage from '@/pages/ward/payment/page.vue'
import WardPaymentPasswordPage from '@/pages/ward/payment/password.vue'
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
import {
  redirectPendingTransfer,
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
      path: '/ward',
      component: WardLayout,
      children: [
        {
          path: 'home',
          name: 'ward-home',
          component: WardPage,
          meta: { title: 'PayWith', activeNavigation: 'payment' },
        },
        {
          path: 'payment',
          name: 'ward-payment',
          component: WardPaymentPage,
          meta: { title: 'QR 결제', activeNavigation: 'payment' },
        },
        {
          path: 'payment/password',
          name: 'ward-payment-password',
          component: WardPaymentPasswordPage,
          meta: {
            title: '비밀번호 입력',
            activeNavigation: 'payment',
            showBottomNavigation: false,
          },
        },
        {
          path: 'payment/complete',
          name: 'ward-payment-complete',
          component: WardPaymentCompletePage,
          meta: { title: '결제 완료', activeNavigation: 'payment' },
        },
        {
          path: 'transfer',
          name: 'ward-transfer',
          component: WardTransferPage,
          beforeEnter: redirectPendingTransfer,
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
