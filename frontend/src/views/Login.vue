<template>
  <div class="login-page">
    <div class="login-mesh" aria-hidden="true" />
    <div class="login-orb login-orb--1" aria-hidden="true" />
    <div class="login-orb login-orb--2" aria-hidden="true" />

    <div class="login-panel">
      <section class="login-brand">
        <div class="brand-icon">
          <AppIcon name="layout-dashboard" :size="32" :stroke-width="1.5" />
        </div>
        <h1>专家评审系统</h1>
        <p class="brand-tagline">可追踪 · 可审计的在线评审平台</p>
        <ul class="brand-pills">
          <li><AppIcon name="users" :size="14" />专家库</li>
          <li><AppIcon name="clipboard-pen" :size="14" />盲审</li>
          <li><AppIcon name="file-text" :size="14" />汇总</li>
        </ul>
      </section>

      <el-card class="login-card" shadow="never">
        <h2>登录</h2>
        <p class="login-sub">使用账号密码或统一认证进入</p>
        <el-form :model="form" @submit.prevent="handleLogin" size="large">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名">
              <template #prefix>
                <AppIcon name="user" :size="16" class="input-prefix-icon" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" show-password>
              <template #prefix>
                <AppIcon name="lock" :size="16" class="input-prefix-icon" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <div class="captcha-row">
              <el-input v-model="form.captchaCode" placeholder="验证码" maxlength="4">
                <template #prefix>
                  <AppIcon name="shield" :size="16" class="input-prefix-icon" />
                </template>
              </el-input>
              <img
                v-if="captchaImage"
                class="captcha-image"
                :src="captchaImage"
                alt="验证码"
                title="点击图片换一张"
                @click="loadCaptcha"
              />
              <button v-else type="button" class="captcha-placeholder" @click="loadCaptcha">获取验证码</button>
            </div>
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="login-btn">进入系统</el-button>
        </el-form>

        <div v-if="ssoProviders.length" class="sso-section">
          <div class="sso-divider"><span>统一认证</span></div>
          <el-button
            v-for="p in ssoProviders"
            :key="p.id"
            class="sso-btn"
            plain
            :loading="ssoLoading === p.id"
            @click="handleSso(p)"
          >
            {{ p.name }}
          </el-button>
        </div>

        <details class="demo-details">
          <summary>演示账号（点击展开）</summary>
          <div class="demo-cards">
            <button
              v-for="acc in demoAccounts"
              :key="acc.username"
              type="button"
              class="demo-card"
              @click="fillDemo(acc)"
            >
              <span class="demo-role">{{ acc.role }}</span>
              <span class="demo-user">{{ acc.username }}</span>
            </button>
          </div>
        </details>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { getSsoProviders, ssoMockLogin, getSsoOAuth2Authorize, getCaptcha } from '../api'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const ssoLoading = ref('')
const ssoProviders = ref([])
const captchaImage = ref('')
const form = reactive({ username: 'admin', password: 'admin123', captchaId: '', captchaCode: '' })

const demoAccounts = [
  { role: '管理员', username: 'admin', password: 'admin123' },
  { role: '领导', username: 'leader', password: 'leader123' },
  { role: '申报人', username: 'applicant', password: 'applicant123' },
  { role: '专家（库内首位）', username: 'expert1', password: 'expert123' }
]

function fillDemo(acc) {
  form.username = acc.username
  form.password = acc.password
}

async function loadSsoProviders() {
  try {
    const res = await getSsoProviders()
    ssoProviders.value = res.data || []
  } catch {
    ssoProviders.value = []
  }
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    form.captchaId = res.data.captchaId
    captchaImage.value = `/api/auth/captcha/${res.data.captchaId}/image?t=${Date.now()}`
    form.captchaCode = ''
  } catch {
    captchaImage.value = ''
    ElMessage.error('验证码加载失败')
  }
}

