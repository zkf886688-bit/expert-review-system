<template>
  <div class="airy-page experts-page">
    <PageHeader title="专家库管理" subtitle="维护专家信息、材料与信用记录">
      <template #extra>
        <div class="toolbar-actions">
          <el-upload :show-file-list="false" :http-request="handleImport" accept=".xlsx,.xls">
            <el-button>导入 Excel</el-button>
          </el-upload>
          <el-button @click="exportData">导出</el-button>
          <el-button type="primary" @click="openDialog()">新增专家</el-button>
        </div>
      </template>
    </PageHeader>

    <el-card class="content-card experts-card" shadow="never">
      <el-radio-group v-model="listMode" class="list-mode" @change="switchListMode">
        <el-radio-button value="all">全部专家</el-radio-button>
        <el-radio-button value="pending">待审核</el-radio-button>
      </el-radio-group>
      <el-form :inline="true" :model="query" class="filter-bar">
        <el-form-item label="姓名"><el-input v-model="query.name" clearable /></el-form-item>
        <el-form-item label="单位"><el-input v-model="query.organization" clearable /></el-form-item>
        <el-form-item label="领域"><el-input v-model="query.field" clearable /></el-form-item>
        <el-form-item label="标签"><el-input v-model="query.tag" clearable placeholder="精确匹配" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width:120px">
            <el-option v-for="(label, key) in expertStatusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">查询</el-button></el-form-item>
      </el-form>

      <div class="table-wrap">
        <el-table v-if="list.length || loading" :data="list" v-loading="loading" stripe>
          <el-table-column prop="name" label="姓名" width="100" />
          <el-table-column prop="organization" label="单位" min-width="180" />
          <el-table-column prop="title" label="职称" width="100" />
          <el-table-column prop="phone" label="手机" width="130" />
          <el-table-column label="专业领域" min-width="160">
            <template #default="{ row }">{{ (row.fields || []).join('、') }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="expertStatusTag(row.status)">{{ expertStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewCount" label="评审次数" width="90" />
          <el-table-column prop="onTimeRate" label="按时率" width="80" />
          <el-table-column prop="avgQualityScore" label="质量均分" width="90" />
          <el-table-column label="操作" width="380" fixed="right">
            <template #default="{ row }">
              <template v-if="listMode === 'pending'">
                <el-button link type="success" @click="approve(row)">通过</el-button>
                <el-button link type="danger" @click="reject(row)">驳回</el-button>
              </template>
              <template v-else>
                <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
                <el-button link @click="openMaterials(row)">材料</el-button>
                <el-button link @click="openAvoidances(row)">回避</el-button>
                <el-button link type="info" @click="openCredit(row)">信用</el-button>
                <el-button link type="warning" @click="openAccount(row)">建账号</el-button>
                <el-button link type="danger" @click="remove(row)">删除</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <EmptyGuide
          v-else-if="!loading"
          description="暂无专家记录，请新增专家或导入 Excel"
          action-text="新增专家"
          icon="users"
          @action="openDialog()"
        />
      </div>

      <el-pagination
        v-if="total"
        class="experts-pagination"
        background
        layout="total, prev, pager, next"
        :total="total"
        v-model:current-page="query.page"
        :page-size="query.size"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" class="airy-dialog" :title="form.id ? '编辑专家' : '新增专家'" width="640px">
      <el-form :model="form" label-width="90px" class="dialog-form">
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="性别"><el-input v-model="form.gender" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.organization" /></el-form-item>
        <el-form-item label="职称"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="地区"><el-input v-model="form.region" /></el-form-item>
        <el-form-item label="单位性质"><el-input v-model="form.orgType" placeholder="高校/企业/科研院所" /></el-form-item>
        <el-form-item label="专业领域">
          <el-select v-model="fieldsList" multiple filterable allow-create default-first-option placeholder="输入后回车添加" style="width:100%" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="tagsList" multiple filterable allow-create default-first-option placeholder="输入后回车添加" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option v-for="(label, key) in expertStatusMap" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="accountVisible" class="airy-dialog" title="创建专家登录账号" width="440px">
      <el-form label-width="80px" class="dialog-form">
        <el-form-item label="用户名"><el-input v-model="accountForm.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="accountForm.password" type="password" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountVisible=false">取消</el-button>
        <el-button type="primary" @click="saveAccount">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="creditVisible" class="airy-dialog" :title="'信用记录 - ' + (creditExpert?.name || '')" width="600px">
      <el-table :data="creditRecords" size="small" max-height="360">
        <el-table-column prop="recordType" label="类型" width="100" />
        <el-table-column prop="score" label="评分" width="70" />
        <el-table-column prop="comment" label="说明" />
        <el-table-column prop="createdAt" label="时间" width="160" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="materialVisible" class="airy-dialog" :title="'资质材料 - ' + (materialExpert?.name || '')" width="600px">
      <el-form :inline="true" class="material-toolbar">
        <el-form-item label="材料类型">
          <el-select v-model="materialType" style="width:140px">
            <el-option label="简历" value="简历" />
            <el-option label="职称证明" value="职称证明" />
            <el-option label="身份证" value="身份证" />
            <el-option label="保密协议" value="保密协议" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-upload :show-file-list="false" :http-request="uploadMaterial">
            <el-button type="primary" size="small">上传</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <el-table :data="materials" size="small">
        <el-table-column prop="materialType" label="类型" width="100" />
        <el-table-column prop="fileName" label="文件名" min-width="160" />
        <el-table-column prop="createdAt" label="上传时间" width="170" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="downloadMaterial(row)">下载</el-button>
            <el-button link type="danger" @click="removeMaterial(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="avoidanceVisible" class="airy-dialog" :title="'回避关系 - ' + (avoidanceExpert?.name || '')" width="640px">
      <el-form :inline="true" :model="avoidanceForm" class="material-toolbar">
        <el-form-item label="类型">
          <el-select v-model="avoidanceForm.avoidType" style="width:130px">
            <el-option label="单位" value="ORGANIZATION" />
            <el-option label="项目" value="PROJECT" />
            <el-option label="人员" value="EXPERT" />
          </el-select>
        </el-form-item>
        <el-form-item label="回避对象"><el-input v-model="avoidanceForm.avoidValue" placeholder="单位名/项目名/专家姓名" style="width:180px" /></el-form-item>
        <el-form-item label="原因"><el-input v-model="avoidanceForm.reason" placeholder="可选" style="width:140px" /></el-form-item>
        <el-form-item><el-button type="primary" @click="saveAvoidance">添加</el-button></el-form-item>
      </el-form>
      <el-table :data="avoidances" size="small" max-height="320">
        <el-table-column prop="avoidType" label="类型" width="90">
          <template #default="{ row }">{{ avoidanceTypeLabel(row.avoidType) }}</template>
        </el-table-column>
        <el-table-column prop="avoidValue" label="回避对象" min-width="140" />
        <el-table-column prop="reason" label="原因" min-width="120" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeAvoidance(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExperts, createExpert, updateExpert, deleteExpert, createExpertAccount, importExperts, downloadFile, getExpertCredit, getExpertMaterials, uploadExpertMaterial, deleteExpertMaterial, downloadExpertMaterial, getExpertAvoidances, addExpertAvoidance, deleteExpertAvoidance, approveExpert, rejectExpert } from '../api'
