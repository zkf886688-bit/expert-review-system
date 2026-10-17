<template>
  <div class="airy-page settings-page">
    <PageHeader title="系统设置" subtitle="个人资料、密码与系统配置" />

    <el-row :gutter="16" class="settings-row">
      <el-col :xs="24" :sm="24" :md="12">
        <el-card header="个人资料" class="settings-card content-card">
          <el-form label-width="88px" class="settings-form">
            <el-form-item label="用户名">
              <el-input :model-value="userStore.user?.username" disabled />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="profile.realName" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profile.email" />
            </el-form-item>
            <el-form-item label="手机">
              <el-input v-model="profile.phone" />
            </el-form-item>
            <el-form-item class="form-actions">
              <el-button type="primary" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="24" :md="12">
        <el-card header="修改密码" class="settings-card content-card">
          <el-form label-width="88px" class="settings-form">
            <el-form-item label="原密码">
              <el-input v-model="pwd.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="pwd.newPassword" type="password" show-password placeholder="至少6位" />
            </el-form-item>
            <el-form-item label="确认密码">
              <el-input v-model="pwd.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item class="form-actions">
              <el-button type="primary" @click="changePwd">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <template v-if="userStore.isSystemAdmin">
      <el-row :gutter="16" class="settings-row">
        <el-col :xs="24" :sm="24" :md="12">
          <el-card header="系统状态" class="settings-card content-card" v-loading="infoLoading">
            <el-descriptions v-if="sysInfo" :column="1" border size="small" class="sys-desc">
              <el-descriptions-item label="应用">{{ sysInfo.appName }}</el-descriptions-item>
              <el-descriptions-item label="版本">{{ sysInfo.version }}</el-descriptions-item>
              <el-descriptions-item label="健康状态">
                <el-tag :type="sysInfo.healthStatus === 'UP' ? 'success' : 'danger'" size="small">
                  {{ sysInfo.healthStatus }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="用户数">{{ sysInfo.userCount }}</el-descriptions-item>
              <el-descriptions-item label="邮件服务">{{ mailStatusText }}</el-descriptions-item>
              <el-descriptions-item label="短信服务">{{ smsStatusText }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="24" :md="12">
          <el-card header="邮件测试" class="settings-card content-card">
            <el-form label-width="88px" class="settings-form">
              <el-form-item label="收件人">
                <el-input v-model="mail.to" />
              </el-form-item>
              <el-form-item label="主题">
                <el-input v-model="mail.subject" />
              </el-form-item>
              <el-form-item label="内容">
                <el-input v-model="mail.content" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item class="form-actions">
                <el-button type="primary" @click="testMail">发送测试</el-button>
              </el-form-item>
              <p class="hint">未启用 SMTP 时请在 backend 日志查看 [邮件模拟]</p>
            </el-form>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="settings-row">
        <el-col :xs="24" :sm="24" :md="12">
          <el-card header="短信测试" class="settings-card content-card">
            <el-form label-width="88px" class="settings-form">
              <el-form-item label="手机号">
                <el-input v-model="sms.phone" placeholder="11 位手机号" />
              </el-form-item>
              <el-form-item label="内容">
                <el-input v-model="sms.content" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item class="form-actions">
                <el-button type="primary" @click="testSmsSend">发送测试</el-button>
              </el-form-item>
              <p class="hint">未启用真实短信时请在 backend 日志查看 [短信模拟]</p>
            </el-form>
          </el-card>
        </el-col>

        <el-col :xs="24" :sm="24" :md="12">
          <el-card header="开发者工具" class="settings-card content-card">
            <el-descriptions :column="1" border size="small" class="dev-desc">
              <el-descriptions-item label="Swagger UI">
                <el-link href="/swagger-ui.html" target="_blank">/swagger-ui.html</el-link>
              </el-descriptions-item>
              <el-descriptions-item label="健康检查">
                <div class="dev-health">
                  <el-link href="/api/public/health" target="_blank">/api/public/health</el-link>
                  <el-link href="/actuator/prometheus" target="_blank">/actuator/prometheus</el-link>
                  <el-button link type="primary" @click="openHealthDetail">查看详情</el-button>
                </div>
              </el-descriptions-item>
            </el-descriptions>
            <p class="hint">健康检查返回 JSON，status 为 UP 即表示服务正常</p>
          </el-card>
        </el-col>
      </el-row>

      <el-card header="通知模板" class="settings-card content-card settings-row" v-loading="tplLoading">
        <div class="tpl-toolbar">
          <p class="hint tpl-hint">模板保存在数据库，修改后立即生效（无需重启）。占位符：{{ placeholderText }}</p>
          <div class="tpl-actions">
            <el-button @click="resetTemplates">恢复默认</el-button>
            <el-button type="primary" @click="saveTemplates">保存模板</el-button>
          </div>
        </div>
        <el-form v-if="editTemplates" label-width="140px" class="tpl-form">
          <el-form-item v-for="key in templateKeys" :key="key" :label="templateLabel(key)">
            <div class="tpl-item">
              <el-input
                v-model="editTemplates[key]"
                type="textarea"
                :rows="key.endsWith('-content') ? 2 : 1"
              />
              <el-button link type="primary" @click="previewTemplate(key)">预览</el-button>
            </div>
          </el-form-item>
        </el-form>
      </el-card>
    </template>

    <el-dialog v-model="previewVisible" title="模板预览" width="480px">
      <p><strong>标题：</strong>{{ previewResult?.title }}</p>
      <p><strong>内容：</strong>{{ previewResult?.content }}</p>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../stores/user'
import PageHeader from '../components/PageHeader.vue'
import {
  changePassword, testEmail, testSms, updateProfile, getSystemInfo, getPublicHealth,
  getNotificationTemplates, updateNotificationTemplates, resetNotificationTemplates, previewNotificationTemplate
} from '../api'

const userStore = useUserStore()
const profile = reactive({ realName: '', email: '', phone: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const mail = reactive({ to: '', subject: '测试邮件', content: '外审专家管理系统邮件测试' })
const sms = reactive({ phone: '', content: '外审专家管理系统短信测试' })

const sysInfo = ref(null)
const infoLoading = ref(false)
const tplLoading = ref(false)
const placeholders = ref(null)
const editTemplates = ref(null)
const templateKeys = ref([])
const previewVisible = ref(false)
const previewResult = ref(null)

const templateNameMap = {
  'invite-title': '邀请-标题',
  'invite-content': '邀请-内容',
  'deadline-title': '截止-标题',
  'deadline-content': '截止-内容',
  'overdue-title': '逾期-标题',
  'overdue-content': '逾期-内容',
  'return-title': '退回-标题',
  'return-content': '退回-内容',
  'applicant-progress-title': '申报进度-标题',
  'applicant-progress-content': '申报进度-内容'
}

const placeholderText = computed(() => {
  if (!placeholders.value) return ''
  return Object.entries(placeholders.value).map(([k, v]) => `{${k}}=${v}`).join('，')
})

const mailStatusText = computed(() => {
  if (!sysInfo.value) return '-'
  const s = sysInfo.value
  if (!s.mailEnabled) return '未启用（模拟/日志）'
  return s.mailConfigured ? `SMTP 已配置（${s.mailMode}）` : `已启用但未配置 SMTP（${s.mailMode}）`
})

const smsStatusText = computed(() => {
  if (!sysInfo.value) return '-'
  const s = sysInfo.value
  if (!s.smsEnabled) return '未启用（模拟/日志）'
  const name = s.smsProvider === 'aliyun' ? '阿里云' : s.smsProvider === 'tencent' ? '腾讯云' : s.smsProvider
  return s.smsConfigured ? `${name} 已配置` : `${name} 已启用但未配置密钥/模板`
})

function templateLabel(key) {
  return templateNameMap[key] || key
}

async function saveProfile() {
  const res = await updateProfile(profile)
  userStore.user = { ...userStore.user, ...res.data }
  localStorage.setItem('user', JSON.stringify(userStore.user))
  ElMessage.success('资料已保存')
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写密码')
  if (pwd.newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  if (pwd.newPassword !== pwd.confirmPassword) return ElMessage.warning('两次输入的新密码不一致')
  await changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改')
  pwd.oldPassword = ''
  pwd.newPassword = ''
  pwd.confirmPassword = ''
}

async function testMail() {
  if (!mail.to) return ElMessage.warning('请填写收件人')
  const res = await testEmail(mail)
  const r = res.data
  if (r?.success) ElMessage.success(r.message || '已触发发送')
  else ElMessage.error(r?.message || '发送失败')
}

async function testSmsSend() {
  if (!sms.phone) return ElMessage.warning('请填写手机号')
  const res = await testSms(sms)
  const r = res.data
  if (r?.success) ElMessage.success(r.message || '已触发发送')
  else ElMessage.error(r?.message || '发送失败')
}

async function loadSystemInfo() {
  if (!userStore.isSystemAdmin) return
  infoLoading.value = true
  try {
    const res = await getSystemInfo()
    sysInfo.value = res.data
  } finally {
    infoLoading.value = false
  }
}

async function loadTemplates() {
  if (!userStore.isSystemAdmin) return
  tplLoading.value = true
  try {
    const res = await getNotificationTemplates()
    placeholders.value = res.data.placeholders
    editTemplates.value = { ...res.data.templates }
    templateKeys.value = Object.keys(res.data.templates || {})
  } finally {
    tplLoading.value = false
  }
}

async function saveTemplates() {
  await updateNotificationTemplates({ templates: editTemplates.value })
  ElMessage.success('通知模板已保存')
  loadTemplates()
}

async function resetTemplates() {
  await ElMessageBox.confirm('确认恢复为 application.yml 默认模板？', '恢复默认')
  await resetNotificationTemplates()
  ElMessage.success('已恢复默认模板')
  loadTemplates()
}

async function previewTemplate(key) {
  const res = await previewNotificationTemplate({ templateKey: key })
  previewResult.value = res.data
  previewVisible.value = true
}

async function openHealthDetail() {
  try {
    const res = await getPublicHealth()
    const d = res.data
    const componentLines = d.components
      ? Object.entries(d.components).map(([name, info]) => {
          const status = typeof info === 'object' ? info.status : info
          const mode = typeof info === 'object' && info.mode ? ` (${info.mode})` : ''
          return `${name}: ${status}${mode}`
        }).join('\n')
      : '无'
    await ElMessageBox.alert(
      `状态：${d.status}\n应用：${d.app}\n版本：${d.version}\n阶段：Phase ${d.phase}\n时间：${d.timestamp}\n\n组件检查：\n${componentLines}`,
      '服务健康检查',
      { confirmButtonText: '知道了' }
    )
  } catch {
    ElMessage.error('无法连接后端，请确认 backend 已在 8080 端口启动')
  }
}

onMounted(async () => {
  await userStore.fetchMe()
  profile.realName = userStore.user?.realName || ''
  profile.email = userStore.user?.email || ''
  profile.phone = userStore.user?.phone || ''
  loadSystemInfo()
  loadTemplates()
})
</script>

<style scoped>
.settings-row {
  margin-bottom: 16px;
}

.settings-row:last-child {
  margin-bottom: 0;
}

.settings-card {
  height: 100%;
}

.settings-card :deep(.el-card__body) {
  padding-top: 16px;
}

.settings-form :deep(.el-input),
.settings-form :deep(.el-textarea) {
  width: 100%;
}

.form-actions {
  margin-bottom: 0;
}

.form-actions :deep(.el-form-item__content) {
  justify-content: flex-start;
}

.hint {
  font-size: 12px;
  color: #909399;
  margin: 0 0 0 88px;
  line-height: 1.5;
}

.sys-desc :deep(.el-descriptions__label),
.dev-desc :deep(.el-descriptions__label) {
  width: 88px;
}

.dev-health {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.tpl-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.tpl-hint {
  flex: 1;
  min-width: 280px;
  margin: 0;
}

.tpl-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.tpl-form :deep(.el-textarea) {
  width: 100%;
}

.tpl-item {
  width: 100%;
}

.tpl-item .el-button {
  margin-top: 4px;
  padding-left: 0;
}

@media (max-width: 992px) {
  .settings-row .el-col {
    margin-bottom: 16px;
  }

  .settings-row .el-col:last-child {
    margin-bottom: 0;
  }

  .hint {
    margin-left: 0;
  }
}
</style>
