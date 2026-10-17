import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getMe } from '../api'

export const useUserStore = defineStore('user', () => {
  function loadStoredUser() {
    try {
      const raw = localStorage.getItem('user')
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  const token = ref(localStorage.getItem('token') || '')
  const user = ref(loadStoredUser())

  const isExpert = computed(() => user.value?.roles?.includes('EXPERT'))
  const isFullAdmin = computed(() =>
    user.value?.roles?.some(r => ['ADMIN', 'REVIEW_ADMIN'].includes(r))
  )
  const isLeader = computed(() => user.value?.roles?.includes('LEADER'))
  const isLeaderOnly = computed(() => isLeader.value && !isFullAdmin.value)
  const isSystemAdmin = computed(() => user.value?.roles?.includes('ADMIN'))
  const isApplicant = computed(() => user.value?.roles?.includes('APPLICANT'))
  const isApplicantOnly = computed(() =>
    isApplicant.value && !isFullAdmin.value && !isLeader.value && !isExpert.value
  )
  /** 管理端菜单（不含纯领导） */
  const isAdmin = computed(() => isFullAdmin.value)

  async function login(form) {
    const res = await loginApi(form)
    token.value = res.data.token
    user.value = res.data
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(res.data))
  }

  async function loginWithToken(jwt) {
    token.value = jwt
    localStorage.setItem('token', jwt)
    user.value = null
    await fetchMe()
    user.value = { ...user.value, token: jwt }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function fetchMe() {
    const res = await getMe()
    user.value = { ...user.value, ...res.data }
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function homePath() {
    if (isApplicantOnly.value) return '/my-projects'
    if (isExpert.value && !isFullAdmin.value && !isLeader.value) return '/expert/workbench'
    if (isLeaderOnly.value) return '/leader/tasks'
    if (isFullAdmin.value) return '/dashboard'
    return '/settings'
  }

  return { token, user, isExpert, isFullAdmin, isLeader, isLeaderOnly, isSystemAdmin, isApplicant, isApplicantOnly, isAdmin, login, loginWithToken, fetchMe, logout, homePath }
})
