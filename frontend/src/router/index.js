import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../stores/auth.js'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/auth/LoginView.vue'), meta: { guestOnly: true } },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/home/HomeView.vue') },
      { path: 'search', name: 'search', component: () => import('../views/search/SearchView.vue') },
      { path: 'topics/:topicId', name: 'topic-detail', component: () => import('../views/topic/TopicDetailView.vue') },
      { path: 'circles/create', name: 'circle-create', component: () => import('../views/circle/CreateCircleView.vue'), meta: { requiresAuth: true } },
      { path: 'circles/:circleId', name: 'circle-detail', component: () => import('../views/circle/CircleDetailView.vue') },
      { path: 'me/interests', name: 'my-interests', component: () => import('../views/me/MyInterestsView.vue'), meta: { requiresAuth: true } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach((to) => {
  if (to.matched.some((record) => record.meta.requiresAuth) && !isAuthenticated()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && isAuthenticated()) return { name: 'home' }
  return true
})

export default router
