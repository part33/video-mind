<template>
  <div class="task-grid">
    <article
      v-for="project in projects"
      :key="project.id"
      class="task-card"
      :class="{ 'task-card--active': project.id === selectedId }"
      @click="$emit('select', project.id)"
    >
      <button
        v-if="!previewMode"
        class="task-card__close"
        type="button"
        title="删除项目"
        @click.stop="$emit('delete', project.id)"
      >
        ×
      </button>

      <div class="task-card__main">
        <div class="task-card__icon">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M15 10.5V6.8a1.8 1.8 0 0 0-1.8-1.8H6.8A1.8 1.8 0 0 0 5 6.8v10.4A1.8 1.8 0 0 0 6.8 19h6.4a1.8 1.8 0 0 0 1.8-1.8v-3.7l4 2.4a1 1 0 0 0 1.5-.86V8.96a1 1 0 0 0-1.5-.86z"
              fill="none"
              stroke="currentColor"
              stroke-linejoin="round"
              stroke-width="1.6"
            />
          </svg>
        </div>

        <div class="task-card__meta">
          <h3>{{ project.projectTitle || project.filename || '未命名项目' }}</h3>
          <div class="task-card__subline">
            <span>{{ formatTime(project.updatedAt) }}</span>
            <span class="task-card__badge" :class="statusClass(project.analysisStatus)">
              {{ statusLabel(project.analysisStatus) }}
            </span>
          </div>
        </div>
      </div>

      <div class="task-card__actions">
        <button class="action-button" type="button" disabled @click.stop>
          <span class="action-button__icon">↘</span>
          <span>下载音频</span>
        </button>

        <button class="action-button" type="button" @click.stop="$emit('open-transcript', project.id)">
          <span class="action-button__icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M7 4.5h7l4 4V19a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-13a1 1 0 0 1 1-1zm7 0V9h4"
                fill="none"
                stroke="currentColor"
                stroke-linejoin="round"
                stroke-width="1.6"
              />
            </svg>
          </span>
          <span>提取文字</span>
        </button>

        <button
          class="action-button action-button--summary"
          type="button"
          @click.stop="$emit('open-summary', project.id)"
        >
          <span class="action-button__icon">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M9 3h6v3h3v6h-3v3H9v-3H6V6h3zm1.4 1.5v3h-3v3h3v3h3.2v-3h3v-3h-3v-3z"
                fill="currentColor"
              />
            </svg>
          </span>
          <span>AI 智能总结</span>
        </button>
      </div>
    </article>
  </div>
</template>

<script setup>
defineProps({
  projects: {
    type: Array,
    default: () => [],
  },
  selectedId: {
    type: [String, Number],
    default: null,
  },
  previewMode: Boolean,
})

defineEmits(['select', 'delete', 'open-transcript', 'open-summary'])

function formatTime(value) {
  if (!value) {
    return '--/-- --:--'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '--/-- --:--'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function statusLabel(status) {
  if (status === 'FAILED') return '失败'
  if (status === 'REWRITTEN') return '已完成'
  if (status === 'SEGMENTED') return '已拆解'
  if (status === 'TRANSCRIBED') return '已转写'
  return '待处理'
}

function statusClass(status) {
  if (status === 'FAILED') return 'task-card__badge--danger'
  if (status === 'REWRITTEN' || status === 'SEGMENTED' || status === 'TRANSCRIBED') {
    return 'task-card__badge--ready'
  }
  return 'task-card__badge--pending'
}
</script>

<style scoped>
.task-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 28px;
}

.task-card {
  --glow-x: 50%;
  --glow-y: 50%;
  --glow-intensity: 0;
  --glow-radius: 280px;
  --glow-color: 132, 0, 255;
  position: relative;
  min-height: 282px;
  overflow: hidden;
  border-radius: 22px;
  border: 1px solid rgba(108, 118, 135, 0.26);
  background:
    linear-gradient(180deg, rgba(17, 19, 24, 0.94), rgba(12, 14, 18, 0.98)),
    rgba(18, 20, 24, 0.92);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.03),
    0 18px 30px rgba(0, 0, 0, 0.18);
  transform-style: preserve-3d;
  transition: border-color 0.28s ease, box-shadow 0.28s ease;
  will-change: transform, opacity;
}

