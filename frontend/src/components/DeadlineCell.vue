<template>
  <span class="deadline-cell" :class="urgency !== 'normal' && urgency !== 'none' ? `deadline-cell--${urgency}` : ''">
    <el-tag
      v-if="showTag && urgency !== 'normal' && urgency !== 'none'"
      :type="tagType"
      size="small"
      effect="light"
      class="deadline-tag"
    >
      {{ tagLabel }}
    </el-tag>
    <span class="deadline-text">{{ formattedValue }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { deadlineUrgency, deadlineUrgencyLabel, deadlineUrgencyTagType, formatDeadline } from '../utils/deadline'

const props = defineProps({
  value: { type: String, default: '' },
  showTag: { type: Boolean, default: true }
})

const urgency = computed(() => deadlineUrgency(props.value))
const tagLabel = computed(() => deadlineUrgencyLabel(urgency.value))
const tagType = computed(() => deadlineUrgencyTagType(urgency.value))
const formattedValue = computed(() => formatDeadline(props.value))
</script>

<style scoped>
.deadline-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.deadline-cell--soon .deadline-text {
  color: #d97706;
  font-weight: 500;
}

.deadline-cell--overdue .deadline-text {
  color: #dc2626;
  font-weight: 600;
}

.deadline-tag {
  flex-shrink: 0;
}
</style>
