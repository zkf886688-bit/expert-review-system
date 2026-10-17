<template>
  <div class="airy-page summary-page">
    <PageHeader title="评审结果汇总" subtitle="项目得分与专家意见汇总" :show-back="true">
      <template #extra>
        <div class="export-bar">
          <el-button @click="exportExcel">
            <AppIcon name="file-spreadsheet" :size="16" />
            Excel
          </el-button>
          <el-button @click="exportWord">
            <AppIcon name="file-pen" :size="16" />
            Word
          </el-button>
          <el-button @click="previewPdf">
            <AppIcon name="eye" :size="16" />
            预览 PDF
          </el-button>
          <el-button type="primary" @click="exportPdf">
            <AppIcon name="printer" :size="16" />
            下载 PDF
          </el-button>
        </div>
      </template>
    </PageHeader>

    <PageSkeleton v-if="loading && !list.length" />

    <template v-else>
      <el-alert v-if="masked" title="当前为领导脱敏视图，专家姓名已匿名化" type="info" show-icon :closable="false" class="airy-alert summary-alert" />

      <div v-if="summaryStats" class="mini-stat-grid summary-stats">
        <div class="mini-stat" style="--mini-stat-accent: var(--app-primary)">
          <div class="mini-stat-value">{{ summaryStats.projects }}</div>
          <div class="mini-stat-label">参评项目</div>
        </div>
        <div class="mini-stat" style="--mini-stat-accent: #059669">
          <div class="mini-stat-value">{{ summaryStats.avgScore }}</div>
          <div class="mini-stat-label">平均得分</div>
        </div>
        <div class="mini-stat" style="--mini-stat-accent: #d97706">
          <div class="mini-stat-value">{{ summaryStats.submitted }}</div>
          <div class="mini-stat-label">已提交意见</div>
        </div>
        <div class="mini-stat" style="--mini-stat-accent: #7c3aed">
          <div class="mini-stat-value">{{ summaryStats.complete }}</div>
          <div class="mini-stat-label">已达最低人数</div>
        </div>
      </div>

      <el-card class="content-card" shadow="never">
        <el-tabs v-model="viewTab">
          <el-tab-pane label="汇总视图" name="summary">
            <div v-if="list.length" class="table-wrap">
              <el-table :data="list" stripe>
                <el-table-column prop="rank" label="排名" width="70" />
                <el-table-column prop="projectName" label="项目名称" min-width="180" />
                <el-table-column prop="applicant" label="申请人" width="100" />
                <el-table-column label="提交/要求" width="100">
                  <template #default="{ row }">{{ row.submittedCount }} / {{ row.minReviewers }}</template>
                </el-table-column>
                <el-table-column prop="avgScore" label="平均分" width="90">
                  <template #default="{ row }">
                    <span class="score-pill">{{ row.avgScore ?? '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="maxScore" label="最高分" width="90" />
                <el-table-column prop="minScore" label="最低分" width="90" />
                <el-table-column prop="stdDev" label="标准差" width="90" />
                <el-table-column prop="recommendation" label="结论" width="140">
                  <template #default="{ row }">
                    <el-tag v-if="row.recommendation" size="small" effect="light">{{ row.recommendation }}</el-tag>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="专家意见" type="expand">
                  <template #default="{ row }">
                    <div v-for="(o, i) in row.opinions" :key="i" class="opinion-card">
                      <div class="opinion-head">
                        <strong>{{ o.expertName }}</strong>
                        <el-tag size="small" type="info">{{ o.totalScore }} 分</el-tag>
                        <el-tag size="small" effect="plain">{{ o.conclusion }}</el-tag>
                        <el-button v-if="!masked && o.submissionId" link type="primary" size="small" @click="openRate(o)">质量评分</el-button>
                      </div>
                      <p class="opinion-text">{{ o.opinion }}</p>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <EmptyGuide v-if="!list.length" description="暂无汇总数据，请确认任务已有专家提交评审" icon="file-text" />
          </el-tab-pane>

          <el-tab-pane label="评分项汇总" name="dimensions">
            <PageSkeleton v-if="dimLoading" :stats="false" :table-height="200" />
            <EmptyGuide v-else-if="!dimensions.length" description="暂无评分项数据，需专家提交带分项评分的意见" icon="bar-chart" />
            <div v-else class="table-wrap">
              <el-table :data="dimensions" stripe>
                <el-table-column prop="name" label="评分项" min-width="140" />
                <el-table-column prop="sampleCount" label="样本数" width="90" />
                <el-table-column prop="avgScore" label="平均分" width="100" />
                <el-table-column prop="maxScore" label="最高" width="90" />
                <el-table-column prop="minScore" label="最低" width="90" />
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane label="多轮对比" name="rounds">
            <PageSkeleton v-if="roundLoading" :stats="false" :table-height="180" />
            <EmptyGuide v-else-if="!roundList.length" description="暂无多轮评审数据。启动复审后，历史轮次将在此对比展示" icon="bar-chart" />
            <div v-else>
              <div v-for="project in roundList" :key="project.projectId" class="round-project">
                <div class="round-project-head">
                  <h4>{{ project.projectName }}</h4>
                  <el-tag size="small" type="warning" effect="light">当前第 {{ project.currentRound }} 轮</el-tag>
                </div>
                <div :ref="el => setRoundChartRef(project.projectId, el)" class="round-chart" />
                <div class="table-wrap">
                  <el-table :data="project.rounds" size="small" stripe>
                    <el-table-column prop="round" label="轮次" width="70">
                      <template #default="{ row }">第 {{ row.round }} 轮</template>
                    </el-table-column>
                    <el-table-column prop="submittedCount" label="提交数" width="80" />
                    <el-table-column prop="avgScore" label="平均分" width="90" />
                    <el-table-column prop="maxScore" label="最高" width="80" />
                    <el-table-column prop="minScore" label="最低" width="80" />
                    <el-table-column label="专家意见" type="expand">
                      <template #default="{ row }">
                        <div v-for="(o, i) in row.opinions" :key="i" class="opinion-card">
                          <div class="opinion-head">
                            <strong>{{ o.expertName }}</strong>
                            <el-tag size="small" type="info">{{ o.totalScore }} 分</el-tag>
                            <el-tag size="small" effect="plain">{{ o.conclusion }}</el-tag>
                          </div>
                          <p class="opinion-text">{{ o.opinion }}</p>
                        </div>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </template>

    <el-dialog v-model="rateVisible" class="airy-dialog" title="评审质量评分（1-5分）" width="400px">
      <el-rate v-model="rateForm.score" :max="5" show-score />
      <el-input v-model="rateForm.comment" type="textarea" :rows="2" placeholder="评语（可选）" style="margin-top:12px" />
      <template #footer>
        <el-button @click="rateVisible=false">取消</el-button>
        <el-button type="primary" @click="submitRate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loadEcharts } from '../utils/echartsLoader'
import { getSummary, getRoundComparison, getSummaryDimensions, downloadFile, previewFile, rateSubmissionQuality } from '../api'
import { lineOption, chartColors } from '../utils/chartTheme'
import PageHeader from '../components/PageHeader.vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import EmptyGuide from '../components/EmptyGuide.vue'

const route = useRoute()
const taskId = route.params.taskId
const loading = ref(false)
const roundLoading = ref(false)
const list = ref([])
const roundList = ref([])
const dimensions = ref([])
const dimLoading = ref(false)
const viewTab = ref('summary')
const rateVisible = ref(false)
const rateForm = reactive({ submissionId: null, score: 4, comment: '' })
const roundChartRefs = new Map()
const roundCharts = []

const masked = computed(() => list.value.length > 0 && list.value[0].masked)

const summaryStats = computed(() => {
  if (!list.value.length) return null
  const scores = list.value.map(r => Number(r.avgScore)).filter(n => !Number.isNaN(n))
  return {
    projects: list.value.length,
    avgScore: scores.length ? (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1) : '-',
    submitted: list.value.reduce((s, r) => s + (r.submittedCount || 0), 0),
    complete: list.value.filter(r => (r.submittedCount || 0) >= (r.minReviewers || 0)).length
  }
})

function setRoundChartRef(projectId, el) {
  if (el) roundChartRefs.set(projectId, el)
}

async function renderRoundCharts() {
  roundCharts.forEach(c => c.dispose())
  roundCharts.length = 0
  const echarts = await loadEcharts()
  roundList.value.forEach(project => {
    const el = roundChartRefs.get(project.projectId)
    if (!el || !project.rounds?.length) return
    const chart = echarts.init(el)
    roundCharts.push(chart)
    chart.setOption(lineOption(
      project.rounds.map(r => `第${r.round}轮`),
      project.rounds.map(r => Number(r.avgScore) || 0),
      chartColors.amber
    ))
  })
}

async function load() {
  loading.value = true
  try {
    const res = await getSummary(taskId)
    list.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadRounds() {
  roundLoading.value = true
  try {
    const res = await getRoundComparison(taskId)
    roundList.value = res.data || []
  } finally {
    roundLoading.value = false
  }
  await nextTick()
  await renderRoundCharts()
}

async function loadDimensions() {
  dimLoading.value = true
  try {
    const res = await getSummaryDimensions(taskId)
    dimensions.value = res.data || []
  } finally {
    dimLoading.value = false
  }
}

watch(viewTab, (tab) => {
  if (tab === 'dimensions' && !dimensions.value.length) {
    loadDimensions()
  }
  if (tab === 'rounds' && !roundList.value.length) {
    loadRounds()
  }
})

function openRate(opinion) {
  rateForm.submissionId = opinion.submissionId
  rateForm.score = 4
  rateForm.comment = ''
  rateVisible.value = true
}

async function submitRate() {
  await rateSubmissionQuality({ submissionId: rateForm.submissionId, score: rateForm.score, comment: rateForm.comment })
  ElMessage.success('评分已保存')
  rateVisible.value = false
}

function exportQuery() {
  return masked.value ? '?masked=true' : ''
}

async function exportExcel() {
  try {
    await downloadFile(`/api/summary/tasks/${taskId}/export${exportQuery()}`, `summary_${taskId}.xlsx`)
  } catch (e) {
    ElMessage.error(e.message || 'Excel 导出失败')
  }
}
async function exportWord() {
  try {
    await downloadFile(`/api/summary/tasks/${taskId}/export/word${exportQuery()}`, `summary_${taskId}.docx`)
  } catch (e) {
    ElMessage.error(e.message || 'Word 导出失败')
  }
}
async function exportPdf() {
  try {
    await downloadFile(`/api/summary/tasks/${taskId}/export/pdf${exportQuery()}`, `summary_${taskId}.pdf`)
  } catch (e) {
    ElMessage.error(e.message || 'PDF 下载失败')
  }
}
async function previewPdf() {
  try {
    await previewFile(`/api/summary/tasks/${taskId}/export/pdf${exportQuery()}`)
  } catch (e) {
    ElMessage.error(e.message || 'PDF 预览失败')
  }
}

onMounted(load)
onUnmounted(() => {
  roundCharts.forEach(c => c.dispose())
})
</script>

<style scoped>
.summary-alert {
  margin-bottom: 16px;
}

.summary-stats {
  margin-bottom: 20px;
}

.export-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.export-bar .el-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.score-pill {
  display: inline-block;
  min-width: 36px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eff6ff;
  color: var(--app-primary);
  font-weight: 600;
  text-align: center;
}

.opinion-card {
  padding: 12px 14px;
  margin-bottom: 10px;
  background: #f8fafc;
  border-left: 4px solid var(--app-primary);
  border-radius: 0 8px 8px 0;
}

.opinion-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.opinion-text {
  margin: 0;
  line-height: 1.65;
  color: var(--app-text);
  white-space: pre-wrap;
}

.round-project {
  margin-bottom: 32px;
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.06);
}

.round-project:last-child {
  border-bottom: none;
}

.round-project-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.round-project-head h4 {
  margin: 0;
  font-size: 15px;
}

.round-chart {
  height: 220px;
  margin-bottom: 16px;
}
</style>
