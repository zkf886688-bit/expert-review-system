<template>
  <div class="airy-page task-detail-page">
    <PageHeader
      :title="detail.task?.name || '任务详情'"
      subtitle="项目分配、催办与整改管理"
      show-back
    />

    <PageSkeleton v-if="loading && !detail.task?.name" :table-height="400" />

    <template v-else>
      <div v-if="showAlerts" class="alert-stack">
        <el-alert v-if="detail.task?.blindReview" class="airy-alert" title="本任务已开启盲审模式，专家端将隐藏申请人/单位信息" type="info" show-icon :closable="false" />
        <el-alert v-if="detail.task?.avoidSameOrg !== false" class="airy-alert" title="已开启同单位回避：专家与项目申报单位相同时不可分配" type="info" show-icon :closable="false" />
        <el-alert v-if="detail.task?.avoidDuplicateExpert !== false" class="airy-alert" title="已开启专家不重复：同一专家不可评审本任务下多个项目" type="info" show-icon :closable="false" />
        <el-alert v-if="isReadOnly" class="airy-alert" title="任务已结束（已汇总或已归档），分配操作已禁用" type="warning" show-icon :closable="false" />
        <el-alert v-if="needsPublish" class="airy-alert" title="请先发布任务后再分配专家" type="warning" show-icon :closable="false" />
        <el-alert v-if="detail.task?.status === 'PUBLISHED'" class="airy-alert" title="任务已发布，可以开始分配专家" type="success" show-icon :closable="false" />
        <el-alert
          v-for="(a, i) in anomalies"
          :key="`${a.type}-${i}`"
          class="airy-alert"
          :title="a.message"
          :type="a.level === 'ERROR' ? 'error' : 'warning'"
          show-icon
          :closable="false"
        />
      </div>

      <el-card class="content-card steps-card" shadow="never">
        <el-steps :active="taskStepActive" align-center finish-status="success" class="task-steps">
          <el-step title="创建任务" description="任务与项目已配置" />
          <el-step title="专家分配" description="邀请专家并确认" />
          <el-step title="在线评审" description="专家提交评分意见" />
          <el-step title="结果汇总" description="汇总导出与归档" />
          <el-step title="任务归档" description="只读留存" />
        </el-steps>
      </el-card>

      <div v-if="detail.assignSummary" class="mini-stat-grid">
      <div class="mini-stat" style="--mini-stat-accent:var(--app-primary)">
        <div class="mini-stat-value">{{ detail.assignSummary.totalProjects }}</div>
        <div class="mini-stat-label">待评项目</div>
      </div>
      <div class="mini-stat" style="--mini-stat-accent:#059669">
        <div class="mini-stat-value">{{ detail.assignSummary.completedProjects }}<span class="mini-stat-suffix">/ {{ detail.assignSummary.totalProjects }}</span></div>
        <div class="mini-stat-label">已完成评审</div>
      </div>
      <div class="mini-stat" style="--mini-stat-accent:#d97706">
        <div class="mini-stat-value">{{ detail.assignSummary.totalAssignments }}</div>
        <div class="mini-stat-label">专家分配</div>
      </div>
      <div class="mini-stat" style="--mini-stat-accent:#7c3aed">
        <div class="mini-stat-value">{{ detail.assignSummary.submittedAssignments }}</div>
        <div class="mini-stat-label">已提交评审</div>
        <el-progress :percentage="submitPercent" :stroke-width="6" :show-text="false" class="mini-stat-progress" />
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="content-card" shadow="never">
          <template #header>
            <div class="card-header-bar">
              <span>项目列表</span>
              <div class="card-header-actions">
                <el-button size="small" link type="warning" :disabled="isReadOnly || needsPublish || !detail.projects?.length" @click="openBatchRandom">批量随机分配</el-button>
                <el-button size="small" link type="primary" @click="downloadTemplate">导入模板</el-button>
                <el-button size="small" link type="primary" @click="exportProjects">导出</el-button>
                <el-upload :show-file-list="false" :http-request="importProjects">
                  <el-button size="small" type="primary">批量导入</el-button>
                </el-upload>
              </div>
            </div>
          </template>
          <div class="table-wrap">
          <el-table :data="detail.projects" size="small">
            <el-table-column prop="projectName" label="项目名称" />
            <el-table-column prop="applicant" label="申请人" width="90" />
            <el-table-column label="分配进度" width="150">
              <template #default="{ row }">
                <div class="progress-cell">
                  <el-progress :percentage="projectProgress(row).percent" :status="projectProgress(row).progressStatus"
                    :stroke-width="8" :show-text="false" />
                  <el-tag :type="projectProgress(row).type" size="small">{{ projectProgress(row).text }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="申报账号" width="180">
              <template #default="{ row }">
                <el-select :model-value="row.applicantUserId" placeholder="绑定账号" size="small" clearable
                  @change="v => bindApplicant(row, v)" style="width:100%">
                  <el-option v-for="u in applicantUsers" :key="u.id"
                    :label="`${u.realName || u.username}`" :value="u.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="轮次" width="60">
              <template #default="{ row }">{{ row.reviewRound || 1 }}</template>
            </el-table-column>
            <el-table-column label="整改" width="100">
              <template #default="{ row }">
                <el-tag size="small" v-if="row.rectificationStatus && row.rectificationStatus !== 'NONE'"
                  :type="rectificationStatusTag(row.rectificationStatus)">
                  {{ rectificationStatusLabel(row.rectificationStatus) }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="320">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="isReadOnly || needsPublish" @click="openAssign(row)">分配</el-button>
                <el-button link type="warning" :disabled="isReadOnly || needsPublish" @click="openRandom(row)">随机</el-button>
                <el-button link type="danger"
                  :disabled="isReadOnly || !row.applicantUserId || ['REQUESTED','SUBMITTED'].includes(row.rectificationStatus)"
                  @click="openRectification(row)">要求整改</el-button>
                <el-button link type="info"
                  v-if="row.rectificationStatus && row.rectificationStatus !== 'NONE'"
                  @click="viewRectification(row)">整改详情</el-button>
                <el-button link type="success"
                  :disabled="row.rectificationStatus !== 'SUBMITTED'"
                  @click="doStartReReview(row)">启动复审</el-button>
              </template>
            </el-table-column>
          </el-table>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="content-card" shadow="never">
          <template #header>
            <div class="card-header-bar">
              <span>评审材料</span>
              <div class="card-header-actions">
                <el-select v-model="materialType" size="small" style="width:120px;margin-right:8px">
                  <el-option label="申报书" value="申报书" />
                  <el-option label="附件" value="附件" />
                  <el-option label="补充材料" value="补充材料" />
                  <el-option label="其他" value="其他" />
                </el-select>
                <el-upload :show-file-list="false" :http-request="upload">
                  <el-button size="small">上传</el-button>
                </el-upload>
              </div>
            </div>
          </template>
          <div class="table-wrap">
          <el-table :data="detail.materials" size="small">
            <el-table-column prop="materialType" label="类型" width="90" />
            <el-table-column prop="fileName" label="文件名" />
            <el-table-column label="下载" width="80">
              <template #default="{ row }">
                <el-button link type="primary" @click="downloadMaterial(row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="content-card" shadow="never">
      <template #header>
        <div class="card-header-bar">
          <span>专家分配情况</span>
          <div class="card-header-actions">
            <el-button size="small" type="warning" :disabled="isReadOnly || !selectedAssignments.length"
              @click="doRemindSelected">催办选中 ({{ selectedAssignments.length }})</el-button>
            <el-button size="small" link type="primary" @click="exportAssignments">导出分配</el-button>
          </div>
        </div>
      </template>
      <div class="table-wrap">
      <el-table :data="detail.assignments" size="small" @selection-change="onAssignmentSelection">
        <el-table-column type="selection" width="45" :selectable="canRemindAssignment" />
        <el-table-column prop="projectName" label="项目" />
        <el-table-column prop="expertName" label="专家" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="assignmentStatusTag(row.status)" size="small">{{ assignmentStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因" min-width="120" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status === 'SUBMITTED'" link type="warning" @click="doReturn(row)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
    </el-card>

    <el-card class="content-card action-card" shadow="never">
      <div class="action-toolbar">
        <div class="action-primary">
          <el-button type="primary" @click="doPublish" :disabled="isReadOnly || !needsPublish" :loading="publishing">发布任务</el-button>
          <el-button type="success" @click="$router.push(`/summary/${taskId}`)">查看汇总</el-button>
          <el-button type="warning" @click="doRemind" :disabled="isReadOnly">催办未提交</el-button>
        </div>
        <el-dropdown trigger="click">
          <el-button>
            更多操作
            <AppIcon name="arrow-left" :size="14" style="transform:rotate(-90deg);margin-left:4px" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :disabled="isReadOnly || detail.task?.status === 'ARCHIVED'" @click="openDeadline">修改截止时间</el-dropdown-item>
              <el-dropdown-item :disabled="isReadOnly" @click="openFormConfig">评分表配置</el-dropdown-item>
              <el-dropdown-item :disabled="isReadOnly" @click="doSummarize">标记已汇总</el-dropdown-item>
              <el-dropdown-item @click="openClone">复制任务</el-dropdown-item>
              <el-dropdown-item :disabled="isReadOnly" divided @click="doArchive">归档任务</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-card>
    </template>

    <el-dialog v-model="formConfigVisible" class="airy-dialog" title="评分表配置" width="640px">
      <p class="hint">权重之和须为 1.0；修改后仅影响尚未提交的评审</p>
      <el-table :data="formConfigRows" size="small">
        <el-table-column label="标识(key)" width="120">
          <template #default="{ row }"><el-input v-model="row.key" size="small" /></template>
        </el-table-column>
        <el-table-column label="名称" min-width="120">
          <template #default="{ row }"><el-input v-model="row.name" size="small" /></template>
        </el-table-column>
        <el-table-column label="满分" width="100">
          <template #default="{ row }"><el-input-number v-model="row.maxScore" :min="1" size="small" /></template>
        </el-table-column>
        <el-table-column label="权重" width="100">
          <template #default="{ row }"><el-input-number v-model="row.weight" :min="0" :max="1" :step="0.05" size="small" /></template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" @click="formConfigRows.splice($index, 1)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button style="margin-top:8px" @click="formConfigRows.push({ key: '', name: '', maxScore: 10, weight: 0.1 })">添加评分项</el-button>
      <template #footer>
        <el-button @click="formConfigVisible=false">取消</el-button>
        <el-button type="primary" @click="saveFormConfig">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deadlineVisible" class="airy-dialog" title="修改截止时间" width="420px">
      <el-date-picker v-model="deadlineForm.deadline" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
        placeholder="选择新截止时间" style="width:100%" />
      <template #footer>
        <el-button @click="deadlineVisible=false">取消</el-button>
        <el-button type="primary" @click="saveDeadline">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignVisible" class="airy-dialog" title="手动分配专家" width="500px">
      <p>项目：{{ currentProject?.projectName }}</p>
      <el-select v-model="selectedExperts" multiple filterable placeholder="选择专家" style="width:100%">
        <el-option v-for="e in experts" :key="e.id" :label="`${e.name} - ${e.organization}`" :value="e.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignVisible=false">取消</el-button>
        <el-button type="primary" @click="doAssign">发送邀请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchResultVisible" class="airy-dialog" title="批量分配结果" width="560px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="成功项目">{{ batchResult?.processedProjects || 0 }}</el-descriptions-item>
        <el-descriptions-item label="分配人次">{{ batchResult?.totalAssigned || 0 }}</el-descriptions-item>
        <el-descriptions-item label="跳过项目">{{ batchResult?.skippedProjects || 0 }}</el-descriptions-item>
        <el-descriptions-item label="失败项">{{ batchResult?.errors?.length || 0 }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="batchResult?.details?.length" :data="batchResult.details" size="small" style="margin-top:12px" max-height="240">
        <el-table-column prop="projectName" label="项目" min-width="140" />
        <el-table-column label="分配专家" min-width="200">
          <template #default="{ row }">{{ (row.expertNames || row.expertIds || []).join('、') || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-alert v-for="(err, i) in batchResult?.errors || []" :key="i" :title="err" type="error" show-icon :closable="false" style="margin-top:8px" />
      <template #footer>
        <el-button type="primary" @click="batchResultVisible=false">知道了</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchRandomVisible" class="airy-dialog" title="批量随机分配专家" width="460px">
      <p class="hint">将为任务下所有项目随机抽取专家；已分配足够的项目可自动跳过</p>
      <el-form label-width="100px">
        <el-form-item label="每项目人数"><el-input-number v-model="batchRandomForm.count" :min="1" /></el-form-item>
        <el-form-item label="专业领域"><el-input v-model="batchRandomForm.field" placeholder="可选，如：人工智能" /></el-form-item>
        <el-form-item label="同单位回避"><el-switch v-model="batchRandomForm.excludeSameOrg" /></el-form-item>
        <el-form-item label="跳过已满"><el-switch v-model="batchRandomForm.skipAssigned" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchRandomVisible=false">取消</el-button>
        <el-button type="primary" @click="doBatchRandom">开始分配</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rectificationVisible" class="airy-dialog" title="要求项目整改" width="480px">
      <p v-if="currentProject">项目：{{ currentProject.projectName }}（第 {{ currentProject.reviewRound || 1 }} 轮）</p>
      <el-input v-model="rectificationForm.note" type="textarea" :rows="4" placeholder="请填写整改要求，将通知申报人" />
      <template #footer>
        <el-button @click="rectificationVisible=false">取消</el-button>
        <el-button type="primary" @click="doRequestRectification">发送整改通知</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rectDetailVisible" class="airy-dialog" title="整改详情" width="560px">
      <template v-if="rectDetail">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="状态">{{ rectificationStatusLabel(rectDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="评审轮次">第 {{ rectDetail.reviewRound }} 轮</el-descriptions-item>
          <el-descriptions-item v-if="rectDetail.note" label="整改要求">{{ rectDetail.note }}</el-descriptions-item>
          <el-descriptions-item v-if="rectDetail.reply" label="申报人说明">{{ rectDetail.reply }}</el-descriptions-item>
          <el-descriptions-item v-if="rectDetail.requestedAt" label="发起时间">{{ rectDetail.requestedAt }}</el-descriptions-item>
          <el-descriptions-item v-if="rectDetail.submittedAt" label="提交时间">{{ rectDetail.submittedAt }}</el-descriptions-item>
        </el-descriptions>
        <el-table v-if="rectDetail.materials?.length" :data="rectDetail.materials" size="small" style="margin-top:12px">
          <el-table-column prop="fileName" label="整改材料" />
          <el-table-column prop="createdAt" label="上传时间" width="170" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="downloadRectMaterial(row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
      <template #footer>
        <el-button v-if="rectDetail?.canCancel" type="warning" @click="doCancelRectification">取消整改</el-button>
        <el-button v-if="rectDetail?.canStartReReview" type="success" @click="doStartReReviewFromDetail">启动复审</el-button>
        <el-button @click="rectDetailVisible=false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="randomVisible" class="airy-dialog" title="随机抽取专家" width="420px">
      <el-form label-width="100px">
        <el-form-item label="抽取人数"><el-input-number v-model="randomForm.count" :min="1" /></el-form-item>
        <el-form-item label="专业领域"><el-input v-model="randomForm.field" placeholder="可选，如：人工智能" /></el-form-item>
        <el-form-item label="同单位回避"><el-switch v-model="randomForm.excludeSameOrg" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="randomVisible=false">取消</el-button>
        <el-button type="primary" @click="doRandom">开始抽取</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { assignmentStatusLabel, assignmentStatusTag, rectificationStatusLabel, rectificationStatusTag } from '../utils/labels'
import { getTask, getExperts, getApplicantUsers, bindProjectApplicant, assignExperts, randomAssignExperts, batchRandomAssignExperts, updateTaskDeadline, updateTaskFormConfig, remindPendingExperts, uploadMaterial, summarizeTask, archiveTask, publishTask, cloneTask, importTaskProjects, returnReview, downloadReviewMaterial, requestRectification, cancelRectification, getRectification, startReReview, downloadRectificationMaterial, getTaskAnomalies, downloadFile } from '../api'
import PageHeader from '../components/PageHeader.vue'
import PageSkeleton from '../components/PageSkeleton.vue'

const route = useRoute()
const router = useRouter()
const taskId = route.params.id
const loading = ref(false)
const detail = ref({ task: {}, projects: [], materials: [], assignments: [], projectStats: [], assignSummary: null })
const experts = ref([])
const applicantUsers = ref([])
const assignVisible = ref(false)
const randomVisible = ref(false)
const batchRandomVisible = ref(false)
const batchResultVisible = ref(false)
const batchResult = ref(null)
const selectedAssignments = ref([])
const deadlineVisible = ref(false)
const deadlineForm = reactive({ deadline: '' })
const formConfigVisible = ref(false)
const formConfigRows = ref([])
const currentProject = ref(null)
const selectedExperts = ref([])
const randomForm = reactive({ count: 3, field: '', excludeSameOrg: true })
const batchRandomForm = reactive({ count: 3, field: '', excludeSameOrg: true, skipAssigned: true })
const rectificationVisible = ref(false)
const rectificationForm = reactive({ note: '' })
const rectDetailVisible = ref(false)
const rectDetail = ref(null)
const rectDetailProject = ref(null)
const anomalies = ref([])
const materialType = ref('申报书')
const publishing = ref(false)

const isReadOnly = computed(() => {
  const s = detail.value.task?.status
  return s === 'SUMMARIZED' || s === 'ARCHIVED'
})

const needsPublish = computed(() => detail.value.task?.status === 'UNASSIGNED')

const showAlerts = computed(() =>
  detail.value.task?.blindReview
  || detail.value.task?.avoidSameOrg !== false
  || detail.value.task?.avoidDuplicateExpert !== false
  || isReadOnly.value
  || needsPublish.value
  || detail.value.task?.status === 'PUBLISHED'
  || anomalies.value.length
)

const taskStepActive = computed(() => {
  const map = { UNASSIGNED: 0, PUBLISHED: 1, PENDING_CONFIRM: 1, IN_REVIEW: 2, SUMMARIZED: 3, ARCHIVED: 4 }
  return map[detail.value.task?.status] ?? 0
})

const submitPercent = computed(() => {
  const sum = detail.value.assignSummary
  if (!sum?.totalAssignments) return 0
  return Math.min(100, Math.round((sum.submittedAssignments / sum.totalAssignments) * 100))
})

function projectProgress(project) {
  const stat = (detail.value.projectStats || []).find(s => s.projectId === project.id)
  const min = stat?.minReviewers || detail.value.task?.minReviewers || 3
  const submitted = stat?.submittedCount ?? 0
  const assigned = stat?.assignedCount ?? 0
  const percent = Math.min(100, Math.round((submitted / min) * 100))
  if (stat?.completed || submitted >= min) {
    return { text: `${submitted}/${min} 已完成`, type: 'success', percent, progressStatus: 'success' }
  }
  if (assigned >= min) {
    return { text: `${submitted}/${min} 评审中`, type: 'warning', percent, progressStatus: 'warning' }
  }
  return {
    text: `${assigned}/${min} 已分配`,
    type: assigned > 0 ? 'primary' : 'info',
    percent: Math.min(100, Math.round((assigned / min) * 100)),
    progressStatus: assigned > 0 ? '' : 'exception'
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getTask(taskId)
    detail.value = res.data
    await loadAnomalies()
  } finally {
    loading.value = false
  }
}

async function loadAnomalies() {
  try {
    const res = await getTaskAnomalies(taskId)
    anomalies.value = res.data || []
  } catch {
    anomalies.value = []
  }
}

async function loadExperts() {
  const res = await getExperts({ page: 1, size: 500, status: 'ACTIVE' })
  experts.value = res.data.records
}

async function loadApplicantUsers() {
  try {
    const res = await getApplicantUsers()
    applicantUsers.value = res.data
  } catch { applicantUsers.value = [] }
}

async function bindApplicant(project, applicantUserId) {
  await bindProjectApplicant(taskId, project.id, applicantUserId || null)
  ElMessage.success('已更新申报人绑定')
  load()
}

function openAssign(project) {
  if (needsPublish.value) return ElMessage.warning('请先发布任务后再分配专家')
  currentProject.value = project
  selectedExperts.value = []
  assignVisible.value = true
  loadExperts()
}

function openRandom(project) {
  if (needsPublish.value) return ElMessage.warning('请先发布任务后再分配专家')
  currentProject.value = project
  randomForm.count = detail.value.task?.minReviewers || 3
  randomVisible.value = true
}

function openBatchRandom() {
  if (needsPublish.value) return ElMessage.warning('请先发布任务后再分配专家')
  if (!detail.value.projects?.length) return ElMessage.warning('请先添加待评项目')
  batchRandomForm.count = detail.value.task?.minReviewers || 3
  batchRandomForm.field = ''
  batchRandomForm.excludeSameOrg = detail.value.task?.avoidSameOrg !== false
  batchRandomForm.skipAssigned = true
  batchRandomVisible.value = true
}

async function doBatchRandom() {
  await ElMessageBox.confirm(
    `将为 ${detail.value.projects.length} 个项目各分配 ${batchRandomForm.count} 名专家（已满项目可跳过），确认继续？`,
    '批量随机分配',
    { type: 'warning' }
  ).catch(() => { throw new Error('cancel') })
  try {
    const res = await batchRandomAssignExperts(taskId, batchRandomForm)
    batchResult.value = res.data
    batchRandomVisible.value = false
    batchResultVisible.value = true
    load()
  } catch (e) {
    if (e.message !== 'cancel') throw e
  }
}

async function doAssign() {
  await assignExperts(taskId, { projectId: currentProject.value.id, expertIds: selectedExperts.value })
  ElMessage.success('邀请已发送，等待专家确认')
  assignVisible.value = false
  load()
}

async function doRandom() {
  const res = await randomAssignExperts(taskId, {
    projectId: currentProject.value.id,
    count: randomForm.count,
    field: randomForm.field,
    excludeSameOrg: randomForm.excludeSameOrg
  })
  ElMessage.success(`已随机抽取 ${res.data.length} 名专家并发送邀请`)
  randomVisible.value = false
  load()
}

async function upload({ file }) {
  await uploadMaterial(taskId, file, null, materialType.value)
  ElMessage.success('上传成功')
  load()
}

async function doPublish() {
  publishing.value = true
  try {
    await publishTask(taskId)
    ElMessage.success(`任务已发布（${detail.value.projects?.length || 0} 个项目），可开始分配专家`)
    load()
  } finally {
    publishing.value = false
  }
}

async function doRemind() {
  try {
    await ElMessageBox.confirm('将向所有待确认/未提交的专家重新发送提醒，确认？', '催办')
    const res = await remindPendingExperts(taskId)
    ElMessage.success(`已催办 ${res.data.remindedCount} 位专家`)
  } catch { /* cancelled or error handled by interceptor */ }
}

function canRemindAssignment(row) {
  return ['PENDING', 'ACCEPTED', 'OVERDUE'].includes(row.status)
}

function onAssignmentSelection(rows) {
  selectedAssignments.value = rows || []
}

async function doRemindSelected() {
  if (!selectedAssignments.value.length) return
  try {
    await ElMessageBox.confirm(`将向选中的 ${selectedAssignments.value.length} 位专家发送催办提醒，确认？`, '批量催办')
    const res = await remindPendingExperts(taskId, {
      assignmentIds: selectedAssignments.value.map(r => r.id)
    })
    ElMessage.success(`已催办 ${res.data.remindedCount} 位专家`)
    selectedAssignments.value = []
  } catch { /* cancelled */ }
}

function openDeadline() {
  deadlineForm.deadline = detail.value.task?.deadline || ''
  deadlineVisible.value = true
}

async function saveDeadline() {
  if (!deadlineForm.deadline) return ElMessage.warning('请选择截止时间')
  await updateTaskDeadline(taskId, { deadline: deadlineForm.deadline })
  ElMessage.success('截止时间已更新')
  deadlineVisible.value = false
  load()
}

function openFormConfig() {
  formConfigRows.value = JSON.parse(JSON.stringify(detail.value.task?.formConfig || []))
  formConfigVisible.value = true
}

async function saveFormConfig() {
  await updateTaskFormConfig(taskId, { formConfig: formConfigRows.value })
  ElMessage.success('评分表已保存')
  formConfigVisible.value = false
  load()
}

async function exportAssignments() {
  try {
    await downloadFile(`/api/tasks/${taskId}/assignments/export`, `task_${taskId}_assignments.xlsx`)
  } catch (e) {
    ElMessage.error(e.message || '导出分配失败')
  }
}

function openRectification(row) {
  currentProject.value = row
  rectificationForm.note = row.rectificationNote || ''
  rectificationVisible.value = true
}

async function doRequestRectification() {
  if (!rectificationForm.note?.trim()) return ElMessage.warning('请填写整改要求')
  await requestRectification(taskId, currentProject.value.id, { note: rectificationForm.note.trim() })
  ElMessage.success('已通知申报人整改')
  rectificationVisible.value = false
  load()
}

async function doStartReReview(row) {
  await ElMessageBox.confirm(
    `确认启动「${row.projectName}」第 ${(row.reviewRound || 1) + 1} 轮复审？已分配专家需重新提交评审。`,
    '启动复审'
  )
  await startReReview(taskId, row.id)
  ElMessage.success('已启动复审')
  load()
}

async function viewRectification(row) {
  rectDetailProject.value = row
  const res = await getRectification(taskId, row.id)
  rectDetail.value = res.data
  rectDetailVisible.value = true
}

async function doCancelRectification() {
  if (!rectDetailProject.value) return
  await ElMessageBox.confirm('确认取消整改要求？申报人将收到通知。', '取消整改')
  await cancelRectification(taskId, rectDetailProject.value.id)
  ElMessage.success('已取消整改')
  rectDetailVisible.value = false
  load()
}

async function doStartReReviewFromDetail() {
  if (!rectDetailProject.value) return
  await doStartReReview(rectDetailProject.value)
  rectDetailVisible.value = false
}

function downloadRectMaterial(row) {
  if (!rectDetailProject.value) return
  downloadRectificationMaterial(taskId, rectDetailProject.value.id, row.id, row.fileName, false)
}

async function doSummarize() {
  await summarizeTask(taskId)
  ElMessage.success('已标记汇总')
  load()
}

async function doArchive() {
  await ElMessageBox.confirm('归档后任务只读，确认归档？', '提示')
  await archiveTask(taskId)
  ElMessage.success('已归档')
  load()
}

async function openClone() {
  const { value } = await ElMessageBox.prompt('请输入新截止时间', '复制任务', {
    inputPattern: /.+/,
    inputErrorMessage: '必填',
    inputPlaceholder: '如 2026-12-31 18:00:00'
  }).catch(() => ({ value: null }))
  if (!value) return
  const deadline = value.includes('T') ? value : value.replace(' ', 'T')
  const res = await cloneTask(taskId, { deadline })
  ElMessage.success('任务已复制')
  router.push(`/tasks/${res.data.id}`)
}

async function downloadTemplate() {
  try {
    await downloadFile('/api/tasks/projects/import-template', '项目导入模板.xlsx')
  } catch (e) {
    ElMessage.error(e.message || '下载模板失败')
  }
}

async function exportProjects() {
  try {
    await downloadFile(`/api/tasks/${taskId}/projects/export`, `task_${taskId}_projects.xlsx`)
  } catch (e) {
    ElMessage.error(e.message || '导出项目失败')
  }
}

async function importProjects({ file }) {
  const res = await importTaskProjects(taskId, file)
  const errCount = res.data.errors?.length || 0
  ElMessage.success(`导入成功 ${res.data.successCount} 条${errCount ? `，失败 ${errCount} 条` : ''}`)
  if (errCount) console.warn(res.data.errors)
  load()
  loadApplicantUsers()
}

async function doReturn(row) {
  await ElMessageBox.confirm('确认退回该专家的评审？', '提示')
  await returnReview(row.id)
  ElMessage.success('已退回')
  load()
}

function downloadMaterial(row) {
  downloadReviewMaterial(row.id, row.fileName)
}

onMounted(() => { load(); loadApplicantUsers() })
</script>

<style scoped>
.hint { font-size: 12px; color: var(--app-text-secondary); margin: 0 0 12px; }
.progress-cell { display: flex; flex-direction: column; gap: 4px; }
.progress-cell .el-progress { width: 100%; }
.mini-stat-suffix { font-size: 16px; font-weight: 500; color: var(--app-text-secondary); margin-left: 4px; }
.mini-stat-progress { margin-top: 10px; }
.steps-card :deep(.el-card__body) { padding: 28px 32px 32px; }
.task-steps { max-width: 920px; margin: 0 auto; }
.action-card :deep(.el-card__body) { padding: 24px 32px; }
.action-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.action-primary { display: flex; flex-wrap: wrap; gap: 10px; }
.alert-stack { display: flex; flex-direction: column; gap: 12px; margin-bottom: 4px; }
</style>
