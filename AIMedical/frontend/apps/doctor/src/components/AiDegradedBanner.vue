<template>
  <el-alert
    v-if="visible"
    type="warning"
    :closable="false"
    show-icon
    class="ai-degraded-banner"
  >
    <template #title>
      <span class="banner-title">AI 服务不可用 — 已降级处理</span>
    </template>
    <div class="banner-content">
      <p v-if="reason" class="banner-reason">{{ reason }}</p>
      <p class="banner-hint">
        当前展示的数据为兜底建议，仅供参考。请结合临床判断进行人工决策，
        或稍后重试。降级路径：手动录入 / 模板填充 / 人工审核。
      </p>
    </div>
  </el-alert>
</template>

<script setup lang="ts">
/**
 * AI 降级标识横幅。
 *
 * <p>当后端 AiResultResponse.degraded=true 时展示，显式标注 AI 不可用并说明降级路径。
 * 各 AI 页面根据接口返回的 degraded 字段决定是否显示此横幅。
 */
defineProps<{
  visible: boolean
  reason?: string | null
}>()
</script>

<style scoped>
.ai-degraded-banner {
  margin-bottom: 16px;
}

.banner-title {
  font-weight: 600;
  font-size: 14px;
}

.banner-content {
  margin-top: 4px;
}

.banner-reason {
  margin: 0 0 4px 0;
  font-size: 13px;
  color: #b54708;
}

.banner-hint {
  margin: 0;
  font-size: 12px;
  color: #92400e;
}
</style>
