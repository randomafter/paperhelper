import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { guest: true } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { guest: true } },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('../views/ForgotPassword.vue'), meta: { guest: true } },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'Home', component: () => import('../views/Home.vue') },
      { path: 'profile', name: 'Profile', component: () => import('../views/Profile.vue') },
      { path: 'materials', name: 'Materials', component: () => import('../views/Materials.vue') },
      { path: 'favorites', name: 'Favorites', component: () => import('../views/Favorites.vue') },
      { path: 'works', name: 'Works', component: () => import('../views/Works.vue') },
      { path: 'workspace', name: 'Workspace', component: () => import('../views/Workspace.vue') },
      { path: 'materials/:id', name: 'MaterialDetail', component: () => import('../views/MaterialDetail.vue') },
      { path: 'admin/materials', name: 'AdminMaterials', component: () => import('../views/AdminMaterials.vue'), meta: { admin: true } },
      { path: 'admin/review', name: 'AdminReview', component: () => import('../views/AdminReview.vue'), meta: { admin: true } },
      { path: 'admin/feedback', name: 'AdminFeedback', component: () => import('../views/AdminFeedback.vue'), meta: { admin: true } },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  const token = auth.token
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.admin && auth.user?.role !== 'ADMIN') {
    next({ name: 'Home' })
  } else if (to.meta.guest && token) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
