<template>
  <div class="airy-page">
    <PageHeader title="用户管理" subtitle="管理系统账号、角色与数据域">
      <template #extra>
        <el-input v-model="keyword" placeholder="搜索用户名/姓名/邮箱" clearable style="width:220px" @keyup.enter="load" @clear="load" />
        <el-button @click="load">查询</el-button>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </template>
    </PageHeader>

    <el-card class="content-card" shadow="never">
      <div v-if="list.length || loading" class="table-wrap">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="140" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机" width="110" />
        <el-table-column prop="dataDomain" label="数据域" width="90" />
        <el-table-column label="角色" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="code in row.roles || []" :key="code" size="small" style="margin-right:4px">{{ roleName(code) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>
      <EmptyGuide
        v-else-if="!loading"
        description="暂无用户，点击右上角新增第一个账号"
        action-text="新增用户"
        icon="user-cog"
        @action="openCreate"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" class="airy-dialog" :title="form.id ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item :label="form.id ? '新密码' : '密码'">
          <el-input v-model="form.password" type="password" placeholder="留空则不修改，至少6位" show-password />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="数据域">
          <el-input v-model="form.dataDomain" placeholder="REVIEW_ADMIN 可见范围，默认 DEFAULT" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roles" multiple style="width:100%">
            <el-option
              v-for="r in roleOptions"
              :key="r.code"
              :label="roleName(r.code, r.name)"
              :value="r.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.id" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { roleNameLabel } from '../utils/labels'
import { getUsers, getRoles, createUser, updateUser } from '../api'
import PageHeader from '../components/PageHeader.vue'
import EmptyGuide from '../components/EmptyGuide.vue'

const loading = ref(false)
const keyword = ref('')
const list = ref([])
const roleOptions = ref([])
const dialogVisible = ref(false)
const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  dataDomain: 'DEFAULT',
  roles: [],
  status: 1
})

function roleName(code, dbName) {
  return roleNameLabel(code, dbName)
}

function resetForm() {
  form.id = null
  form.username = ''
  form.password = ''
  form.realName = ''
  form.email = ''
  form.phone = ''
  form.dataDomain = 'DEFAULT'
  form.roles = []
  form.status = 1
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  resetForm()
  form.id = row.id
  form.username = row.username
  form.realName = row.realName || ''
  form.email = row.email || ''
  form.phone = row.phone || ''
  form.dataDomain = row.dataDomain || 'DEFAULT'
  form.roles = [...(row.roles || [])]
  form.status = row.status ?? 1
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (keyword.value) params.keyword = keyword.value
    const res = await getUsers(params)
    list.value = res.data
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  const res = await getRoles()
  roleOptions.value = res.data || []
}

async function save() {
  if (!form.username) return ElMessage.warning('请填写用户名')
  const payload = {
    realName: form.realName,
    email: form.email,
    phone: form.phone,
    dataDomain: form.dataDomain,
    roles: form.roles
  }
  if (form.id) {
    if (form.password) payload.newPassword = form.password
    payload.status = form.status
    await updateUser(form.id, payload)
  } else {
    if (!form.password) return ElMessage.warning('请设置密码')
    await createUser({ username: form.username, password: form.password, ...payload })
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

onMounted(() => {
  loadRoles()
  load()
})
</script>
