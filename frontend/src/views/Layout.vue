<template>
  <el-container class="layout">
    <el-aside :width="asideWidth" class="aside" :class="{ 'aside--collapsed': collapsed }">
      <div class="logo">
        <div class="logo-mark">
          <AppIcon name="layout-dashboard" :size="20" />
        </div>
        <span v-show="!collapsed" class="logo-title">专家评审系统</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="side-menu"
          background-color="transparent"
          text-color="rgba(255,255,255,0.72)"
          active-text-color="#ffffff"
        >
          <template v-if="userStore.isAdmin">
            <el-menu-item index="/dashboard"><AppIcon name="layout-dashboard" :size="18" class="menu-item-icon" /><span class="menu-item-label">进度看板</span></el-menu-item>
            <el-menu-item index="/experts"><AppIcon name="users" :size="18" class="menu-item-icon" /><span class="menu-item-label">专家库</span></el-menu-item>
            <el-menu-item index="/tasks"><AppIcon name="file-text" :size="18" class="menu-item-icon" /><span class="menu-item-label">评审任务</span></el-menu-item>
            <el-menu-item v-if="userStore.isSystemAdmin" index="/users"><AppIcon name="user-cog" :size="18" class="menu-item-icon" /><span class="menu-item-label">用户管理</span></el-menu-item>
            <el-menu-item index="/analytics"><AppIcon name="pie-chart" :size="18" class="menu-item-icon" /><span class="menu-item-label">统计分析</span></el-menu-item>
            <el-menu-item index="/notifications">
              <AppIcon name="bell" :size="18" class="menu-item-icon" />
              <span class="menu-item-label">消息中心</span>
              <el-badge v-if="unreadCount" :value="unreadCount" :max="99" class="menu-badge" />
            </el-menu-item>
            <el-menu-item index="/audit"><AppIcon name="scroll-text" :size="18" class="menu-item-icon" /><span class="menu-item-label">审计日志</span></el-menu-item>
            <el-menu-item index="/settings"><AppIcon name="settings" :size="18" class="menu-item-icon" /><span class="menu-item-label">系统设置</span></el-menu-item>
          </template>
          <template v-if="userStore.isLeaderOnly">
            <el-menu-item index="/leader/tasks"><AppIcon name="file-text" :size="18" class="menu-item-icon" /><span class="menu-item-label">评审汇总</span></el-menu-item>
            <el-menu-item index="/analytics"><AppIcon name="pie-chart" :size="18" class="menu-item-icon" /><span class="menu-item-label">统计分析</span></el-menu-item>
          </template>
          <template v-if="userStore.isExpert">
            <el-menu-item index="/expert/workbench"><AppIcon name="layout-dashboard" :size="18" class="menu-item-icon" /><span class="menu-item-label">工作台</span></el-menu-item>
            <el-menu-item index="/my-reviews">
              <AppIcon name="clipboard-pen" :size="18" class="menu-item-icon" />
              <span class="menu-item-label">我的评审</span>
              <el-badge v-if="todoCount" :value="todoCount" :max="99" class="menu-badge" />
            </el-menu-item>
          </template>
          <template v-if="userStore.isApplicant">
            <el-menu-item index="/my-projects"><AppIcon name="folder-open" :size="18" class="menu-item-icon" /><span class="menu-item-label">我的申报</span></el-menu-item>
          </template>
          <template v-if="!userStore.isAdmin">
            <el-menu-item index="/settings"><AppIcon name="settings" :size="18" class="menu-item-icon" /><span class="menu-item-label">系统设置</span></el-menu-item>
            <el-menu-item index="/notifications">
              <AppIcon name="bell" :size="18" class="menu-item-icon" />
              <span class="menu-item-label">消息中心</span>
              <el-badge v-if="unreadCount" :value="unreadCount" :max="99" class="menu-badge" />
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
      <button v-show="!isMobile" type="button" class="collapse-btn" :title="collapsed ? '展开菜单' : '收起菜单'" @click="collapsed = !collapsed">
        <AppIcon :name="collapsed ? 'panel-right-open' : 'panel-left-close'" :size="18" />
      </button>
    </el-aside>

    <el-container class="main-wrap">
      <el-header class="header">
        <div class="header-left">
          <el-button v-if="isMobile" circle class="menu-toggle" @click="mobileMenuOpen = true">
            <AppIcon name="menu" :size="18" />
          </el-button>
          <nav class="breadcrumb" aria-label="页面路径">
            <span class="breadcrumb-root">工作台</span>
            <span class="breadcrumb-sep">/</span>
            <span class="breadcrumb-current">{{ pageShortTitle }}</span>
          </nav>
          <el-tag
            v-if="urgentDeadlineCount"
            type="danger"
            size="small"
            effect="plain"
            round
            class="header-urgent-tag"
          >
            {{ urgentDeadlineCount }} 项临近/逾期
          </el-tag>
          <el-tag
            v-else-if="todoCount && userStore.isExpert"
            type="warning"
            size="small"
            effect="plain"
            round
            class="header-urgent-tag"
          >
            {{ todoCount }} 项待办
          </el-tag>
        </div>
        <div class="header-right">
          <el-badge :value="unreadCount" :hidden="!unreadCount" class="notify-badge">
            <el-popover width="380" trigger="click" popper-class="notify-popover" @show="loadNotifications">
              <template #reference>
                <el-button circle class="header-icon-btn">
                  <AppIcon name="bell" :size="18" />
                </el-button>
              </template>
              <div class="notify-header">
                <span class="notify-header-title">消息通知</span>
                <span class="notify-header-actions">
                  <el-button link size="small" type="primary" @click="$router.push('/notifications')">全部</el-button>
                  <el-button link size="small" @click="readAll">已读</el-button>
                </span>
              </div>
              <div v-if="notifications.length" class="notify-pop-list">
                <div
                  v-for="n in notifications"
                  :key="n.id"
                  class="notify-card"
                  :class="{ unread: !n.readFlag }"
                  @click="readOne(n)"
                >
                  <div class="notify-card-head">
                    <el-tag v-if="n.type" size="small" type="info" effect="plain" round>{{ notificationTypeLabel(n.type) }}</el-tag>
                    <strong>{{ n.title }}</strong>
                  </div>
                  <p>{{ n.content }}</p>
                  <time>{{ n.createdAt }}</time>
                </div>
              </div>
              <el-empty v-else description="暂无消息" :image-size="56" />
            </el-popover>
          </el-badge>

          <el-dropdown trigger="click" @command="onUserCommand">
            <div class="user-trigger">
              <el-avatar :size="34" class="user-avatar">{{ userInitial }}</el-avatar>
              <div v-show="!isMobile" class="user-meta">
                <span class="user-name">{{ userStore.user?.realName || userStore.user?.username }}</span>
                <span v-if="roleLabel" class="user-role">{{ roleLabel }}</span>
              </div>
              <AppIcon name="chevron-down" :size="16" class="user-chevron" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">
                  <AppIcon name="settings" :size="16" />系统设置
                </el-dropdown-item>
                <el-dropdown-item command="notifications">
                  <AppIcon name="bell" :size="16" />消息中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout" class="logout-item">
                  <AppIcon name="log-out" :size="16" />退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <el-drawer
      v-model="mobileMenuOpen"
      direction="ltr"
      size="260px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="drawer-logo">
        <div class="logo-mark">
          <AppIcon name="layout-dashboard" :size="20" />
        </div>
        <span class="logo-title">专家评审系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="side-menu drawer-menu"
        background-color="transparent"
        text-color="rgba(255,255,255,0.72)"
        active-text-color="#ffffff"
        @select="mobileMenuOpen = false"
      >
        <template v-if="userStore.isAdmin">
          <el-menu-item index="/dashboard">进度看板</el-menu-item>
          <el-menu-item index="/experts">专家库</el-menu-item>
          <el-menu-item index="/tasks">评审任务</el-menu-item>
          <el-menu-item v-if="userStore.isSystemAdmin" index="/users">用户管理</el-menu-item>
          <el-menu-item index="/analytics">统计分析</el-menu-item>
          <el-menu-item index="/notifications">消息中心</el-menu-item>
          <el-menu-item index="/audit">审计日志</el-menu-item>
          <el-menu-item index="/settings">系统设置</el-menu-item>
        </template>
        <template v-if="userStore.isLeaderOnly">
          <el-menu-item index="/leader/tasks">评审汇总</el-menu-item>
          <el-menu-item index="/analytics">统计分析</el-menu-item>
        </template>
        <template v-if="userStore.isExpert">
          <el-menu-item index="/expert/workbench">工作台</el-menu-item>
          <el-menu-item index="/my-reviews">我的评审</el-menu-item>
        </template>
        <template v-if="userStore.isApplicant">
          <el-menu-item index="/my-projects">我的申报</el-menu-item>
        </template>
        <template v-if="!userStore.isAdmin">
          <el-menu-item index="/settings">系统设置</el-menu-item>
          <el-menu-item index="/notifications">消息中心</el-menu-item>
        </template>
      </el-menu>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { notificationTypeLabel } from '../utils/labels'
