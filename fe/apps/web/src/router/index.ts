import { createRouter, createWebHistory } from 'vue-router'

import WardLayout from '@/pages/ward/layout.vue'
import WardPage from '@/pages/ward/page.vue'
import WardPaymentCompletePage from '@/pages/ward/payment/complete.vue'
import WardPaymentPage from '@/pages/ward/payment/page.vue'
import WardPaymentPasswordPage from '@/pages/ward/payment/password.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
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
