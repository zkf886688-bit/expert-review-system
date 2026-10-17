<template>
  <div class="page-skeleton">
    <el-skeleton animated>
      <template #template>
        <el-skeleton-item variant="h1" style="width: 28%; height: 28px; margin-bottom: 12px" />
        <el-skeleton-item variant="text" style="width: 42%; margin-bottom: 24px" />
        <div v-if="stats" class="sk-stat-grid">
          <el-skeleton-item v-for="i in statCount" :key="i" variant="rect" class="sk-stat" />
        </div>
        <el-skeleton-item variant="rect" :style="{ height: tableHeight + 'px', borderRadius: '12px' }" />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup>
defineProps({
  stats: { type: Boolean, default: true },
  statCount: { type: Number, default: 4 },
  tableHeight: { type: Number, default: 320 }
})
</script>

<style scoped>
.page-skeleton {
  padding: 8px 0;
}

.sk-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.sk-stat {
  height: 88px;
  border-radius: 12px;
}

@media (max-width: 992px) {
  .sk-stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
