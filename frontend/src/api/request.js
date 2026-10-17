import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import router from '../router'

const request = axios.create({ baseURL: '', timeout: 60000 })

const HTTP_MESSAGES = {
  401: '未登录或登录已过期，请重新登录',
  403: '无权访问该资源',
  404: '请求的资源不存在',
  429: '操作过于频繁，请稍后再试',
  500: '服务器异常，请稍后重试'
}

function messageForError(err) {
  const status = err.response?.status
  const bodyMsg = err.response?.data?.message
  if (bodyMsg) return bodyMsg
  if (status && HTTP_MESSAGES[status]) return HTTP_MESSAGES[status]
  if (err.message && !err.message.startsWith('Request failed')) return err.message
  return '网络错误，请检查连接'
}

function handleAuthFailure(status) {
  if (status !== 401 && status !== 403) return
  const userStore = useUserStore()
  if (!userStore.token) return
  userStore.logout()
  const msg = status === 401 ? HTTP_MESSAGES[401] : '登录状态无效或权限不足，请重新登录'
  router.push('/login').then(() => ElMessage.warning(msg))
}

request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  err => {
    const status = err.response?.status
    handleAuthFailure(status)
    if (status !== 401 && status !== 403) {
      ElMessage.error(messageForError(err))
    }
    return Promise.reject(err)
  }
)

export default request
