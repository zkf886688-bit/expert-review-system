<template>
  <div class="airy-page">
    <PageHeader title="评审任务整体进展一览" subtitle="跟踪任务状态、项目完成和专家提交进度" />

    <div v-loading="loading" class="stat-grid-fluid">
      <el-card v-for="item in cards" :key="item.label" shadow="hover" class="stat-card" :class="item.theme">
        <div class="stat-icon">
          <AppIcon :name="item.icon" :size="22" />
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </el-card>
    </div>

    <el-card class="content-card" shadow="never">
      <template #header>
        <span>任务进度</span>
      </template>
      <div class="table-wrap">
      <el-table v-if="tasks.length || loading" :data="tasks" stripe :row-class-name="taskRowClass" v-loading="loading" table-layout="auto" style="width: 100%">
        <el-table-column prop="taskName" label="任务名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" min-width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="taskStatusTag(row.status)">{{ taskStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" min-width="200">
          <template #default="{ row }">
            <DeadlineCell :value="row.deadline" />
          </template>
        </el-table-column>
        <el-table-column label="项目完成" min-width="180">
          <template #default="{ row }">
            <div v-if="hasTotal(row.projectCount)" class="progress-cell">
              <span>{{ row.completedProjectCount ?? 0 }} / {{ row.projectCount }}</span>
              <el-progress
                :percentage="progressPercentage(row.completedProjectCount, row.projectCount)"
                :show-text="false"
                :stroke-width="6"
              />
            </div>
            <span v-else class="muted-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="提交进度" min-width="200">
          <template #default="{ row }">
            <div v-if="hasTotal(row.totalAssignments)" class="progress-cell">
              <span>{{ row.submittedCount ?? 0 }} / {{ row.totalAssignments }}</span>
              <el-progress
                :percentage="progressPercentage(row.submittedCount, row.totalAssignments)"
                :show-text="false"
                :stroke-width="6"
                status="success"
              />
            </div>
            <span v-else class="muted-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button link type="success" class="action-link" @click="$router.push(`/summary/${row.taskId}`)">汇总</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
      <EmptyGuide
        v-if="!loading && !tasks.length"
        description="暂无进行中的评审任务"
        action-text="创建任务"
        action-to="/tasks"
        icon="file-text"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { taskStatusLabel, taskStatusTag } from '../utils/labels'
import { getDashboard, getTaskProgress } from '../api'
import PageHeader from '../components/PageHeader.vue'
import DeadlineCell from '../components/DeadlineCell.vue'
import EmptyGuide from '../components/EmptyGuide.vue'
import { deadlineRowClassName } from '../utils/deadline'

const loading = ref(false)
const overview = ref({})
const tasks = ref([])

function taskRowClass({ row }) {
  return deadlineRowClassName(row.deadline)
}

function hasTotal(value) {
  return Number(value) > 0
}

function progressPercentage(current, total) {
  const denominator = Number(total)
  if (!denominator) return 0
  return Math.min(100, Math.round((Number(current || 0) / denominator) * 100))
}

const cards = computed(() => [
  { label: '总任务数', value: overview.value.totalTasks ?? 0, icon: 'file-text', theme: 'stat-card--blue' },
  { label: '已发布', value: overview.value.publishedTasks ?? 0, icon: 'send', theme: 'stat-card--blue' },
  { label: '评审中', value: overview.value.inReviewTasks ?? 0, icon: 'clipboard-list', theme: 'stat-card--amber' },
  { label: '待汇总', value: overview.value.pendingSummaryTasks ?? 0, icon: 'layers', theme: 'stat-card--amber' },
  { label: '已提交', value: overview.value.submitted ?? 0, icon: 'circle-check', theme: 'stat-card--green' },
  { label: '逾期', value: overview.value.overdue ?? 0, icon: 'triangle-alert', theme: 'stat-card--red' }
])

onMounted(async () => {
  loading.value = true
  try {
    const [d, t] = await Promise.all([getDashboard(), getTaskProgress()])
    overview.value = d.data
    tasks.value = t.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.progress-cell {
  display: grid;
  gap: 6px;
  min-width: 0;
  color: var(--app-text);
  font-size: 13px;
}

.muted-text {
  color: #9ca3af;
}

.action-link {
  padding: 2px 4px;
  font-weight: 500;
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  transition: background-color 0.2s ease;
}

.action-link:hover {
  background: #f1f5f9;
}
</style>
