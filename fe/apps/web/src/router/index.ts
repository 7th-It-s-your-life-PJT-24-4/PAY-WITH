import { createRouter, createWebHistory } from 'vue-router'

import WardLayout from '@/pages/ward/layout.vue'
import WardPage from '@/pages/ward/page.vue'

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
  ],
})

export default router
