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
      ],
    },
    {
      path: '/',
      redirect: '/ward/home',
    },
  ],
})

export default router
