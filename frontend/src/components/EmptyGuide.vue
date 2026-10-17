<template>
  <div class="empty-guide">
    <div class="empty-visual">
      <div class="empty-icon-ring">
        <AppIcon :name="icon" :size="40" :stroke-width="1.25" class="empty-state-icon" />
      </div>
    </div>
    <p class="empty-desc">{{ description }}</p>
    <el-button v-if="actionText" type="primary" round @click="handleAction">{{ actionText }}</el-button>
    <slot />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  description: { type: String, required: true },
  actionText: { type: String, default: '' },
  actionTo: { type: String, default: '' },
  imageSize: { type: Number, default: 100 },
  icon: { type: String, default: 'inbox' }
})

const emit = defineEmits(['action'])
const router = useRouter()

function handleAction() {
  if (props.actionTo) router.push(props.actionTo)
  emit('action')
}
</script>

<style scoped>
.empty-guide {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 24px 40px;
  text-align: center;
}

.empty-visual {
  margin-bottom: 20px;
}

.empty-icon-ring {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, var(--app-primary-muted), var(--app-accent-muted));
  color: var(--app-primary);
  box-shadow: var(--app-shadow-inset);
}

.empty-desc {
  margin: 0 0 20px;
  font-size: 15px;
  color: var(--app-text-secondary);
  max-width: 320px;
  line-height: 1.6;
}

.empty-state-icon {
  opacity: 0.9;
}
</style>
