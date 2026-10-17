export const taskStatusMap = {
  UNASSIGNED: '待分配',
  PUBLISHED: '已发布',
  PENDING_CONFIRM: '待确认',
  IN_REVIEW: '评审中',
  SUMMARIZED: '已汇总',
  ARCHIVED: '已归档'
}

export const expertStatusMap = {
  ACTIVE: '正常',
  DISABLED: '已停用',
  BLACKLIST: '黑名单',
  PENDING: '待审核'
}

export const assignmentStatusMap = {
  PENDING: '待确认',
  ACCEPTED: '已接受',
  REJECTED: '已拒绝',
  SUBMITTED: '已提交',
  OVERDUE: '已逾期'
}

export const notificationTypeMap = {
  INVITE: '评审邀请',
  DEADLINE: '截止提醒',
  OVERDUE: '逾期提醒',
  SYSTEM: '系统通知',
  APPLICANT_PROGRESS: '申报进度',
  REMIND: '催办提醒',
  RECTIFICATION: '整改通知',
  RE_REVIEW: '复审通知'
}

export const rectificationStatusMap = {
  NONE: '无',
  REQUESTED: '待整改',
  SUBMITTED: '已提交整改',
  RE_REVIEWING: '复审中'
}

export const roleNameMap = {
  ADMIN: '系统管理员',
  REVIEW_ADMIN: '评审管理员',
  EXPERT: '外部专家',
  LEADER: '研究院领导',
  APPLICANT: '项目申报人'
}

export function roleNameLabel(code, dbName) {
  if (dbName && !dbName.includes('?')) return dbName
  return roleNameMap[code] || code
}

export function taskStatusLabel(s) {
  return taskStatusMap[s] || s || '-'
}

export function assignmentStatusLabel(s) {
  return assignmentStatusMap[s] || s || '-'
}

export function notificationTypeLabel(s) {
  return notificationTypeMap[s] || s || '-'
}

export function rectificationStatusLabel(s) {
  return rectificationStatusMap[s] || s || '-'
}

export function rectificationStatusTag(s) {
  return {
    REQUESTED: 'danger',
    SUBMITTED: 'warning',
    RE_REVIEWING: 'primary'
  }[s] || 'info'
}

export function expertStatusLabel(s) {
  return expertStatusMap[s] || s || '-'
}

/** 将统计接口返回的 { 状态码: 数量 } 转为图表用的中文键名 */
export function mapStatLabels(stats, labelFn) {
  if (!stats || typeof stats !== 'object') return {}
  return Object.fromEntries(
    Object.entries(stats).map(([key, value]) => [labelFn(key), value])
  )
}

export function expertStatusTag(s) {
  return { ACTIVE: 'success', DISABLED: 'info', BLACKLIST: 'danger', PENDING: 'warning' }[s] || 'info'
}

export function taskStatusTag(s) {
  if (s === 'UNASSIGNED') return 'primary'
  if (s === 'IN_REVIEW') return 'warning'
  if (s === 'SUMMARIZED') return 'success'
  if (s === 'ARCHIVED') return 'info'
  if (s === 'PUBLISHED') return 'success'
  if (s === 'PENDING_CONFIRM') return 'warning'
  if (s === 'OVERDUE') return 'danger'
  return ''
}

export function assignmentStatusTag(s) {
  return { PENDING: 'warning', ACCEPTED: 'primary', SUBMITTED: 'success', REJECTED: 'info', OVERDUE: 'danger' }[s] || 'info'
}
