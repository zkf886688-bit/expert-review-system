/** 解析后端日期时间字符串 */
export function parseDeadline(value) {
  if (!value) return null
  const normalized = String(value).replace(' ', 'T')
  const d = new Date(normalized)
  return Number.isNaN(d.getTime()) ? null : d
}

function padTimeUnit(value) {
  return String(value).padStart(2, '0')
}

export function formatDeadline(value) {
  const d = parseDeadline(value)
  if (!d) return value || '-'
  const year = d.getFullYear()
  const month = padTimeUnit(d.getMonth() + 1)
  const day = padTimeUnit(d.getDate())
  const hour = padTimeUnit(d.getHours())
  const minute = padTimeUnit(d.getMinutes())
  const second = padTimeUnit(d.getSeconds())
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

/** @returns {'none'|'normal'|'soon'|'overdue'} */
export function deadlineUrgency(deadline, now = new Date()) {
  const d = parseDeadline(deadline)
  if (!d) return 'none'
  const diffDays = (d - now) / 86400000
  if (diffDays < 0) return 'overdue'
  if (diffDays <= 3) return 'soon'
  return 'normal'
}

export function deadlineUrgencyLabel(urgency) {
  return { overdue: '已逾期', soon: '临近截止', normal: '', none: '' }[urgency] || ''
}

export function deadlineUrgencyTagType(urgency) {
  return { overdue: 'danger', soon: 'warning', normal: 'info', none: 'info' }[urgency] || 'info'
}

export function deadlineRowClassName(deadline, options = {}) {
  const { skipWhen } = options
  if (typeof skipWhen === 'function' && skipWhen()) return ''
  const urgency = deadlineUrgency(deadline)
  if (urgency === 'overdue') return 'row-deadline-overdue'
  if (urgency === 'soon') return 'row-deadline-soon'
  return ''
}
