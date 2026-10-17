<template>
  <div v-loading="loading" class="airy-page review-page">
    <PageHeader
      :title="detail.projectName || '在线评审'"
      subtitle="查看材料并填写评分与评审意见"
      show-back
    />

    <div v-if="showAlerts" class="alert-stack">
      <el-alert v-if="detail.blindReview" class="airy-alert" title="盲审模式：申请人及单位信息已隐藏" type="warning" show-icon :closable="false" />
      <el-alert v-if="detail.assignmentStatus === 'PENDING'" class="airy-alert" title="请先返回列表接受邀请后再填写评审" type="info" show-icon :closable="false" />
      <el-alert
        v-if="deadlineUrgencyLevel === 'overdue' && detail.assignmentStatus !== 'SUBMITTED'"
        class="airy-alert"
        title="评审已逾期，请尽快提交"
        type="error"
        show-icon
        :closable="false"
      />
      <el-alert
        v-else-if="deadlineUrgencyLevel === 'soon' && detail.assignmentStatus !== 'SUBMITTED'"
        class="airy-alert"
        title="评审即将截止，请尽快完成"
        type="warning"
        show-icon
        :closable="false"
      />
    </div>

    <el-card class="content-card review-info-card" shadow="never">
      <h3 class="airy-section-title">项目信息</h3>
      <div class="info-grid">
        <p><strong>项目</strong>{{ detail.projectName }}</p>
        <p><strong>申请人</strong>{{ detail.applicant || '—' }}</p>
        <p><strong>单位</strong>{{ detail.organization || '—' }}</p>
        <p><strong>截止</strong><DeadlineCell :value="detail.deadline" /></p>
      </div>
      <div v-if="detail.materials?.length" class="materials-row">
        <span class="materials-label">评审材料</span>
        <div class="materials-list">
          <el-button
            v-for="m in detail.materials"
            :key="m.id"
            class="material-btn"
            @click="downloadReviewMaterial(m.id, m.fileName)"
          >
            {{ m.fileName }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card v-if="detail.task?.formConfig" class="content-card review-form-card" shadow="never">
      <h3 class="airy-section-title">评分表</h3>
      <el-form label-width="120px" class="review-form">
        <el-form-item v-for="item in detail.task.formConfig" :key="item.key" :label="item.name">
          <el-input-number v-model="scores[item.key]" :min="0" :max="item.maxScore" :disabled="readonly" />
          <span class="score-hint">满分 {{ item.maxScore }}</span>
        </el-form-item>
        <el-form-item label="评审结论">
          <el-select v-model="conclusion" style="width:260px" :disabled="readonly">
            <el-option v-for="c in conclusionOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!readonly" label="常用模板">
          <div class="template-row">
            <el-select v-model="selectedTemplate" placeholder="插入常用评语" clearable filterable style="flex:1"
              @change="applyTemplate">
              <el-option v-for="(t, i) in templates" :key="i" :label="templateLabel(t)" :value="t" />
            </el-select>
            <el-button link type="primary" @click="openTemplateManage">管理模板</el-button>
          </div>
        </el-form-item>
        <el-form-item label="评审意见">
          <el-input v-model="opinion" type="textarea" :rows="6" placeholder="请填写详细评审意见" :disabled="readonly" />
        </el-form-item>
        <el-form-item v-if="!readonly || detail.submission?.hasAttachment" label="补充附件">
          <div class="attachment-row">
            <el-button v-if="detail.submission?.hasAttachment" @click="downloadAttachment">
              {{ detail.submission.attachmentName || '下载附件' }}
            </el-button>
            <el-upload v-if="!readonly" :show-file-list="false" :http-request="handleAttachmentUpload" accept=".pdf,.doc,.docx,.zip">
              <el-button>{{ detail.submission?.hasAttachment ? '重新上传' : '上传附件' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <div v-if="!readonly" class="form-actions">
        <el-button @click="save(false)">保存草稿</el-button>
        <el-button type="primary" @click="save(true)">提交评审</el-button>
      </div>
      <el-alert v-else class="airy-alert submitted-alert" title="评审已提交，如需修改请联系管理员" type="info" show-icon :closable="false" />
    </el-card>

    <el-dialog v-model="templateVisible" class="airy-dialog" title="管理评审意见模板" width="600px">
      <p class="hint">保存后可在评审时快速插入，最多 20 条</p>
      <div v-for="(t, i) in editableTemplates" :key="i" class="template-edit-row">
        <el-input v-model="editableTemplates[i]" type="textarea" :rows="2" />
        <el-button link type="danger" @click="editableTemplates.splice(i, 1)">删除</el-button>
      </div>
      <el-button style="margin-top:12px" @click="editableTemplates.push('')">添加模板</el-button>
      <template #footer>
        <el-button @click="templateVisible=false">取消</el-button>
        <el-button type="primary" @click="saveTemplates">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReviewDetail, submitReview, getOpinionTemplates, saveOpinionTemplates, getReviewTypeConfig, downloadReviewMaterial, downloadSubmissionAttachment, uploadReviewAttachment } from '../api'
import PageHeader from '../components/PageHeader.vue'
import DeadlineCell from '../components/DeadlineCell.vue'
import { deadlineUrgency } from '../utils/deadline'

const route = useRoute()
const loading = ref(false)
const detail = ref({})
const scores = reactive({})
const opinion = ref('')
const conclusion = ref('')
const templates = ref([])
const selectedTemplate = ref('')
const templateVisible = ref(false)
const editableTemplates = ref([])
const conclusionOptions = ref(['推荐立项', '建议修改后通过', '不推荐立项'])

const readonly = computed(() => detail.value.assignmentStatus === 'SUBMITTED')
const deadlineUrgencyLevel = computed(() => deadlineUrgency(detail.value.deadline))

const showAlerts = computed(() =>
  detail.value.blindReview
  || detail.value.assignmentStatus === 'PENDING'
  || ((deadlineUrgencyLevel.value === 'overdue' || deadlineUrgencyLevel.value === 'soon')
    && detail.value.assignmentStatus !== 'SUBMITTED')
)

function templateLabel(text) {
  if (!text) return ''
  return text.length > 40 ? text.slice(0, 40) + '…' : text
}

function applyTemplate(value) {
  if (!value) return
  opinion.value = opinion.value ? `${opinion.value}\n${value}` : value
  selectedTemplate.value = ''
}

function openTemplateManage() {
  editableTemplates.value = [...templates.value]
  templateVisible.value = true
}

async function loadTemplates() {
  const res = await getOpinionTemplates()
  templates.value = res.data.templates || []
}

async function saveTemplates() {
  const cleaned = editableTemplates.value.map(t => t.trim()).filter(Boolean)
  const res = await saveOpinionTemplates({ templates: cleaned })
  templates.value = res.data.templates || []
  ElMessage.success('模板已保存')
  templateVisible.value = false
}

async function load() {
  loading.value = true
  try {
    const res = await getReviewDetail(route.params.id)
    detail.value = res.data
    if (res.data.reviewType) {
      const cfg = await getReviewTypeConfig(res.data.reviewType)
      if (cfg.data?.conclusions?.length) {
        conclusionOptions.value = cfg.data.conclusions
      }
    }
    if (res.data.submission) {
      Object.assign(scores, res.data.submission.scores || {})
      opinion.value = res.data.submission.opinion || ''
      conclusion.value = res.data.submission.conclusion || conclusionOptions.value[0] || ''
    }
  } finally {
    loading.value = false
  }
}

function downloadAttachment() {
  const sub = detail.value.submission
  if (!sub?.id) return
  downloadSubmissionAttachment(sub.id, sub.attachmentName || 'attachment')
}

async function handleAttachmentUpload({ file }) {
  const res = await uploadReviewAttachment(Number(route.params.id), file)
  if (!detail.value.submission) detail.value.submission = {}
  Object.assign(detail.value.submission, res.data)
  ElMessage.success('附件已上传')
}

async function save(submit) {
  if (submit) {
    await ElMessageBox.confirm('提交后将无法修改，确认提交？', '提示')
  }
  await submitReview({
    assignmentId: Number(route.params.id),
    scores: { ...scores },
    opinion: opinion.value,
    conclusion: conclusion.value,
    submit
  })
  ElMessage.success(submit ? '提交成功' : '草稿已保存')
  load()
}

onMounted(() => {
  load()
  loadTemplates()
})
</script>

<style scoped>
.review-info-card :deep(.el-card__body),
.review-form-card :deep(.el-card__body) {
  padding: 32px;
}

.materials-row {
  margin-top: 28px;
  padding: 20px 24px;
  background: rgba(248, 250, 252, 0.85);
  border-radius: var(--app-radius-sm);
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.04);
}

.materials-label {
  display: block;
  margin-bottom: 12px;
  color: var(--app-text-secondary);
  font-size: 13px;
  font-weight: 500;
}

.materials-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.material-btn {
  border-radius: var(--app-radius-sm);
  background: #fff;
  box-shadow: var(--app-shadow-soft);
  border: none;
}

.review-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.review-form :deep(.el-textarea__inner),
.review-form :deep(.el-input__wrapper),
.review-form :deep(.el-select__wrapper) {
  border-radius: var(--app-radius-sm);
}

.score-hint {
  margin-left: 10px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  padding-top: 24px;
}

.submitted-alert {
  margin-top: 8px;
}

.template-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 680px;
}

.template-edit-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.template-edit-row .el-input {
  flex: 1;
}

.attachment-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hint {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin: 0 0 16px;
}
</style>
