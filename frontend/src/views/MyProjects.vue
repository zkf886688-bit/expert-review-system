<template>
  <div class="airy-page">
    <PageHeader title="我的申报项目" subtitle="查看评审进度、整改要求与评审结果">
      <template #extra>
        <el-button link type="primary" @click="$router.push('/notifications')">消息中心</el-button>
      </template>
    </PageHeader>

    <el-card class="content-card" shadow="never">
      <div v-if="list.length || loading" class="table-wrap">
      <el-table
        :data="list"
        v-loading="loading"
        stripe
        :row-class-name="rowClassName"
        @row-click="openDetail"
      >
        <el-table-column prop="projectName" label="项目名称" min-width="180" />
        <el-table-column prop="taskName" label="评审任务" min-width="160" />
        <el-table-column label="轮次" width="70">
          <template #default="{ row }">第 {{ row.reviewRound || 1 }} 轮</template>
        </el-table-column>
        <el-table-column prop="reviewType" label="类型" width="100" />
        <el-table-column label="截止时间" width="200">
          <template #default="{ row }">
            <DeadlineCell :value="row.deadline" />
          </template>
        </el-table-column>
      <el-table-column label="评审进度" width="140">
        <template #default="{ row }">
          {{ row.submittedCount }} / {{ row.minReviewers }} 已提交
        </template>
      </el-table-column>
      <el-table-column label="整改" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.rectificationStatus && row.rectificationStatus !== 'NONE'" size="small" :type="rectificationStatusTag(row.rectificationStatus)">
            {{ rectificationStatusLabel(row.rectificationStatus) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="taskStatus" label="任务状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="taskStatusTag(row.taskStatus)">{{ taskStatusLabel(row.taskStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>
    <EmptyGuide
      v-else-if="!loading"
      description="暂无关联的申报项目，请联系管理员绑定"
      action-text="查看消息"
      action-to="/notifications"
      icon="folder-open"
    />
    </el-card>

    <el-drawer v-model="drawerVisible" :title="current?.projectName || '项目详情'" size="560px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="项目名称">{{ current.projectName }}</el-descriptions-item>
          <el-descriptions-item label="申报单位">{{ current.organization || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评审任务">{{ current.taskName }}</el-descriptions-item>
          <el-descriptions-item label="评审轮次">第 {{ current.reviewRound || 1 }} 轮</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ current.deadline }}</el-descriptions-item>
          <el-descriptions-item label="评审进度">{{ current.submittedCount }} / {{ current.minReviewers }} 已提交</el-descriptions-item>
          <el-descriptions-item label="任务状态">{{ taskStatusLabel(current.taskStatus) }}</el-descriptions-item>
          <el-descriptions-item label="整改状态">
            <el-tag size="small" :type="rectificationStatusTag(current.rectificationStatus)">
              {{ rectificationStatusLabel(current.rectificationStatus) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <template v-if="current.rectification?.status === 'REQUESTED'">
          <h4 style="margin-top:20px">整改要求</h4>
          <el-alert type="warning" show-icon :closable="false">
            <template #title>请在提交前完成整改说明与材料上传</template>
            <p style="margin:8px 0 0">{{ current.rectification.note || '管理员要求您提交整改材料' }}</p>
            <p v-if="current.rectification.requestedAt" class="hint">发起时间：{{ current.rectification.requestedAt }}</p>
          </el-alert>
          <el-form label-width="80px" style="margin-top:12px">
            <el-form-item label="整改说明">
              <el-input v-model="rectReply" type="textarea" :rows="3" placeholder="请说明整改内容" />
            </el-form-item>
            <el-form-item label="材料">
              <el-upload :show-file-list="false" :http-request="uploadRectMaterial">
                <el-button size="small" type="primary">上传材料</el-button>
              </el-upload>
              <p class="hint">可多次上传，确认无误后点击「提交整改」</p>
            </el-form-item>
            <el-table v-if="current.rectification.materials?.length" :data="current.rectification.materials" size="small" style="margin-bottom:12px">
              <el-table-column prop="fileName" label="已上传材料" />
              <el-table-column prop="createdAt" label="时间" width="170" />
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button link type="primary" @click="downloadRect(row)">下载</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button type="primary" :loading="rectSubmitting" @click="submitRect">提交整改</el-button>
          </el-form>
        </template>

        <template v-else-if="current.rectification && current.rectification.status !== 'NONE'">
          <h4 style="margin-top:20px">整改记录</h4>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item v-if="current.rectification.note" label="整改要求">{{ current.rectification.note }}</el-descriptions-item>
            <el-descriptions-item v-if="current.rectification.reply" label="我的说明">{{ current.rectification.reply }}</el-descriptions-item>
            <el-descriptions-item v-if="current.rectification.submittedAt" label="提交时间">{{ current.rectification.submittedAt }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="current.rectification.materials?.length" :data="current.rectification.materials" size="small" style="margin-top:12px">
            <el-table-column prop="fileName" label="整改材料" />
            <el-table-column prop="createdAt" label="上传时间" width="170" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click="downloadRect(row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-alert v-if="current.rectification.status === 'SUBMITTED'" title="整改已提交，等待管理员启动复审" type="success" show-icon :closable="false" style="margin-top:12px" />
          <el-alert v-if="current.rectification.status === 'RE_REVIEWING'" title="复审进行中，专家重新评审完成后系统将自动更新状态" type="info" show-icon :closable="false" style="margin-top:12px" />
        </template>

        <h4 style="margin-top:20px">申报材料</h4>
        <el-table :data="current.materials || []" size="small">
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="createdAt" label="上传时间" width="170" />
        </el-table>
        <el-upload v-if="current.canUpload" :show-file-list="false" :http-request="upload" style="margin-top:12px">
          <el-button type="primary" size="small">上传材料</el-button>
        </el-upload>
        <el-alert v-else title="当前不可上传（已截止或任务已结束）" type="warning" show-icon :closable="false" style="margin-top:12px" />

        <h4 style="margin-top:20px">评审结果</h4>
        <template v-if="current.result?.available">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="平均分">{{ current.result.avgScore ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="综合结论">{{ current.result.recommendation }}</el-descriptions-item>
            <el-descriptions-item label="最高分">{{ current.result.maxScore ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="最低分">{{ current.result.minScore ?? '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-table :data="current.result.opinions || []" size="small" style="margin-top:12px">
            <el-table-column prop="expertLabel" label="评审专家" width="90" />
            <el-table-column prop="totalScore" label="得分" width="70" />
            <el-table-column prop="conclusion" label="结论" width="120" />
            <el-table-column prop="opinion" label="意见" show-overflow-tooltip />
          </el-table>
          <p class="hint">专家信息已脱敏显示</p>
        </template>
        <el-alert v-else :title="current.result?.message || '评审进行中'" type="info" show-icon :closable="false" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { taskStatusLabel, taskStatusTag, rectificationStatusLabel, rectificationStatusTag } from '../utils/labels'
import {
  getApplicantProjects, getApplicantProject, uploadApplicantMaterial,
  submitRectification, uploadRectificationMaterial, downloadRectificationMaterial
} from '../api'
import PageHeader from '../components/PageHeader.vue'
import DeadlineCell from '../components/DeadlineCell.vue'
import EmptyGuide from '../components/EmptyGuide.vue'
import { deadlineRowClassName } from '../utils/deadline'

const loading = ref(false)
const list = ref([])
const drawerVisible = ref(false)
const current = ref(null)
const rectReply = ref('')
const rectSubmitting = ref(false)

function rowClassName({ row }) {
  if (row.rectificationStatus === 'REQUESTED') return 'row-rect-pending'
  const deadlineClass = deadlineRowClassName(row.deadline)
  return deadlineClass || ''
}

async function load() {
  loading.value = true
  try {
    const res = await getApplicantProjects()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

async function refreshCurrent() {
  if (!current.value) return
  const res = await getApplicantProject(current.value.projectId)
  current.value = res.data
}

async function openDetail(row) {
  const res = await getApplicantProject(row.projectId)
  current.value = res.data
  rectReply.value = ''
  drawerVisible.value = true
}

async function uploadRectMaterial({ file }) {
  if (!current.value) return
  await uploadRectificationMaterial(current.value.projectId, file)
  ElMessage.success('材料已上传')
  await refreshCurrent()
}

async function submitRect() {
  if (!current.value) return
  const hasMaterials = current.value.rectification?.materials?.length > 0
  if (!rectReply.value?.trim() && !hasMaterials) {
    return ElMessage.warning('请填写整改说明或上传材料')
  }
  rectSubmitting.value = true
  try {
    await submitRectification(current.value.projectId, rectReply.value?.trim())
    ElMessage.success('整改已提交')
    await refreshCurrent()
    rectReply.value = ''
    load()
  } finally {
    rectSubmitting.value = false
  }
}

function downloadRect(row) {
  if (!current.value) return
  downloadRectificationMaterial(null, current.value.projectId, row.id, row.fileName, true)
}

async function upload({ file }) {
  if (!current.value) return
  await uploadApplicantMaterial(current.value.taskId, current.value.projectId, file)
  ElMessage.success('上传成功')
  await refreshCurrent()
  load()
}

onMounted(load)
</script>

<style scoped>
:deep(.el-table__row) { cursor: pointer; }
:deep(.row-rect-pending) { background-color: #fdf6ec !important; }
.hint { font-size: 12px; color: #909399; margin-top: 8px; }
</style>
