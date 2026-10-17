import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

function redirectHome() {
  const userStore = useUserStore()
  if (!userStore.token) return '/login'
  return userStore.homePath()
}

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/login/sso-callback', name: 'SsoCallback', component: () => import('../views/SsoCallback.vue') },
  { path: '/help', redirect: redirectHome },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: redirectHome,
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { fullAdmin: true } },
      { path: 'experts', name: 'Experts', component: () => import('../views/Experts.vue'), meta: { fullAdmin: true } },
      { path: 'tasks', name: 'Tasks', component: () => import('../views/Tasks.vue'), meta: { fullAdmin: true } },
      { path: 'tasks/:id', name: 'TaskDetail', component: () => import('../views/TaskDetail.vue'), meta: { fullAdmin: true } },
      { path: 'summary/:taskId', name: 'Summary', component: () => import('../views/Summary.vue'), meta: { summary: true } },
      { path: 'analytics', name: 'Analytics', component: () => import('../views/Analytics.vue'), meta: { analytics: true } },
      { path: 'audit', name: 'Audit', component: () => import('../views/Audit.vue'), meta: { fullAdmin: true } },
      { path: 'users', name: 'Users', component: () => import('../views/Users.vue'), meta: { systemAdmin: true } },
      { path: 'settings', name: 'Settings', component: () => import('../views/Settings.vue') },
      { path: 'expert/workbench', name: 'ExpertWorkbench', component: () => import('../views/ExpertWorkbench.vue'), meta: { expert: true } },
      { path: 'notifications', name: 'Notifications', component: () => import('../views/Notifications.vue') },
      { path: 'leader/tasks', name: 'LeaderTasks', component: () => import('../views/LeaderTasks.vue'), meta: { leader: true } },
      { path: 'leader/summary/:taskId', name: 'LeaderSummary', component: () => import('../views/Summary.vue'), meta: { leader: true, summary: true } },
      { path: 'my-reviews', name: 'MyReviews', component: () => import('../views/MyReviews.vue'), meta: { expert: true } },
      { path: 'my-reviews/:id', name: 'ReviewDetail', component: () => import('../views/ReviewDetail.vue'), meta: { expert: true } },
      { path: 'my-projects', name: 'MyProjects', component: () => import('../views/MyProjects.vue'), meta: { applicant: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: redirectHome }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login' || to.path === '/login/sso-callback') {
    next()
    return
  }
  if (!userStore.token) {
    next('/login')
    return
  }
  if (to.meta.systemAdmin && !userStore.isSystemAdmin) {
    next(userStore.homePath())
    return
  }
  if (to.meta.fullAdmin && !userStore.isFullAdmin) {
    const target = userStore.homePath()
    next(target === to.path ? '/settings' : target)
    return
  }
  if (to.meta.expert && userStore.isLeaderOnly) {
    next('/leader/tasks')
    return
  }
  if (to.meta.applicant && !userStore.isApplicant) {
    next(userStore.homePath())
    return
  }
  if (to.meta.applicant && userStore.isFullAdmin && !userStore.isApplicant) {
    next(userStore.homePath())
    return
  }
  if (to.meta.expert && userStore.isFullAdmin && !userStore.isExpert) {
    next(userStore.homePath())
    return
  }
  if (to.meta.leader && !userStore.isLeader && !userStore.isFullAdmin) {
    next(userStore.homePath())
    return
  }
  if (to.meta.analytics && userStore.isExpert && !userStore.isLeader && !userStore.isFullAdmin) {
    next(userStore.homePath())
    return
  }
  if (to.name === 'Summary' && userStore.isLeaderOnly) {
    next({ name: 'LeaderSummary', params: { taskId: to.params.taskId } })
    return
  }
  next()
})

export default router