import { expertStatusMap, expertStatusLabel, expertStatusTag } from '../utils/labels'
import PageHeader from '../components/PageHeader.vue'
import EmptyGuide from '../components/EmptyGuide.vue'

const loading = ref(false)
const listMode = ref('all')
const list = ref([])
const total = ref(0)
const query = reactive({ name: '', organization: '', field: '', tag: '', status: '', page: 1, size: 10 })
const dialogVisible = ref(false)
const accountVisible = ref(false)
const form = reactive({ id: null, name: '', gender: '', organization: '', title: '', phone: '', email: '', region: '', orgType: '', status: 'ACTIVE' })
const fieldsList = ref([])
const tagsList = ref([])
const accountForm = reactive({ expertId: null, username: '', password: '123456' })
const creditVisible = ref(false)
const creditExpert = ref(null)
const creditRecords = ref([])
const materialVisible = ref(false)
const materialExpert = ref(null)
const materials = ref([])
const materialType = ref('简历')
const avoidanceVisible = ref(false)
const avoidanceExpert = ref(null)
const avoidances = ref([])
const avoidanceForm = reactive({ avoidType: 'ORGANIZATION', avoidValue: '', reason: '' })

const payload = computed(() => ({
  ...form,
  fields: fieldsList.value.filter(Boolean),
  tags: tagsList.value.filter(Boolean)
}))

function switchListMode() {
  query.status = listMode.value === 'pending' ? 'PENDING' : ''
  query.page = 1
  load()
}

