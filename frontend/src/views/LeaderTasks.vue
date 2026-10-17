<template>
  <div class="airy-page">
    <PageHeader title="评审结果查阅" subtitle="领导视图：仅展示汇总结果，专家个人信息已脱敏" />

    <div class="leader-banner">
      <AppIcon name="shield" :size="22" />
      <div>
        <strong>脱敏查阅模式</strong>
        <p>本页面仅展示任务汇总与结论，专家姓名与联系方式已匿名化处理，符合领导查阅权限要求。</p>
      </div>
    </div>

    <div v-if="list.length" class="mini-stat-grid leader-stats">
      <div class="mini-stat" style="--mini-stat-accent: var(--app-primary)">
        <div class="mini-stat-value">{{ list.length }}</div>
        <div class="mini-stat-label">可查阅任务</div>
      </div>
      <div class="mini-stat" style="--mini-stat-accent: #059669">
        <div class="mini-stat-value">{{ inReviewCount }}</div>
        <div class="mini-stat-label">评审进行中</div>
      </div>
      <div class="mini-stat" style="--mini-stat-accent: #d97706">
        <div class="mini-stat-value">{{ urgentCount }}</div>
        <div class="mini-stat-label">临近截止</div>
      </div>
    </div>

    <el-card class="content-card" shadow="never">
      <div v-if="list.length || loading" class="table-wrap">
      <el-table :data="list" v-loading="loading" stripe :row-class-name="taskRowClass">
        <el-table-column prop="name" label="任务名称" min-width="200" />
        <el-table-column prop="reviewType" label="类型" width="120" />
        <el-table-column prop="batchName" label="批次" width="140" />
        <el-table-column label="截止时间" width="200">
          <template #default="{ row }">
            <DeadlineCell :value="row.deadline" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="taskStatusTag(row.status)">{{ taskStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/leader/summary/${row.id}`)">查看汇总</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
      <EmptyGuide
        v-else-if="!loading"
        description="暂无可查阅的评审任务"
        icon="file-text"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getTasks } from '../api'
import { taskStatusLabel, taskStatusTag } from '../utils/labels'
import { deadlineRowClassName, deadlineUrgency } from '../utils/deadline'
import PageHeader from '../components/PageHeader.vue'
import DeadlineCell from '../components/DeadlineCell.vue'
import EmptyGuide from '../components/EmptyGuide.vue'

const loading = ref(false)
const list = ref([])

const inReviewCount = computed(() =>
  list.value.filter(r => r.status === 'IN_REVIEW' || r.status === 'RE_REVIEWING').length
)

const urgentCount = computed(() =>
  list.value.filter(r => {
    const u = deadlineUrgency(r.deadline)
    return u === 'soon' || u === 'overdue'
  }).length
)

function taskRowClass({ row }) {
  return deadlineRowClassName(row.deadline)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getTasks({ page: 1, size: 100 })
    list.value = res.data.records
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.leader-banner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 18px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #f0fdf4 100%);
  border: 1px solid #bfdbfe;
  border-radius: var(--app-radius);
  color: #1e3a5f;
}

.leader-banner p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--app-text-secondary);
  line-height: 1.5;
}

.leader-stats {
  grid-template-columns: repeat(3, 1fr);
  margin-bottom: 20px;
}

@media (max-width: 768px) {
  .leader-stats {
    grid-template-columns: 1fr;
  }
}
</style>
