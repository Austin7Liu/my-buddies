import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../stores/auth.js'
import { isContentAdmin, rolesAreLoaded, setRoles } from '../stores/auth.js'
import { getMyRoles } from '../api/admin.js'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/auth/LoginView.vue'), meta: { guestOnly: true } },
  { path: '/forbidden', name: 'forbidden', component: () => import('../views/error/ForbiddenView.vue') },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresContentAdmin: true },
    children: [
      { path: '', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboardView.vue') },
      { path: 'circles', name: 'admin-circles', component: () => import('../views/admin/CircleModerationView.vue') },
      { path: 'posts', name: 'admin-posts', component: () => import('../views/admin/PostModerationView.vue') },
      { path: 'meetups', name: 'admin-meetups', component: () => import('../views/admin/MeetupManagementView.vue') },
      { path: 'search', name: 'admin-search', component: () => import('../views/admin/SearchManagementView.vue') },
    ],
  },
  {
    path: '/',
    component: () => import('../layouts/AppLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/home/HomeView.vue') },
      { path: 'search', name: 'search', component: () => import('../views/search/SearchView.vue') },
      { path: 'topics/:topicId', name: 'topic-detail', component: () => import('../views/topic/TopicDetailView.vue') },
      { path: 'circles/create', name: 'circle-create', component: () => import('../views/circle/CreateCircleView.vue'), meta: { requiresAuth: true } },
      { path: 'circles/:circleId', name: 'circle-detail', component: () => import('../views/circle/CircleDetailView.vue') },
      { path: 'posts/create', name: 'post-create', component: () => import('../views/post/CreatePostView.vue'), meta: { requiresAuth: true } },
      { path: 'posts/:postId', name: 'post-detail', component: () => import('../views/post/PostDetailView.vue') },
      { path: 'me/interests', name: 'my-interests', component: () => import('../views/me/MyInterestsView.vue'), meta: { requiresAuth: true } },
      { path: 'me/feed', name: 'my-feed', component: () => import('../views/me/PostCollectionView.vue'), props: { mode: 'feed' }, meta: { requiresAuth: true } },
      { path: 'me/bookmarks', name: 'my-bookmarks', component: () => import('../views/me/PostCollectionView.vue'), props: { mode: 'bookmarks' }, meta: { requiresAuth: true } },
      { path: 'me/posts', name: 'my-posts', component: () => import('../views/me/MyPostsView.vue'), meta: { requiresAuth: true } },
      { path: 'me/identity', name: 'my-identity', component: () => import('../views/me/IdentityVerificationView.vue'), meta: { requiresAuth: true } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach(async (to) => {
  if (to.matched.some((record) => record.meta.requiresAuth) && !isAuthenticated()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.matched.some((record) => record.meta.requiresContentAdmin)) {
    if (!rolesAreLoaded()) {
      try { setRoles((await getMyRoles()).data.roles) } catch { return { name: 'forbidden' } }
    }
    if (!isContentAdmin()) return { name: 'forbidden' }
  }
  if (to.meta.guestOnly && isAuthenticated()) return { name: 'home' }
  return true
})

export default router
