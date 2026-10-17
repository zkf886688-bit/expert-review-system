<template>
  <div class="airy-page">
    <PageHeader title="评审任务" subtitle="创建、分配与管理外审任务">
      <template #extra>
        <el-input v-model="keyword" placeholder="搜索任务/批次" clearable style="width:180px" @keyup.enter="load" @clear="load" />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width:130px" @change="load">
          <el-option v-for="(label, key) in taskStatusMap" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button @click="load">查询</el-button>
        <el-button @click="exportTasks">导出</el-button>
        <el-button type="primary" @click="dialogVisible = true">创建任务</el-button>
      </template>
    </PageHeader>

    <el-card class="content-card" shadow="never">
      <div class="table-wrap">
      <el-table v-if="list.length || loading" :data="list" v-loading="loading" stripe :row-class-name="taskRowClass" style="width: 100%">
      <el-table-column label="任务名称" min-width="200">
        <template #default="{ row }">
          <router-link :to="`/tasks/${row.id}`" class="task-name-link">{{ row.name }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="reviewType" label="类型" min-width="100" />
      <el-table-column prop="batchName" label="批次" min-width="110" />
      <el-table-column prop="projectCount" label="项目数" min-width="88" align="center" />
      <el-table-column label="评审完成" min-width="120">
        <template #default="{ row }">
          <span v-if="row.projectCount">{{ row.completedProjectCount || 0 }} / {{ row.projectCount }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="截止时间" min-width="200">
        <template #default="{ row }">
          <DeadlineCell :value="row.deadline" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" min-width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="taskStatusTag(row.status)">{{ taskStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="canViewSummary(row)"
            link
            type="success"
            @click.stop="goSummary(row.id)"
          >
            汇总
          </el-button>
          <el-tooltip content="复制评审配置与项目列表，不含专家分配与材料" placement="top">
            <el-button link type="warning" @click.stop="openClone(row)">复制任务</el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    </div>
    <EmptyGuide
      v-if="!loading && !list.length"
      description="还没有评审任务，点击右上角创建第一个任务"
      action-text="创建任务"
      icon="file-text"
      @action="dialogVisible = true"
    />
    </el-card>

    <el-dialog v-model="dialogVisible" class="airy-dialog" title="创建评审任务" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="任务名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="评审类型" required>
          <el-select v-model="form.reviewType" style="width:100%" @change="onReviewTypeChange">
            <el-option v-for="t in reviewTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-alert v-if="typeConfigHint" :title="typeConfigHint" type="info" show-icon :closable="false" class="type-hint" />
        <el-form-item label="批次名称"><el-input v-model="form.batchName" /></el-form-item>
        <el-form-item v-if="userStore.isSystemAdmin" label="数据域">
          <el-input v-model="form.dataDomain" placeholder="DEFAULT" />
        </el-form-item>
        <el-form-item label="截止时间" required>
          <el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="最低评审人数"><el-input-number v-model="form.minReviewers" :min="1" /></el-form-item>
        <el-form-item label="盲审模式"><el-switch v-model="form.blindReview" /></el-form-item>
        <el-form-item label="同单位回避"><el-switch v-model="form.avoidSameOrg" /></el-form-item>
        <el-form-item label="专家不重复"><el-switch v-model="form.avoidDuplicateExpert" /></el-form-item>
        <el-divider>待评项目</el-divider>
        <div v-for="(p, i) in form.projects" :key="i" class="project-row">
          <el-input v-model="p.projectName" placeholder="项目名称" />
          <el-input v-model="p.applicant" placeholder="申请人" />
          <el-input v-model="p.organization" placeholder="申报单位" />
          <el-button link type="danger" @click="form.projects.splice(i,1)">删</el-button>
        </div>
        <el-button @click="form.projects.push({ projectName:'', applicant:'', organization:'' })">添加项目</el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cloneVisible" class="airy-dialog" title="复制任务" width="480px">
      <el-form label-width="90px">
        <el-form-item label="新任务名称"><el-input v-model="cloneForm.name" placeholder="留空则自动加 (副本)" /></el-form-item>
        <el-form-item label="新截止时间" required>
          <el-date-picker v-model="cloneForm.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <p class="hint">将复制评审配置与项目列表，不含专家分配与材料</p>
      </el-form>
      <template #footer>
        <el-button @click="cloneVisible=false">取消</el-button>
        <el-button type="primary" @click="doClone">确认复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { taskStatusMap, taskStatusLabel, taskStatusTag } from '../utils/labels'
import { getTasks, createTask, cloneTask, downloadFile, getReviewTypes, getReviewTypeConfig } from '../api'
import PageHeader from '../components/PageHeader.vue'
import DeadlineCell from '../components/DeadlineCell.vue'
import EmptyGuide from '../components/EmptyGuide.vue'
import { deadlineRowClassName } from '../utils/deadline'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const list = ref([])
const dialogVisible = ref(false)

function taskRowClass({ row }) {
  return deadlineRowClassName(row.deadline) || ''
}

function goSummary(id) {
  router.push(`/summary/${id}`)
}

function canViewSummary(row) {
  if (row.status === 'SUMMARIZED' || row.status === 'ARCHIVED') return true
  if (!row.projectCount) return false
  return (row.completedProjectCount || 0) >= row.projectCount
}

const cloneVisible = ref(false)
const cloneSource = ref(null)
const cloneForm = reactive({ name: '', deadline: '' })
const reviewTypes = ref(['揭榜挂帅', '项目立项', '中期检查', '结题验收', '成果评价', '奖项评审', '人才评审'])
const typeConfigHint = ref('')
const form = reactive({
  name: '', reviewType: '项目立项', batchName: '', dataDomain: 'DEFAULT', deadline: '', minReviewers: 3,
  blindReview: false, avoidSameOrg: true, avoidDuplicateExpert: true,
  projects: [{ projectName: '', applicant: '', organization: '' }]
})

async function loadReviewTypes() {
  try {
    const res = await getReviewTypes()
    if (res.data?.length) reviewTypes.value = res.data
  } catch { /* use defaults */ }
}

async function onReviewTypeChange(type) {
  typeConfigHint.value = ''
  if (!type) return
  try {
    const res = await getReviewTypeConfig(type)
    const items = res.data?.formConfig || []
    const conclusions = res.data?.conclusions || []
    if (items.length) {
      const dims = items.map(i => `${i.name}(${i.maxScore}分×${i.weight})`).join('、')
      typeConfigHint.value = `默认评分维度：${dims}${conclusions.length ? `；结论选项：${conclusions.join('、')}` : ''}`
    }
  } catch { /* ignore */ }
}

async function load() {
  loading.value = true
  try {
    const params = { page: 1, size: 100 }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getTasks(params)
    list.value = res.data.records
  } finally {
    loading.value = false
  }
}

function exportTasks() {
  const params = new URLSearchParams()
  if (statusFilter.value) params.set('status', statusFilter.value)
  if (keyword.value) params.set('keyword', keyword.value)
  const qs = params.toString()
  downloadFile(`/api/tasks/export${qs ? `?${qs}` : ''}`, 'review_tasks.xlsx')
}

async function create() {
  if (!form.name || !form.deadline) return ElMessage.warning('请填写必填项')
  await createTask(form)
  ElMessage.success('创建成功')
  dialogVisible.value = false
  load()
}

function openClone(row) {
  cloneSource.value = row
  cloneForm.name = ''
  cloneForm.deadline = ''
  cloneVisible.value = true
}

async function doClone() {
  if (!cloneForm.deadline) return ElMessage.warning('请设置新截止时间')
  const res = await cloneTask(cloneSource.value.id, {
    name: cloneForm.name || undefined,
    deadline: cloneForm.deadline
  })
  ElMessage.success('任务已复制')
  cloneVisible.value = false
  router.push(`/tasks/${res.data.id}`)
}

watch(dialogVisible, (v) => {
  if (v) onReviewTypeChange(form.reviewType)
})

onMounted(() => {
  loadReviewTypes()
  load()
})
</script>

<style scoped>
.project-row { display: grid; grid-template-columns: 2fr 1fr 1fr auto; gap: 8px; margin-bottom: 8px; }
.hint { font-size: 12px; color: #909399; margin: 0; }
.type-hint { margin-bottom: 16px; }
.task-name-link {
  display: inline;
  color: var(--el-color-primary);
  font-weight: 500;
  text-decoration: none;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-name-link:hover {
  text-decoration: underline;
}
</style>
