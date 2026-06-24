<template>
  <aside class="rail liquid-glass-panel">
    <div class="rail__header">
      <div>
        <p class="eyebrow">{{ previewMode ? '演示项目库' : '项目库' }}</p>
        <h2>{{ previewMode ? '工作台预览' : '真实项目列表' }}</h2>
      </div>
      <span class="count">{{ projects.length }}</span>
    </div>

    <div v-if="loading" class="empty">正在同步项目...</div>
    <div v-else-if="!projects.length" class="empty">还没有项目，先上传一个参考视频开始。</div>

    <div v-else class="project-list">
      <article
        v-for="project in projects"
        :key="project.id"
        class="project-card"
        :class="{ 'project-card--selected': selectedId === project.id }"
        @click="$emit('select', project.id)"
      >
        <div class="project-card__meta">
          <div>
            <strong>{{ project.projectTitle || project.filename }}</strong>
            <p>{{ formatPlatform(project.platform) }} · {{ formatSourceType(project.sourceType) }}</p>
          </div>
          <span class="badge" :class="`badge--${(project.analysisStatus || 'PENDING').toLowerCase()}`">
            {{ formatStatus(project.analysisStatus) }}
          </span>
        </div>

        <p class="project-card__summary">
          {{ project.aiSummary || '还没有摘要，执行分析后会生成结构化结果。' }}
        </p>

        <div class="project-card__footer">
          <small>{{ formatDate(project.updatedAt || project.uploadTime) }}</small>
          <button v-if="!previewMode" class="delete" @click.stop="$emit('delete', project.id)">删除</button>
          <span v-else class="preview-tag">演示</span>
        </div>
      </article>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  projects: {
    type: Array,
    default: () => [],
  },
  selectedId: {
    type: Number,
    default: null,
  },
  loading: Boolean,
  previewMode: Boolean,
})

defineEmits(['select', 'delete'])

function formatPlatform(value) {
  return value || '通用'
}

function formatSourceType(value) {
  if (value === 'UPLOAD') {
    return '本地上传'
  }
  if (value === 'URL') {
    return '链接导入'
  }
  return value || '未知来源'
}

function formatStatus(value) {
  const status = value || 'PENDING'
  const map = {
    PENDING: '待处理',
    TRANSCRIBING: '转写中',
    ANALYZING: '分析中',
    SEGMENTED: '已拆解',
    REWRITTEN: '已改写',
    FAILED: '失败',
  }
  return map[status] || status
}

function formatDate(value) {
  if (!value) {
    return '--'
  }
  return new Date(value).toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.rail {
  border-radius: var(--radius-xl);
  padding: 20px;
}

.rail__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--text-faint);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.74rem;
}

.rail__header h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.76rem;
}

.count {
  min-width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-strong);
}

.empty {
  min-height: 220px;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--text-soft);
  line-height: 1.7;
}

.project-list {
  display: grid;
  gap: 14px;
}

.project-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.project-card:hover {
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.16);
}

.project-card--selected {
  background:
    linear-gradient(180deg, rgba(244, 183, 127, 0.12), rgba(255, 255, 255, 0.03)),
    rgba(255, 255, 255, 0.04);
  border-color: rgba(244, 183, 127, 0.36);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.project-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.project-card__meta strong {
  display: block;
  margin-bottom: 6px;
}

.project-card__meta p,
.project-card__summary,
.project-card__footer small {
  margin: 0;
  color: var(--text-soft);
  font-size: 0.92rem;
  line-height: 1.6;
}

.project-card__summary {
  margin-top: 12px;
}

.project-card__footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-top: 16px;
}

.delete,
.preview-tag {
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 0.78rem;
}

.delete {
  background: rgba(255, 146, 121, 0.12);
  color: var(--rose);
}

.preview-tag {
  background: rgba(119, 213, 206, 0.1);
  color: var(--teal);
}

.badge {
  padding: 8px 10px;
  border-radius: 999px;
  font-size: 0.76rem;
  white-space: nowrap;
}

.badge--pending {
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-soft);
}

.badge--transcribing,
.badge--analyzing {
  background: rgba(244, 183, 127, 0.12);
  color: var(--amber);
}

.badge--segmented,
.badge--rewritten {
  background: rgba(119, 213, 206, 0.12);
  color: var(--teal);
}

.badge--failed {
  background: rgba(255, 146, 121, 0.12);
  color: var(--rose);
}
</style>
