<template>
  <div class="airy-page">
    <PageHeader title="消息中心" :subtitle="unreadCount ? `未读消息 ${unreadCount} 条` : '系统通知与评审提醒'">
      <template #extra>
        <el-button @click="readAll" :disabled="!unreadCount">全部已读</el-button>
      </template>
    </PageHeader>

    <div class="mini-stat-grid notify-stats">
      <div class="mini-stat" style="--mini-stat-accent: #dc2626">
        <div class="mini-stat-value">{{ unreadCount }}</div>
        <div class="mini-stat-label">未读消息</div>
      </div>
      <div class="mini-stat" style="--mini-stat-accent: var(--app-primary)">
        <div class="mini-stat-value">{{ total }}</div>
        <div class="mini-stat-label">消息总数</div>
      </div>
      <div class="mini-stat" style="--mini-stat-accent: #059669">
        <div class="mini-stat-value">{{ readCount }}</div>
        <div class="mini-stat-label">已读消息</div>
      </div>
    </div>

    <el-card class="content-card" shadow="never">
      <el-form :inline="true" :model="query" class="filter-bar">
        <el-form-item label="状态">
          <el-select v-model="query.unreadOnly" clearable placeholder="全部" style="width:120px" @change="onFilterChange">
            <el-option label="全部" :value="null" />
            <el-option label="未读" :value="true" />
            <el-option label="已读" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="query.type" clearable placeholder="全部" style="width:160px" @change="onFilterChange">
            <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button></el-form-item>
      </el-form>

      <PageSkeleton v-if="loading && !list.length" :stats="false" :table-height="200" />

      <div v-else-if="list.length" class="notify-card-list">
        <article
          v-for="row in list"
          :key="row.id"
          class="notify-card"
          :class="{ unread: !row.readFlag }"
          @click="readOne(row)"
        >
          <div class="notify-card-head">
            <el-tag v-if="!row.readFlag" type="danger" size="small" effect="dark">未读</el-tag>
            <el-tag v-else size="small" type="info">已读</el-tag>
            <el-tag size="small" type="info" effect="plain">{{ notificationTypeLabel(row.type) }}</el-tag>
            <strong>{{ row.title }}</strong>
          </div>
          <p>{{ row.content }}</p>
          <time>{{ row.createdAt }}</time>
        </article>
      </div>

      <EmptyGuide
        v-else-if="!loading"
        description="暂无消息，评审动态将在此显示"
        icon="bell"
      />

      <el-pagination v-if="total" class="notify-pagination" background layout="total, prev, pager, next"
        :total="total" v-model:current-page="query.page" :page-size="query.size" @current-change="load" />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { notificationTypeLabel } from '../utils/labels'
import { getNotifications, getUnreadCount, markNotificationRead, markAllNotificationsRead } from '../api'
import PageHeader from '../components/PageHeader.vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import EmptyGuide from '../components/EmptyGuide.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const unreadCount = ref(0)
const query = reactive({ page: 1, size: 15, unreadOnly: null, type: '' })

const readCount = computed(() => Math.max(0, total.value - unreadCount.value))

const typeOptions = [
  { value: 'INVITE', label: '评审邀请' },
  { value: 'DEADLINE', label: '截止提醒' },
  { value: 'OVERDUE', label: '逾期提醒' },
  { value: 'SYSTEM', label: '系统通知' },
  { value: 'APPLICANT_PROGRESS', label: '申报进度' },
  { value: 'REMIND', label: '催办提醒' }
]

function onFilterChange() {
  query.page = 1
  load()
}

async function refreshUnread() {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data.count
  } catch { /* ignore */ }
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.type) params.type = query.type
    if (query.unreadOnly === true) params.readFlag = 0
    else if (query.unreadOnly === false) params.readFlag = 1
    const res = await getNotifications(params)
    list.value = res.data.records
    total.value = res.data.total
    await refreshUnread()
  } finally {
    loading.value = false
  }
}

async function readOne(row) {
  if (!row.readFlag) {
    await markNotificationRead(row.id)
    row.readFlag = 1
    await refreshUnread()
  }
}

async function readAll() {
  await markAllNotificationsRead()
  await load()
}

onMounted(load)
</script>

<style scoped>
.notify-stats {
  grid-template-columns: repeat(3, 1fr);
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .notify-stats {
    grid-template-columns: 1fr;
  }
}

.notify-pagination {
  margin-top: 24px;
  justify-content: flex-end;
}
</style>
