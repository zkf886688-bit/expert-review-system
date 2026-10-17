<template>
  <div class="airy-page my-reviews-page">
    <PageHeader title="我的评审" subtitle="接受邀请并完成在线评审" />

    <div class="mini-stat-grid workbench-stats">
      <div
        v-for="item in statCards"
        :key="item.key"
        class="mini-stat"
        :class="{ 'mini-stat--active': activeTab === item.key }"
        :style="{ '--mini-stat-accent': item.color }"
        @click="switchTab(item.key)"
      >
        <div class="mini-stat-value">{{ item.value }}</div>
        <div class="mini-stat-label">{{ item.label }}</div>
      </div>
    </div>

    <el-card class="content-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="load">
        <el-tab-pane name="ALL">
          <template #label>全部 <span v-if="stats.all" class="tab-badge">{{ stats.all }}</span></template>
        </el-tab-pane>
        <el-tab-pane name="PENDING_ACCEPT">
          <template #label>待接受 <span v-if="stats.pendingAccept" class="tab-badge tab-badge--amber">{{ stats.pendingAccept }}</span></template>
        </el-tab-pane>
        <el-tab-pane name="PENDING_REVIEW">
          <template #label>待评审 <span v-if="stats.pendingReview" class="tab-badge tab-badge--green">{{ stats.pendingReview }}</span></template>
        </el-tab-pane>
        <el-tab-pane name="DEADLINE_SOON">
          <template #label>即将截止 <span v-if="stats.deadlineSoon" class="tab-badge tab-badge--red">{{ stats.deadlineSoon }}</span></template>
        </el-tab-pane>
      </el-tabs>

      <el-table
        v-if="list.length || loading"
        :data="list"
        v-loading="loading"
        stripe
        :row-class-name="rowClassName"
      >
        <el-table-column prop="taskName" label="任务" min-width="180" />
        <el-table-column prop="projectName" label="项目" min-width="160" />
        <el-table-column prop="reviewType" label="类型" width="100" />
        <el-table-column label="截止时间" width="200">
          <template #default="{ row }">
            <DeadlineCell :value="row.deadline" />
          </template>
        </el-table-column>
        <el-table-column prop="assignmentStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="assignmentStatusTag(row.assignmentStatus)" size="small">
              {{ assignmentStatusLabel(row.assignmentStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <template v-if="row.assignmentStatus === 'PENDING'">
              <el-button link type="success" @click="respond(row, true)">接受</el-button>
              <el-button link type="danger" @click="openReject(row)">拒绝</el-button>
            </template>
            <el-button v-else link type="primary" @click="$router.push(`/my-reviews/${row.assignmentId}`)">
              {{ row.assignmentStatus === 'SUBMITTED' ? '查看' : '评审' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <EmptyGuide
        v-else
        :description="emptyDescription"
        action-text="查看消息"
        action-to="/notifications"
      />
    </el-card>

    <el-dialog v-model="rejectVisible" title="拒绝/回避" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请说明拒绝或回避原因" />
      <template #footer>
        <el-button @click="rejectVisible=false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { assignmentStatusLabel, assignmentStatusTag } from '../utils/labels'
import { deadlineRowClassName, deadlineUrgency } from '../utils/deadline'
import { getMyReviews, respondInvite } from '../api'
import PageHeader from '../components/PageHeader.vue'
import DeadlineCell from '../components/DeadlineCell.vue'
import EmptyGuide from '../components/EmptyGuide.vue'

const route = useRoute()
const loading = ref(false)
const list = ref([])
const allList = ref([])
const activeTab = ref('ALL')
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref(null)

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
  { key: 'PENDING_ACCEPT', label: '待接受邀请', value: stats.value.pendingAccept, color: '#d97706' },
  { key: 'PENDING_REVIEW', label: '待完成评审', value: stats.value.pendingReview, color: '#059669' },
  { key: 'DEADLINE_SOON', label: '临近/逾期', value: stats.value.deadlineSoon, color: '#dc2626' }
])

const emptyDescription = computed(() => {
  const map = {
    ALL: '暂无评审任务，请等待管理员分配或接受邀请',
    PENDING_ACCEPT: '暂无待接受的邀请',
    PENDING_REVIEW: '暂无待完成的评审',
    DEADLINE_SOON: '暂无临近截止的评审任务'
  }
  return map[activeTab.value] || map.ALL
})

function rowClassName({ row }) {
  if (row.assignmentStatus === 'SUBMITTED' || row.assignmentStatus === 'REJECTED') return ''
  return deadlineRowClassName(row.deadline)
}

function switchTab(key) {
  activeTab.value = key
  load()
}

async function loadStats() {
  try {
    const res = await getMyReviews({})
    allList.value = res.data || []
  } catch { /* ignore */ }
}

async function load() {
  loading.value = true
  try {
    const params = activeTab.value === 'ALL' ? {} : { workbench: activeTab.value }
    const res = await getMyReviews(params)
    list.value = res.data
  } finally {
    loading.value = false
  }
}

async function respond(row, accept) {
  await respondInvite({ assignmentId: row.assignmentId, accept })
  ElMessage.success(accept ? '已接受邀请' : '已拒绝')
  await loadStats()
  load()
}

function openReject(row) {
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value) return ElMessage.warning('请填写原因')
  await respondInvite({ assignmentId: currentRow.value.assignmentId, accept: false, reason: rejectReason.value })
  ElMessage.success('已拒绝')
  rejectVisible.value = false
  await loadStats()
  load()
}

onMounted(async () => {
  if (route.query.tab) activeTab.value = String(route.query.tab)
  await loadStats()
  load()
})

watch(() => route.query.tab, (tab) => {
  if (tab) {
    activeTab.value = String(tab)
    load()
  }
})
</script>

<style scoped>
.workbench-stats {
  margin-bottom: 20px;
}

.workbench-stats .mini-stat {
  cursor: pointer;
}

.mini-stat--active {
  box-shadow: var(--app-shadow-hover);
  transform: translateY(-2px);
  outline: 2px solid color-mix(in srgb, var(--mini-stat-accent) 35%, transparent);
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  margin-left: 6px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  background: #dbeafe;
  color: #5c6cff;
}

.tab-badge--amber { background: #fef3c7; color: #d97706; }
.tab-badge--green { background: #d1fae5; color: #059669; }
.tab-badge--red { background: #fee2e2; color: #dc2626; }
</style>
