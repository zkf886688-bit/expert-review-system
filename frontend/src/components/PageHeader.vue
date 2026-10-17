<template>
  <div class="page-header">
    <div class="page-header-main">
      <el-button v-if="showBack" link type="primary" class="back-btn" @click="$router.back()">
        <AppIcon name="arrow-left" :size="16" />
        返回
      </el-button>
      <div class="page-header-text">
        <h1 class="page-title">{{ title }}</h1>
        <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
      </div>
    </div>
    <div v-if="$slots.extra" class="page-header-extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  showBack: { type: Boolean, default: false }
})
</script>

<style scoped>
.page-header-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 0;
}

.page-header-text {
  min-width: 0;
}

.back-btn {
  margin-top: 4px;
  padding-left: 0;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.page-header-extra {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.page-header-extra :deep(.el-input),
.page-header-extra :deep(.el-select) {
  --el-input-border-radius: var(--app-radius-sm);
}
</style>