import { deadlineUrgency } from '../utils/deadline'
import { getNotifications, getUnreadCount, markNotificationRead, markAllNotificationsRead, getMyReviews } from '../api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)
const todoCount = ref(0)
const urgentDeadlineCount = ref(0)
const notifications = ref([])
const collapsed = ref(false)
const mobileMenuOpen = ref(false)
const isMobile = ref(false)

const MOBILE_BP = 960

function updateViewport() {
  isMobile.value = window.innerWidth < MOBILE_BP
  if (isMobile.value) collapsed.value = true
}

const asideWidth = computed(() => {
  if (isMobile.value) return '0px'
  return collapsed.value ? '72px' : '216px'
})

/** 子路由映射到侧栏菜单项，保持任务详情/汇总等页面对应菜单高亮 */
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/tasks') || path.startsWith('/summary/')) return '/tasks'
  if (path.startsWith('/leader/summary/') || path.startsWith('/leader/tasks')) return '/leader/tasks'
  if (path.startsWith('/my-reviews')) return '/my-reviews'
  if (path.startsWith('/expert/workbench')) return '/expert/workbench'
  if (path.startsWith('/my-projects')) return '/my-projects'
  return path
})

const pageTitleMap = {
  '/dashboard': '进度看板',
  '/experts': '专家库',
  '/tasks': '评审任务',
  '/analytics': '统计分析',
  '/audit': '审计日志',
  '/users': '用户管理',
  '/settings': '系统设置',
  '/notifications': '消息中心',
  '/my-reviews': '我的评审',
  '/expert/workbench': '专家工作台',
  '/my-projects': '我的申报',
  '/leader/tasks': '评审汇总'
}

