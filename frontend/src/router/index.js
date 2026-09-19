import { createRouter, createWebHistory } from 'vue-router'
import { authState, isAdmin, isAuthenticated, rolesAreLoaded, setRoles } from '../stores/auth.js'
import { getMyRoles } from '../api/admin.js'
import { canAccessRoles } from '../utils/adminRole.js'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/auth/LoginView.vue'), meta: { guestOnly: true } },
  { path: '/forbidden', name: 'forbidden', component: () => import('../views/error/ForbiddenView.vue') },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', name: 'admin-dashboard', component: () => import('../views/admin/AdminDashboardView.vue') },
      { path: 'circles', name: 'admin-circles', component: () => import('../views/admin/CircleModerationView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'posts', name: 'admin-posts', component: () => import('../views/admin/PostModerationView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'comments', name: 'admin-comments', component: () => import('../views/admin/CommentManagementView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'meetup-reviews', name: 'admin-meetup-reviews', component: () => import('../views/admin/MeetupReviewManagementView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'meetups', name: 'admin-meetups', component: () => import('../views/admin/MeetupManagementView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'search', name: 'admin-search', component: () => import('../views/admin/SearchManagementView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'reports', name: 'admin-reports', component: () => import('../views/admin/ContentReportManagementView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'appeals', name: 'admin-appeals', component: () => import('../views/admin/ContentAppealManagementView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'violations', name: 'admin-violations', component: () => import('../views/admin/ContentViolationManagementView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'catalog', name: 'admin-catalog', component: () => import('../views/admin/CatalogManagementView.vue'), meta: { requiredRoles: ['CONTENT_ADMIN'] } },
      { path: 'risk', name: 'admin-risk', component: () => import('../views/admin/RiskRestrictionManagementView.vue'), meta: { requiredRoles: ['RISK_REVIEWER'] } },
      { path: 'roles', name: 'admin-roles', component: () => import('../views/admin/AdminRoleManagementView.vue'), meta: { requiredRoles: ['SUPER_ADMIN'] } },
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
      { path: 'circles/:circleId/edit', name: 'circle-edit', component: () => import('../views/circle/CreateCircleView.vue'), meta: { requiresAuth: true } },
      { path: 'circles/:circleId', name: 'circle-detail', component: () => import('../views/circle/CircleDetailView.vue') },
      { path: 'posts/create', name: 'post-create', component: () => import('../views/post/CreatePostView.vue'), meta: { requiresAuth: true } },
      { path: 'posts/:postId', name: 'post-detail', component: () => import('../views/post/PostDetailView.vue') },
      { path: 'meetups', name: 'meetup-list', component: () => import('../views/meetup/MeetupListView.vue') },
      { path: 'meetups/create', name: 'meetup-create', component: () => import('../views/meetup/CreateMeetupView.vue'), meta: { requiresAuth: true } },
      { path: 'meetups/:meetupId/edit', name: 'meetup-edit', component: () => import('../views/meetup/CreateMeetupView.vue'), meta: { requiresAuth: true } },
      { path: 'meetups/:meetupId', name: 'meetup-detail', component: () => import('../views/meetup/MeetupDetailView.vue') },
      { path: 'profiles/:accountId', name: 'public-profile', component: () => import('../views/profile/PublicProfileView.vue') },
      { path: 'me/profile', name: 'my-profile', component: () => import('../views/me/MyProfileView.vue'), meta: { requiresAuth: true } },
      { path: 'me/account', name: 'my-account', component: () => import('../views/me/AccountSettingsView.vue'), meta: { requiresAuth: true } },
      { path: 'me/notifications', name: 'my-notifications', component: () => import('../views/me/NotificationCenterView.vue'), meta: { requiresAuth: true } },
      { path: 'me/reports', name: 'my-reports', component: () => import('../views/me/MyReportsView.vue'), meta: { requiresAuth: true } },
      { path: 'me/appeals/create', name: 'appeal-create', component: () => import('../views/me/CreateAppealView.vue'), meta: { requiresAuth: true } },
      { path: 'me/appeals', name: 'my-appeals', component: () => import('../views/me/MyAppealsView.vue'), meta: { requiresAuth: true } },
      { path: 'me/account-safety', name: 'account-safety', component: () => import('../views/me/AccountSafetyView.vue'), meta: { requiresAuth: true } },
      { path: 'me/interests', name: 'my-interests', component: () => import('../views/me/MyInterestsView.vue'), meta: { requiresAuth: true } },
      { path: 'me/feed', name: 'my-feed', component: () => import('../views/me/PostCollectionView.vue'), props: { mode: 'feed' }, meta: { requiresAuth: true } },
      { path: 'me/bookmarks', name: 'my-bookmarks', component: () => import('../views/me/PostCollectionView.vue'), props: { mode: 'bookmarks' }, meta: { requiresAuth: true } },
      { path: 'me/posts', name: 'my-posts', component: () => import('../views/me/MyPostsView.vue'), meta: { requiresAuth: true } },
      { path: 'me/meetups', name: 'my-meetups', component: () => import('../views/me/MyMeetupsView.vue'), meta: { requiresAuth: true } },
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
  if (to.matched.some((record) => record.meta.requiresAdmin)) {
    if (!rolesAreLoaded()) {
      try { setRoles((await getMyRoles()).data.roles) } catch { return { name: 'forbidden' } }
    }
    if (!isAdmin()) return { name: 'forbidden' }
    const requiredRoles = to.matched.flatMap((record) => record.meta.requiredRoles ?? [])
    if (!canAccessRoles(authState.roles, requiredRoles)) return { name: 'forbidden' }
  }
  if (to.meta.guestOnly && isAuthenticated()) return { name: 'home' }
  return true
})

export default router
