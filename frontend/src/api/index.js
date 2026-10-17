import request from './request'
import { useUserStore } from '../stores/user'

async function readFetchError(res, fallback) {
  try {
    const data = await res.clone().json()
    if (data?.message) return data.message
  } catch {
    // ignore non-JSON error bodies
  }
  return fallback
}

export async function downloadFile(url, filename) {
  const userStore = useUserStore()
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${userStore.token}` }
  })
  if (!res.ok) throw new Error(await readFetchError(res, '下载失败'))
  const blob = await res.blob()
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(objectUrl)
}

export async function previewFile(url, mimeType = 'application/pdf') {
  const userStore = useUserStore()
  const tab = window.open('', '_blank')
  if (!tab) {
    throw new Error('浏览器拦截了弹出窗口，请允许弹窗或使用下载 PDF')
  }
  try {
    const res = await fetch(url, {
      headers: { Authorization: `Bearer ${userStore.token}` }
    })
    if (!res.ok) {
      throw new Error(await readFetchError(res, '预览失败'))
    }
    const blob = new Blob([await res.arrayBuffer()], {
      type: mimeType || res.headers.get('content-type') || 'application/octet-stream'
    })
    tab.location.href = URL.createObjectURL(blob)
  } catch (err) {
    tab.close()
    throw err
  }
}

export const login = (data) => request.post('/api/auth/login', data)
export const getSsoProviders = () => request.get('/api/auth/sso/providers')
export const ssoMockLogin = (data) => request.post('/api/auth/sso/mock', data)
export const getSsoOAuth2Authorize = () => request.get('/api/auth/sso/oauth2/authorize')
export const getMe = () => request.get('/api/auth/me')

export const getExperts = (params) => request.get('/api/experts', { params })
export const getExpert = (id) => request.get(`/api/experts/${id}`)
export const createExpert = (data) => request.post('/api/experts', data)
export const updateExpert = (id, data) => request.put(`/api/experts/${id}`, data)
export const deleteExpert = (id) => request.delete(`/api/experts/${id}`)
export const getPendingExperts = (params) => request.get('/api/experts/pending', { params })
export const approveExpert = (id, data) => request.post(`/api/experts/${id}/approve`, data || {})
export const rejectExpert = (id, data) => request.post(`/api/experts/${id}/reject`, data || {})
export const getExpertMaterials = (expertId) => request.get(`/api/experts/${expertId}/materials`)
export const uploadExpertMaterial = (expertId, file, materialType) => {
  const form = new FormData()
  form.append('file', file)
  if (materialType) form.append('materialType', materialType)
  return request.post(`/api/experts/${expertId}/materials`, form)
}
export const deleteExpertMaterial = (expertId, materialId) =>
  request.delete(`/api/experts/${expertId}/materials/${materialId}`)
export const getExpertAvoidances = (expertId) => request.get(`/api/experts/${expertId}/avoidances`)
export const addExpertAvoidance = (expertId, data) => request.post(`/api/experts/${expertId}/avoidances`, data)
export const deleteExpertAvoidance = (expertId, id) => request.delete(`/api/experts/${expertId}/avoidances/${id}`)
export const createExpertAccount = (id, data) => request.post(`/api/experts/${id}/account`, data)
export const importExperts = (file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post('/api/experts/import', form)
}

export const getTasks = (params) => request.get('/api/tasks', { params })
export const getTask = (id) => request.get(`/api/tasks/${id}`)
export const createTask = (data) => request.post('/api/tasks', data)
export const addProject = (taskId, data) => request.post(`/api/tasks/${taskId}/projects`, data)
export const assignExperts = (taskId, data) => request.post(`/api/tasks/${taskId}/assign`, data)
export const randomAssignExperts = (taskId, data) => request.post(`/api/tasks/${taskId}/random-assign`, data)
export const batchRandomAssignExperts = (taskId, data) => request.post(`/api/tasks/${taskId}/batch-random-assign`, data)
export const updateTaskDeadline = (taskId, data) => request.put(`/api/tasks/${taskId}/deadline`, data)
export const updateTaskFormConfig = (taskId, data) => request.put(`/api/tasks/${taskId}/form-config`, data)
export const remindPendingExperts = (taskId, data) => request.post(`/api/tasks/${taskId}/remind-pending`, data || {})
export const uploadMaterial = (taskId, file, projectId, materialType) => {
  const form = new FormData()
  form.append('file', file)
  if (projectId) form.append('projectId', projectId)
  if (materialType) form.append('materialType', materialType)
  return request.post(`/api/tasks/${taskId}/materials`, form)
}
export const summarizeTask = (taskId) => request.post(`/api/tasks/${taskId}/summarize`)
export const archiveTask = (taskId) => request.post(`/api/tasks/${taskId}/archive`)
export const publishTask = (taskId) => request.post(`/api/tasks/${taskId}/publish`)
export const cloneTask = (taskId, data) => request.post(`/api/tasks/${taskId}/clone`, data)
export const importTaskProjects = (taskId, file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post(`/api/tasks/${taskId}/projects/import`, form)
}

export const getMyReviews = (params) => request.get('/api/reviews/my', { params })
export const getReviewDetail = (id) => request.get(`/api/reviews/assignments/${id}`)
export const respondInvite = (data) => request.post('/api/reviews/respond', data)
export const submitReview = (data) => request.post('/api/reviews/submit', data)
export const returnReview = (id) => request.post(`/api/reviews/assignments/${id}/return`)

export const getNotifications = (params) => request.get('/api/notifications', { params })
export const getUnreadCount = () => request.get('/api/notifications/unread-count')
export const markNotificationRead = (id) => request.post(`/api/notifications/${id}/read`)
export const markAllNotificationsRead = () => request.post('/api/notifications/read-all')

export const getAnalytics = () => request.get('/api/analytics/overview')

export const getUsers = (params) => request.get('/api/users', { params })
export const getRoles = () => request.get('/api/users/roles')
export const createUser = (data) => request.post('/api/users', data)
export const updateUser = (id, data) => request.put(`/api/users/${id}`, data)
export const updateProfile = (data) => request.put('/api/auth/profile', data)
export const changePassword = (data) => request.post('/api/auth/change-password', data)
export const getSystemInfo = () => request.get('/api/system/info')
export const getPublicHealth = () => request.get('/api/public/health')
export const getNotificationTemplates = () => request.get('/api/system/notification-templates')
export const updateNotificationTemplates = (data) => request.put('/api/system/notification-templates', data)
export const resetNotificationTemplates = () => request.post('/api/system/notification-templates/reset')
export const previewNotificationTemplate = (data) => request.post('/api/system/notification-templates/preview', data)
export const testEmail = (data) => request.post('/api/system/test-email', data)
export const testSms = (data) => request.post('/api/system/test-sms', data)

export const getApplicantProjects = () => request.get('/api/applicant/projects')
export const getApplicantProject = (projectId) => request.get(`/api/applicant/projects/${projectId}`)
export const getApplicantResult = (projectId) => request.get(`/api/applicant/projects/${projectId}/result`)
export const uploadApplicantMaterial = (taskId, projectId, file) => {
  const form = new FormData()
  form.append('file', file)
  form.append('projectId', projectId)
  return request.post(`/api/applicant/tasks/${taskId}/materials`, form)
}

export const getApplicantUsers = () => request.get('/api/users/applicants')
export const bindProjectApplicant = (taskId, projectId, applicantUserId) =>
  request.put(`/api/tasks/${taskId}/projects/${projectId}/applicant`, { applicantUserId })

export const requestRectification = (taskId, projectId, data) =>
  request.post(`/api/tasks/${taskId}/projects/${projectId}/rectification/request`, data)
export const cancelRectification = (taskId, projectId) =>
  request.post(`/api/tasks/${taskId}/projects/${projectId}/rectification/cancel`)
export const getRectification = (taskId, projectId) =>
  request.get(`/api/tasks/${taskId}/projects/${projectId}/rectification`)
export const startReReview = (taskId, projectId) =>
  request.post(`/api/tasks/${taskId}/projects/${projectId}/rectification/start-rereview`)
export const submitRectification = (projectId, reply) => {
  const form = new FormData()
  if (reply) form.append('reply', reply)
  return request.post(`/api/applicant/projects/${projectId}/rectification/submit`, form)
}
export const uploadRectificationMaterial = (projectId, file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post(`/api/applicant/projects/${projectId}/rectification/materials`, form)
}
export const downloadRectificationMaterial = (taskId, projectId, materialId, filename, asApplicant = false) => {
  const base = asApplicant
    ? `/api/applicant/projects/${projectId}/rectification/materials/${materialId}/download`
    : `/api/tasks/${taskId}/projects/${projectId}/rectification/materials/${materialId}/download`
  return downloadFile(base, filename)
}

export const getExpertCredit = (expertId) => request.get(`/api/credit/experts/${expertId}`)
export const rateSubmissionQuality = (data) => request.post('/api/credit/rate', data)

export const getDashboard = () => request.get('/api/dashboard/overview')
export const getTaskProgress = () => request.get('/api/dashboard/tasks')
export const getSummary = (taskId) => request.get(`/api/summary/tasks/${taskId}`)
export const getOpinionTemplates = () => request.get('/api/reviews/opinion-templates')
export const saveOpinionTemplates = (data) => request.put('/api/reviews/opinion-templates', data)
export const getRoundComparison = (taskId) => request.get(`/api/summary/tasks/${taskId}/round-comparison`)
export const getSummaryDimensions = (taskId) => request.get(`/api/summary/tasks/${taskId}/dimensions`)
export const getAuditLogs = (params) => request.get('/api/audit', { params })

export const getReviewTypes = () => request.get('/api/review-types')
export const getReviewTypeConfig = (type) => request.get(`/api/review-types/${encodeURIComponent(type)}/config`)
export const getCaptcha = () => request.get('/api/auth/captcha')
export const downloadReviewMaterial = (id, filename) =>
  downloadFile(`/api/files/review-materials/${id}`, filename)
export const downloadExpertMaterial = (expertId, materialId, filename) =>
  downloadFile(`/api/files/expert-materials/${materialId}`, filename)
export const downloadSubmissionAttachment = (submissionId, filename) =>
  downloadFile(`/api/files/submission-attachments/${submissionId}`, filename)
export const getTaskAnomalies = (taskId) => request.get(`/api/review-types/tasks/${taskId}/anomalies`)
export const getGlobalAnomalies = () => request.get('/api/review-types/anomalies')
export const uploadReviewAttachment = (assignmentId, file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post(`/api/reviews/assignments/${assignmentId}/attachment`, form)
}