const pageShortTitle = computed(() => {
  if (route.path.startsWith('/tasks/')) return '任务详情'
  if (route.path.startsWith('/summary/')) return '评审汇总'
  if (route.path.startsWith('/my-reviews/')) return '评审详情'
  return pageTitleMap[route.path] || '首页'
})

const userInitial = computed(() => {
  const name = userStore.user?.realName || userStore.user?.username || '?'
  return name.charAt(0)
})

const roleLabel = computed(() => {
  const roles = userStore.user?.roles || []
  if (roles.includes('ADMIN')) return '管理员'
  if (roles.includes('REVIEW_ADMIN')) return '评审管理员'
  if (roles.includes('LEADER')) return '领导'
  if (roles.includes('EXPERT')) return '专家'
  if (roles.includes('APPLICANT')) return '申报人'
  return ''
})

function onUserCommand(cmd) {
  if (cmd === 'logout') logout()
  else if (cmd === 'settings') router.push('/settings')
  else if (cmd === 'notifications') router.push('/notifications')
}

async function refreshExpertTodo() {
  if (!userStore.isExpert) {
    todoCount.value = 0
    urgentDeadlineCount.value = 0
    return
  }
  try {
    const res = await getMyReviews({})
    const items = res.data || []
    todoCount.value = items.filter(r =>
      r.assignmentStatus === 'PENDING' || r.assignmentStatus === 'ACCEPTED'
    ).length
    urgentDeadlineCount.value = items.filter(r => {
      if (r.assignmentStatus === 'SUBMITTED' || r.assignmentStatus === 'REJECTED') return false
      const u = deadlineUrgency(r.deadline)
      return u === 'soon' || u === 'overdue'
    }).length
  } catch { /* ignore */ }
}

async function refreshUnread() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data.count
  } catch { /* ignore */ }
}

async function loadNotifications() {
  const res = await getNotifications({ page: 1, size: 10 })
  notifications.value = res.data.records
}

async function readOne(n) {
  if (!n.readFlag) {
    await markNotificationRead(n.id)
    n.readFlag = 1
    refreshUnread()
  }
}

async function readAll() {
  await markAllNotificationsRead()
  refreshUnread()
  loadNotifications()
}

function logout() {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
  refreshUnread()
  refreshExpertTodo()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateViewport)
})

watch(() => route.path, () => {
  refreshUnread()
  refreshExpertTodo()
})
</script>

<style scoped>
.layout {
  height: 100vh;
  overflow: hidden;
}

.aside {
  background: var(--app-sidebar-bg);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  height: 100vh;
  transition: width 0.25s ease;
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.04);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px 16px;
  min-height: 72px;
}

