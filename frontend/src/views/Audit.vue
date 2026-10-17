<template>
  <div class="airy-page">
    <PageHeader title="审计日志" subtitle="记录关键操作与登录行为">
      <template #extra>
        <el-input v-model="keyword" placeholder="操作人/详情" clearable style="width:160px" @keyup.enter="load" @clear="load" />
        <el-select v-model="actionFilter" placeholder="操作类型" clearable style="width:150px" @change="load">
          <el-option v-for="a in actionOptions" :key="a" :label="a" :value="a" />
        </el-select>
        <el-button @click="load">查询</el-button>
        <el-button @click="exportLogs">导出 Excel</el-button>
      </template>
    </PageHeader>

    <el-card class="content-card" shadow="never">
      <div v-if="list.length || loading" class="table-wrap">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="action" label="操作" width="140" />
        <el-table-column prop="targetType" label="对象类型" width="100" />
        <el-table-column prop="targetId" label="对象ID" width="80" />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="120" />
      </el-table>
      </div>
      <EmptyGuide
        v-else-if="!loading"
        description="暂无审计记录，系统操作将自动记录在此"
        icon="scroll-text"
      />
      <el-pagination v-if="total" style="margin-top:16px" background layout="total, prev, pager, next"
        :total="total" v-model:current-page="page" :page-size="20" @current-change="load" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAuditLogs, downloadFile } from '../api'
import PageHeader from '../components/PageHeader.vue'
import EmptyGuide from '../components/EmptyGuide.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const actionFilter = ref('')
const actionOptions = [
  'LOGIN', 'CREATE_TASK', 'PUBLISH_TASK', 'ASSIGN_EXPERT', 'SUBMIT_REVIEW',
  'SUMMARIZE_TASK', 'ARCHIVE_TASK', 'CLONE_TASK', 'IMPORT_PROJECT', 'CREATE_USER'
]

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: 20 }
    if (keyword.value) params.keyword = keyword.value
    if (actionFilter.value) params.action = actionFilter.value
    const res = await getAuditLogs(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function exportLogs() {
  const params = new URLSearchParams()
  if (keyword.value) params.set('keyword', keyword.value)
  if (actionFilter.value) params.set('action', actionFilter.value)
  const qs = params.toString()
  downloadFile(`/api/audit/export${qs ? '?' + qs : ''}`, 'audit_logs.xlsx')
}

onMounted(load)
</script>
