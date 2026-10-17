<template>
  <div class="airy-page expert-workbench">
    <PageHeader
      :title="`你好，${userName}`"
      subtitle="专家工作台 · 待办与截止提醒一览"
    />

    <PageSkeleton v-if="loading" :stat-count="4" :table-height="240" />

    <template v-else>
      <div class="mini-stat-grid">
        <div
          v-for="item in statCards"
          :key="item.key"
          class="mini-stat stat-clickable"
          :style="{ '--mini-stat-accent': item.color }"
          @click="goReviews(item.key)"
        >
          <div class="mini-stat-value">{{ item.value }}</div>
          <div class="mini-stat-label">{{ item.label }}</div>
        </div>
      </div>

      <el-row :gutter="20">
        <el-col :xs="24" :md="14">
          <el-card class="content-card" shadow="never">
            <h3 class="airy-section-title">待办评审</h3>
            <el-table v-if="todoList.length" :data="todoList" stripe size="small" :row-class-name="rowClassName">
              <el-table-column prop="projectName" label="项目" min-width="140" />
              <el-table-column prop="taskName" label="任务" min-width="120" />
              <el-table-column label="截止" width="180">
                <template #default="{ row }"><DeadlineCell :value="row.deadline" /></template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="assignmentStatusTag(row.assignmentStatus)">
                    {{ assignmentStatusLabel(row.assignmentStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" @click="goReview(row)">处理</el-button>
                </template>
              </el-table-column>
            </el-table>
            <EmptyGuide
              v-else
              description="暂无待办评审，邀请到达后将显示在此"
              action-text="查看全部"
              action-to="/my-reviews"
              icon="clipboard-pen"
            />
          </el-card>
        </el-col>

        <el-col :xs="24" :md="10">
          <el-card class="content-card" shadow="never">
            <div class="card-head-row">
              <h3 class="airy-section-title">最新消息</h3>
              <el-button link type="primary" @click="$router.push('/notifications')">全部</el-button>
            </div>
            <div v-if="notifications.length" class="notify-feed">
              <div
                v-for="n in notifications"
                :key="n.id"
                class="notify-feed-item"
                :class="{ unread: !n.readFlag }"
                @click="readNotify(n)"
              >
                <el-tag size="small" type="info">{{ notificationTypeLabel(n.type) }}</el-tag>
                <strong>{{ n.title }}</strong>
                <p>{{ n.content }}</p>
                <small>{{ n.createdAt }}</small>
              </div>
            </div>
            <EmptyGuide v-else description="暂无新消息" action-to="/notifications" icon="bell" />
          </el-card>

          <el-card class="content-card quick-actions" shadow="never">
            <h3 class="airy-section-title">快捷入口</h3>
            <div class="action-grid">
              <el-button @click="$router.push('/my-reviews')">
                <AppIcon name="clipboard-pen" :size="16" /> 我的评审
              </el-button>
              <el-button @click="$router.push('/notifications')">
                <AppIcon name="bell" :size="16" /> 消息中心
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { assignmentStatusLabel, assignmentStatusTag, notificationTypeLabel } from '../utils/labels'
import { deadlineRowClassName, deadlineUrgency } from '../utils/deadline'
import { getMyReviews, getNotifications, markNotificationRead } from '../api'
import PageHeader from '../components/PageHeader.vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import DeadlineCell from '../components/DeadlineCell.vue'
import EmptyGuide from '../components/EmptyGuide.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(true)
const allList = ref([])
const notifications = ref([])

const userName = computed(() => userStore.user?.realName || userStore.user?.username || '专家')

const stats = computed(() => {
  const items = allList.value
  const active = items.filter(r => r.assignmentStatus !== 'SUBMITTED' && r.assignmentStatus !== 'REJECTED')
  return {
    all: items.length,
    pendingAccept: items.filter(r => r.assignmentStatus === 'PENDING').length,
    pendingReview: items.filter(r => ['ACCEPTED', 'OVERDUE'].includes(r.assignmentStatus)).length,
    deadlineSoon: active.filter(r => {
      const u = deadlineUrgency(r.deadline)
      return u === 'soon' || u === 'overdue'
    }).length
  }
})

const statCards = computed(() => [
  { key: 'ALL', label: '全部任务', value: stats.value.all, color: '#5c6cff' },
  { key: 'PENDING_ACCEPT', label: '待接受', value: stats.value.pendingAccept, color: '#d97706' },
  { key: 'PENDING_REVIEW', label: '待评审', value: stats.value.pendingReview, color: '#059669' },
  { key: 'DEADLINE_SOON', label: '临近截止', value: stats.value.deadlineSoon, color: '#dc2626' }
])

const todoList = computed(() =>
  allList.value
    .filter(r => r.assignmentStatus === 'PENDING' || r.assignmentStatus === 'ACCEPTED' || r.assignmentStatus === 'OVERDUE')
    .slice(0, 8)
)

function rowClassName({ row }) {
  if (row.assignmentStatus === 'SUBMITTED' || row.assignmentStatus === 'REJECTED') return ''
  return deadlineRowClassName(row.deadline)
}

function goReviews(tab) {
  router.push({ path: '/my-reviews', query: tab === 'ALL' ? {} : { tab } })
}

function goReview(row) {
  if (row.assignmentStatus === 'PENDING') {
    router.push('/my-reviews')
  } else {
    router.push(`/my-reviews/${row.assignmentId}`)
  }
}

async function readNotify(n) {
  if (!n.readFlag) {
    await markNotificationRead(n.id)
    n.readFlag = 1
  }
  router.push('/notifications')
}

onMounted(async () => {
  loading.value = true
  try {
    const [reviews, notes] = await Promise.all([
      getMyReviews({}),
      getNotifications({ page: 1, size: 5 })
    ])
    allList.value = reviews.data || []
    notifications.value = notes.data?.records || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-clickable {
  cursor: pointer;
}

.card-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.card-head-row .airy-section-title {
  margin-bottom: 0;
}

.notify-feed {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notify-feed-item {
  padding: 14px 16px;
  border-radius: var(--app-radius-sm);
  background: #f8fafc;
  cursor: pointer;
  transition: background 0.15s ease, box-shadow 0.15s ease;
}

.notify-feed-item:hover {
  background: #eff6ff;
}

.notify-feed-item.unread {
  box-shadow: inset 3px 0 0 var(--app-primary);
  background: #eff6ff;
}

.notify-feed-item strong {
  display: block;
  margin: 6px 0 4px;
  font-size: 14px;
}

.notify-feed-item p {
  margin: 0;
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.5;
}

.notify-feed-item small {
  display: block;
  margin-top: 6px;
  color: #9ca3af;
  font-size: 12px;
}

.quick-actions {
  margin-top: 20px;
}

.action-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.action-grid .el-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