async function handleLogin() {
  if (!form.captchaId || !form.captchaCode) {
    return ElMessage.warning('请完成验证码')
  }
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(userStore.homePath())
  } catch {
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

async function handleSso(provider) {
  ssoLoading.value = provider.id
  try {
    if (provider.type === 'mock') {
      const res = await ssoMockLogin({ username: form.username || 'admin' })
      await userStore.loginWithToken(res.data.token)
      ElMessage.success('SSO 登录成功')
      router.push(userStore.homePath())
      return
    }
    if (provider.type === 'oauth2') {
      const res = await getSsoOAuth2Authorize()
      if (res.data?.authorizeUrl) {
        window.location.href = res.data.authorizeUrl
      }
    }
  } finally {
    ssoLoading.value = ''
  }
}

onMounted(() => {
  loadSsoProviders()
  loadCaptcha()
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  position: relative;
  overflow: hidden;
  background: #12132a;
}

.login-mesh {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 70% 60% at 20% 20%, rgba(92, 108, 255, 0.45), transparent 50%),
    radial-gradient(ellipse 50% 50% at 85% 70%, rgba(34, 211, 238, 0.28), transparent 45%),
    radial-gradient(ellipse 40% 35% at 60% 10%, rgba(129, 140, 248, 0.2), transparent 40%),
    linear-gradient(160deg, #12132a 0%, #1a1d3d 50%, #252850 100%);
}

.login-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
  opacity: 0.7;
}

.login-orb--1 {
  width: 320px;
  height: 320px;
  top: -80px;
  right: 10%;
  background: rgba(92, 108, 255, 0.35);
}

.login-orb--2 {
  width: 240px;
  height: 240px;
  bottom: -40px;
  left: 5%;
  background: rgba(34, 211, 238, 0.25);
}

.login-panel {
  display: flex;
  align-items: center;
  gap: 64px;
  max-width: 960px;
  width: 100%;
  position: relative;
  z-index: 1;
}

.login-brand {
  flex: 1;
  color: #fff;
  min-width: 0;
}

.brand-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--app-primary), var(--app-accent));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 28px;
  box-shadow: 0 12px 32px rgba(92, 108, 255, 0.4);
}

.login-brand h1 {
  margin: 0 0 12px;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.15;
}

.brand-tagline {
  margin: 0 0 32px;
  font-size: 16px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.72);
  max-width: 340px;
}

.brand-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.brand-pills li {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--app-radius-full);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.88);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
}

.login-card {
  width: 400px;
  flex-shrink: 0;
  border-radius: var(--app-radius-lg) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
}

.login-card :deep(.el-card__body) {
  padding: 40px 36px 32px;
}

.login-card h2 {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 600;
  letter-spacing: -0.03em;
  color: var(--app-text);
}

.login-sub {
  margin: 0 0 28px;
  font-size: 14px;
  color: var(--app-text-secondary);
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--app-radius-sm);
  margin-top: 4px;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-image,
.captcha-placeholder {
  flex-shrink: 0;
  width: 112px;
  height: 40px;
  border-radius: var(--app-radius-sm);
  cursor: pointer;
  border: 1px solid var(--app-border);
}

.captcha-image {
  object-fit: cover;
}

.captcha-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--app-primary);
  background: var(--app-primary-muted);
  border: none;
  font-family: inherit;
}

.captcha-row .el-input {
  flex: 1;
  min-width: 0;
}

.sso-section {
  margin-top: 20px;
}

.sso-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  color: var(--app-text-muted);
  font-size: 12px;
}

.sso-divider::before,
.sso-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--app-border);
}

.sso-btn {
  width: 100%;
  margin-bottom: 8px;
  border-radius: var(--app-radius-sm);
}

.demo-details {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--app-border);
}

.demo-details summary {
  font-size: 13px;
  color: var(--app-text-secondary);
  cursor: pointer;
  user-select: none;
  list-style: none;
}

.demo-details summary::-webkit-details-marker {
  display: none;
}

.demo-details[open] summary {
  margin-bottom: 12px;
  color: var(--app-primary);
}

.demo-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.demo-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 12px 14px;
  border-radius: var(--app-radius-sm);
  border: 1px solid var(--app-border);
  background: #f9fafc;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  font-family: inherit;
  text-align: left;
}

.demo-card:hover {
  border-color: var(--app-primary-light);
  background: var(--app-primary-muted);
}

.demo-role {
  font-size: 11px;
  font-weight: 600;
  color: var(--app-primary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.demo-user {
  font-size: 14px;
  font-weight: 500;
  color: var(--app-text);
}

@media (max-width: 900px) {
  .login-panel {
    flex-direction: column;
    gap: 32px;
    align-items: stretch;
  }

  .login-brand {
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .brand-tagline {
    max-width: none;
  }

  .brand-pills {
    justify-content: center;
  }

  .login-card {
    width: 100%;
    max-width: 420px;
    margin: 0 auto;
  }
}
</style>
