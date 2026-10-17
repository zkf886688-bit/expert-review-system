<template>
  <div class="sso-callback">
    <el-result icon="success" title="SSO 登录成功" sub-title="正在进入系统…" v-if="!error" />
    <el-result icon="error" title="SSO 登录失败" :sub-title="error" v-else>
      <template #extra>
        <el-button type="primary" @click="$router.replace('/login')">返回登录</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const error = ref('')

onMounted(async () => {
  const token = route.query.token
  if (!token) {
    error.value = '未收到登录令牌'
    return
  }
  try {
    await userStore.loginWithToken(token)
    ElMessage.success('登录成功')
    router.replace(userStore.homePath())
  } catch (e) {
    error.value = e.message || '令牌无效'
  }
})
</script>

<style scoped>
.sso-callback {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-bg, #f5f7fa);
}
</style>
