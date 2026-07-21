import { createRouter, createWebHistory } from 'vue-router'

import WardLayout from '@/pages/ward/layout.vue'
import WardPage from '@/pages/ward/page.vue'
import WardHomePage from '@/pages/WardHomePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: WardLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: WardPage,
        },
      ],
    },
    {
      path: '/ward/home',
      name: 'ward-home',
      component: WardHomePage,
    },
  ],
})

export default router