async function load() {
  loading.value = true
  try {
    const res = await getExperts(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  await approveExpert(row.id)
  ElMessage.success('审核通过')
  load()
}

async function reject(row) {
  await ElMessageBox.confirm(`确认驳回专家「${row.name}」？`, '驳回', { type: 'warning' })
  await rejectExpert(row.id, {})
  ElMessage.success('已驳回')
  load()
}

function openDialog(row) {
  if (row) {
    Object.assign(form, row)
    fieldsList.value = [...(row.fields || [])]
    tagsList.value = [...(row.tags || [])]
    if (!form.status) form.status = 'ACTIVE'
  } else {
    Object.assign(form, { id: null, name: '', gender: '', organization: '', title: '', phone: '', email: '', region: '', orgType: '', status: 'ACTIVE' })
    fieldsList.value = []
    tagsList.value = []
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.name) return ElMessage.warning('请填写姓名')
  if (form.id) await updateExpert(form.id, payload.value)
  else await createExpert(payload.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

function openAccount(row) {
  accountForm.expertId = row.id
  accountForm.username = row.email || `expert${row.id}`
  accountVisible.value = true
}

async function saveAccount() {
  await createExpertAccount(accountForm.expertId, { username: accountForm.username, password: accountForm.password })
  ElMessage.success('账号创建成功')
  accountVisible.value = false
}

async function openCredit(row) {
  creditExpert.value = row
  const res = await getExpertCredit(row.id)
  creditRecords.value = res.data
  creditVisible.value = true
}

async function openMaterials(row) {
  materialExpert.value = row
  materialType.value = '简历'
  const res = await getExpertMaterials(row.id)
  materials.value = res.data
  materialVisible.value = true
}

async function uploadMaterial({ file }) {
  if (!materialExpert.value) return
  await uploadExpertMaterial(materialExpert.value.id, file, materialType.value)
  ElMessage.success('上传成功')
  const res = await getExpertMaterials(materialExpert.value.id)
  materials.value = res.data
}

function downloadMaterial(row) {
  downloadExpertMaterial(materialExpert.value.id, row.id, row.fileName)
}

async function removeMaterial(row) {
  await ElMessageBox.confirm('确认删除该材料？', '提示')
  await deleteExpertMaterial(materialExpert.value.id, row.id)
  ElMessage.success('已删除')
  const res = await getExpertMaterials(materialExpert.value.id)
  materials.value = res.data
}

function avoidanceTypeLabel(type) {
  return { ORGANIZATION: '单位', PROJECT: '项目', EXPERT: '人员' }[type] || type
}

async function openAvoidances(row) {
  avoidanceExpert.value = row
  Object.assign(avoidanceForm, { avoidType: 'ORGANIZATION', avoidValue: '', reason: '' })
  const res = await getExpertAvoidances(row.id)
  avoidances.value = res.data
  avoidanceVisible.value = true
}

async function saveAvoidance() {
  if (!avoidanceForm.avoidValue?.trim()) return ElMessage.warning('请填写回避对象')
  await addExpertAvoidance(avoidanceExpert.value.id, { ...avoidanceForm })
  ElMessage.success('已添加')
  const res = await getExpertAvoidances(avoidanceExpert.value.id)
  avoidances.value = res.data
  Object.assign(avoidanceForm, { avoidType: 'ORGANIZATION', avoidValue: '', reason: '' })
}

async function removeAvoidance(row) {
  await ElMessageBox.confirm('确认删除该回避规则？', '提示')
  await deleteExpertAvoidance(avoidanceExpert.value.id, row.id)
  ElMessage.success('已删除')
  const res = await getExpertAvoidances(avoidanceExpert.value.id)
  avoidances.value = res.data
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除该专家？', '提示')
  await deleteExpert(row.id)
  ElMessage.success('已删除')
  load()
}

function exportData() { downloadFile('/api/experts/export', 'experts.xlsx') }

async function handleImport({ file }) {
  const res = await importExperts(file)
  if (res.data?.length) ElMessage.warning(`导入完成，${res.data.length} 条错误`)
  else ElMessage.success('导入成功')
  load()
}

load()
</script>

<style scoped>
.toolbar-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.list-mode {
  margin-bottom: 16px;
}

.experts-card :deep(.el-card__body) {
  padding-top: 32px;
}

.experts-page :deep(.el-table) {
  --el-table-border-color: rgba(0, 0, 0, 0.04);
  border-radius: var(--app-radius-sm);
}

.experts-page :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.experts-pagination {
  margin-top: 24px;
  justify-content: flex-end;
}

.dialog-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.material-toolbar {
  margin-bottom: 16px;
  padding: 16px 20px;
  background: rgba(248, 250, 252, 0.85);
  border-radius: var(--app-radius-sm);
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.04);
}

.material-toolbar :deep(.el-form-item) {
  margin-bottom: 0;
}
</style>