.task-card::before {
  content: '';
  position: absolute;
  inset: -18%;
  background: radial-gradient(
    circle at var(--glow-x) var(--glow-y),
    rgba(var(--glow-color), calc(var(--glow-intensity) * 0.22)) 0%,
    rgba(var(--glow-color), calc(var(--glow-intensity) * 0.08)) 28%,
    transparent 60%
  );
  opacity: 1;
  pointer-events: none;
  mix-blend-mode: screen;
}

.task-card::after {
  content: '';
  position: absolute;
  inset: 0;
  padding: 1px;
  border-radius: inherit;
  background: radial-gradient(
    var(--glow-radius) circle at var(--glow-x) var(--glow-y),
    rgba(var(--glow-color), calc(var(--glow-intensity) * 0.95)) 0%,
    rgba(var(--glow-color), calc(var(--glow-intensity) * 0.38)) 32%,
    transparent 62%
  );
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  mask-composite: exclude;
  pointer-events: none;
  opacity: 1;
}

.task-card:hover {
  border-color: rgba(201, 168, 255, 0.22);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.05),
    0 24px 44px rgba(0, 0, 0, 0.22);
}

.task-card--active {
  border-color: rgba(201, 168, 255, 0.92);
  box-shadow:
    inset 0 0 0 1px rgba(201, 168, 255, 0.3),
    0 0 0 1px rgba(201, 168, 255, 0.24),
    0 26px 50px rgba(0, 0, 0, 0.24);
}

.task-card__close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 3;
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 999px;
  background: transparent;
  color: rgba(243, 244, 246, 0.22);
  font-size: 1.6rem;
}

.task-card__main {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 18px;
  padding: 36px 34px 30px;
  align-items: center;
}

.task-card__icon {
  display: grid;
  place-items: center;
  width: 82px;
  height: 82px;
  border-radius: 14px;
  border: 2px solid rgba(201, 168, 255, 0.8);
  color: var(--lime);
  transform: translateZ(22px);
}

.task-card__icon svg {
  width: 34px;
  height: 34px;
}

.task-card__meta {
  transform: translateZ(14px);
}

.task-card__meta h3 {
  margin: 0;
  font-size: clamp(1.2rem, 2vw, 1.45rem);
  font-weight: 800;
  line-height: 1.2;
  word-break: break-word;
}

.task-card__subline {
  margin-top: 14px;
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
  color: rgba(243, 244, 246, 0.44);
}

.task-card__badge {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid rgba(201, 168, 255, 0.54);
  font-family: var(--font-display);
  font-size: 0.9rem;
  font-weight: 700;
}

.task-card__badge--ready {
  color: var(--lime);
}

.task-card__badge--pending {
  color: rgba(243, 244, 246, 0.72);
  border-color: rgba(243, 244, 246, 0.22);
}

.task-card__badge--danger {
  color: var(--danger);
  border-color: rgba(255, 125, 107, 0.42);
}

.task-card__actions {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.action-button {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 10px;
  min-height: 88px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(94, 106, 128, 0.28);
  background: rgba(18, 20, 24, 0.72);
  color: rgba(243, 244, 246, 0.48);
  text-align: center;
  transition: border-color 0.24s ease, color 0.24s ease, background 0.24s ease;
}

.action-button__icon {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  font-size: 1.2rem;
}

.action-button__icon svg {
  width: 22px;
  height: 22px;
}

.action-button--summary {
  color: var(--purple);
  border-color: rgba(141, 53, 255, 0.92);
  box-shadow: inset 0 0 0 1px rgba(141, 53, 255, 0.2);
}

.action-button:not(:disabled):hover {
  border-color: rgba(201, 168, 255, 0.34);
  color: rgba(243, 244, 246, 0.86);
  background: rgba(255, 255, 255, 0.03);
}

.action-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

@media (max-width: 1280px) {
  .task-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .task-grid {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .task-card__main {
    grid-template-columns: 80px 1fr;
    padding: 24px 20px;
  }

  .task-card__actions {
    grid-template-columns: 1fr;
  }
}
</style>