.logo-mark {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--app-primary) 0%, var(--app-accent) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 8px 20px rgba(92, 108, 255, 0.35);
}

.logo-title {
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.menu-scroll {
  flex: 1;
}

.side-menu {
  border-right: none;
  padding: 4px 10px 12px;
}

.side-menu :deep(.el-menu-item) {
  border-radius: var(--app-radius-sm);
  margin-bottom: 4px;
  height: 42px;
  line-height: 42px;
  display: flex;
  align-items: center;
  overflow: hidden;
  transition: background 0.2s ease;
}

.side-menu :deep(.el-menu-item:hover) {
  background: var(--app-sidebar-hover) !important;
}

.side-menu :deep(.el-menu-item.is-active) {
  background: var(--app-sidebar-active) !important;
  font-weight: 600;
}

.menu-item-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 收起后只显示图标，彻底隐藏文字和角标 */
.side-menu.el-menu--collapse :deep(.menu-item-label),
.side-menu.el-menu--collapse :deep(.menu-badge) {
  display: none !important;
  width: 0;
  overflow: hidden;
}

.side-menu.el-menu--collapse {
  padding: 4px 6px 12px;
}

.side-menu.el-menu--collapse :deep(.el-menu-item) {
  overflow: hidden;
  padding: 0 !important;
  justify-content: center;
}

.side-menu.el-menu--collapse :deep(.menu-item-icon) {
  margin-right: 0 !important;
}

.aside--collapsed .logo {
  justify-content: center;
  padding-left: 0;
  padding-right: 0;
}

.aside--collapsed .collapse-btn {
  margin-left: auto;
  margin-right: auto;
  width: 36px;
}

.collapse-btn {
  margin: 8px 12px 16px;
  height: 36px;
  border: none;
  border-radius: var(--app-radius-sm);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.55);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease, color 0.2s ease;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.main-wrap {
  flex: 1;
  min-width: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--app-bg-mesh);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  height: 64px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--app-border-soft);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  min-width: 0;
}

.breadcrumb-root {
  color: var(--app-text-muted);
  flex-shrink: 0;
}

.breadcrumb-sep {
  color: var(--app-border);
  flex-shrink: 0;
}

.breadcrumb-current {
  font-weight: 600;
  color: var(--app-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.menu-toggle {
  border: 1px solid var(--app-border);
  background: var(--app-surface);
}

.header-icon-btn {
  border: none;
  background: var(--app-surface);
  box-shadow: var(--app-shadow-inset);
}

.header-icon-btn:hover {
  background: var(--app-primary-muted);
  color: var(--app-primary);
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 12px 4px 4px;
  border-radius: var(--app-radius-full);
  background: var(--app-surface);
  box-shadow: var(--app-shadow-inset);
  cursor: pointer;
  transition: box-shadow 0.2s ease;
}

.user-trigger:hover {
  box-shadow: var(--app-shadow);
}

.user-avatar {
  background: linear-gradient(135deg, var(--app-primary), #818cf8);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.25;
  max-width: 120px;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.user-role {
  font-size: 11px;
  color: var(--app-text-muted);
}

.user-chevron {
  color: var(--app-text-muted);
  flex-shrink: 0;
}

.main-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 24px 28px 36px;
  width: 100%;
  max-width: none;
  box-sizing: border-box;
}

.main-content > * {
  width: 100%;
  max-width: 100%;
}

.notify-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.notify-header-title {
  font-weight: 600;
  font-size: 15px;
}

.notify-header-actions {
  display: flex;
  gap: 4px;
}

.notify-pop-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
}

.menu-badge {
  margin-left: auto;
  vertical-align: middle;
}

:deep(.menu-badge .el-badge__content) {
  border: none;
  background: var(--app-accent);
}

:deep(.logout-item) {
  color: var(--app-danger);
}

.drawer-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px 16px;
  background: var(--app-sidebar-bg);
}

:deep(.mobile-drawer .el-drawer__body) {
  padding: 0;
  background: var(--app-sidebar-bg);
}

.drawer-menu {
  padding: 8px 12px 24px;
}

@media (max-width: 960px) {
  .aside {
    display: none;
  }

  .header {
    padding: 0 16px;
  }

  .main-content {
    padding: 20px 16px 32px;
  }
}
</style>

<style>
.notify-popover.el-popover {
  border-radius: var(--app-radius) !important;
  border: none !important;
  box-shadow: var(--app-shadow-hover) !important;
  padding: 16px 18px !important;
}
</style>
