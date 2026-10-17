<template>
  <div class="airy-page analytics-page">
    <PageHeader title="统计分析大屏" subtitle="专家库、任务与评审数据可视化" />

    <PageSkeleton v-if="loading" :stat-count="6" :table-height="280" />

    <template v-else>
      <el-row :gutter="16" class="stat-grid">
        <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="8" :md="4">
          <el-card shadow="hover" class="stat-card" :class="item.theme">
            <div class="stat-icon">
              <AppIcon :name="item.icon" :size="22" />
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div class="analytics-grid">
        <el-card class="content-card chart-panel" shadow="never">
          <h3 class="airy-section-title">单位性质分布</h3>
          <div ref="orgChart" class="chart" />
        </el-card>
        <el-card class="content-card chart-panel" shadow="never">
          <h3 class="airy-section-title">评审类型分布</h3>
          <div ref="typeChart" class="chart" />
        </el-card>
        <el-card class="content-card chart-panel" shadow="never">
          <h3 class="airy-section-title">任务状态分布</h3>
          <div ref="statusChart" class="chart" />
        </el-card>
        <el-card class="content-card chart-panel chart-panel--wide" shadow="never">
          <h3 class="airy-section-title">技术领域专家数量</h3>
          <div ref="fieldChart" class="chart chart-tall" />
        </el-card>
        <el-card class="content-card chart-panel chart-panel--wide" shadow="never">
          <h3 class="airy-section-title">任务创建趋势</h3>
          <div ref="trendChart" class="chart chart-tall" />
        </el-card>
        <el-card class="content-card chart-panel" shadow="never">
          <h3 class="airy-section-title">专家标签分布</h3>
          <div ref="tagChart" class="chart" />
        </el-card>
        <el-card class="content-card chart-panel" shadow="never">
          <h3 class="airy-section-title">专家状态分布</h3>
          <div ref="statusExpertChart" class="chart" />
        </el-card>
        <el-card class="content-card chart-panel chart-panel--wide" shadow="never">
          <h3 class="airy-section-title">领域专家缺口预警（少于3人）</h3>
          <div v-if="shortageFields.length" class="tag-cloud">
            <el-tag v-for="g in shortageFields" :key="g.field" type="danger" effect="light" round>
              {{ g.field }}（{{ g.count }}人）
            </el-tag>
          </div>
          <EmptyGuide v-else description="各重点领域专家数量充足" icon="users" :image-size="72" />
        </el-card>
        <el-card class="content-card chart-panel chart-panel--wide" shadow="never">
          <h3 class="airy-section-title">专家参与排行 Top 10</h3>
          <div class="table-wrap">
            <el-table :data="data.topExperts || []" size="small" stripe max-height="280">
              <el-table-column prop="name" label="姓名" min-width="90" />
              <el-table-column prop="organization" label="单位" min-width="120" show-overflow-tooltip />
              <el-table-column prop="reviewCount" label="评审次数" width="90" align="center" />
              <el-table-column prop="onTimeRate" label="按时率" width="80" align="center" />
              <el-table-column prop="avgQualityScore" label="质量均分" width="90" align="center" />
            </el-table>
          </div>
        </el-card>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { loadEcharts } from '../utils/echartsLoader'
import { getAnalytics } from '../api'
import { pieOption, barOption, lineOption, chartColors } from '../utils/chartTheme'
import { taskStatusLabel, expertStatusLabel, mapStatLabels } from '../utils/labels'
import PageHeader from '../components/PageHeader.vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import EmptyGuide from '../components/EmptyGuide.vue'
import AppIcon from '../components/AppIcon.vue'

const loading = ref(false)
const data = ref({})
const orgChart = ref()
const typeChart = ref()
const statusChart = ref()
const tagChart = ref()
const statusExpertChart = ref()
const fieldChart = ref()
const trendChart = ref()
let charts = []

const statCards = computed(() => [
  { label: '专家总数', value: data.value.expertTotal ?? 0, icon: 'users', theme: 'stat-card--blue' },
  { label: '评审任务', value: data.value.taskTotal ?? 0, icon: 'file-text', theme: 'stat-card--green' },
  { label: '已提交评审', value: data.value.submittedTotal ?? 0, icon: 'circle-check', theme: 'stat-card--amber' },
  { label: '待确认邀请', value: data.value.pendingInviteTotal ?? 0, icon: 'inbox', theme: 'stat-card--red' },
  { label: '拒绝邀请', value: data.value.rejectedTotal ?? 0, icon: 'user', theme: 'stat-card--purple' },
  { label: '逾期未交', value: data.value.overdueTotal ?? 0, icon: 'triangle-alert', theme: 'stat-card--red' }
])

const shortageFields = computed(() => (data.value.fieldGaps || []).filter(g => g.shortage))

async function renderCharts() {
  charts.forEach(c => c.dispose())
  charts = []
  const echarts = await loadEcharts()
  const init = (el, option) => {
    if (!el) return
    const chart = echarts.init(el)
    charts.push(chart)
    chart.setOption(option)
  }
  init(orgChart.value, pieOption(data.value.orgTypeStats))
  init(typeChart.value, pieOption(data.value.taskTypeStats))
  init(statusChart.value, pieOption(mapStatLabels(data.value.taskStatusStats, taskStatusLabel)))
  init(tagChart.value, pieOption(data.value.tagStats))
  init(statusExpertChart.value, pieOption(mapStatLabels(data.value.expertStatusStats, expertStatusLabel)))
  const fields = Object.keys(data.value.fieldStats || {})
  init(fieldChart.value, barOption(fields, fields.map(f => data.value.fieldStats[f]), chartColors.green))
  const trend = data.value.monthlyTrend || []
  init(trendChart.value, lineOption(trend.map(t => t.label), trend.map(t => t.value)))
  charts.forEach(c => c.resize())
}

function resizeCharts() {
  charts.forEach(c => c.resize())
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getAnalytics()
    data.value = res.data
  } finally {
    loading.value = false
  }
  await nextTick()
  await renderCharts()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  charts.forEach(c => c.dispose())
  window.removeEventListener('resize', resizeCharts)
})
</script>

<style scoped>
.analytics-page {
  gap: 16px;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.chart-panel :deep(.el-card__body) {
  padding: 20px 24px 24px;
  min-height: 320px;
  display: flex;
  flex-direction: column;
}

.chart-panel--wide {
  grid-column: span 1;
}

.chart-panel .airy-section-title {
  margin-bottom: 12px;
}

.chart {
  flex: 1;
  min-height: 240px;
}

.chart-tall {
  min-height: 260px;
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-content: flex-start;
  flex: 1;
}

@media (max-width: 1200px) {
  .analytics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .analytics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
