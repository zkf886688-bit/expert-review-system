<template>
  <component
    :is="iconComponent"
    v-if="iconComponent"
    :size="size"
    :stroke-width="strokeWidth"
    :class="['app-icon', { 'app-icon--spin': spin }, $attrs.class]"
    v-bind="passThroughAttrs"
  />
</template>

<script setup>
import { computed, useAttrs } from 'vue'
import { getIconComponent } from '../icons'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 18 },
  strokeWidth: { type: [Number, String], default: 1.75 },
  spin: { type: Boolean, default: false }
})

const attrs = useAttrs()

const iconComponent = computed(() => getIconComponent(props.name))

const passThroughAttrs = computed(() => {
  const { class: _class, ...rest } = attrs
  return rest
})
</script>

<style scoped>
.app-icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}

.app-icon--spin {
  animation: app-icon-spin 1s linear infinite;
}

@keyframes app-icon-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
