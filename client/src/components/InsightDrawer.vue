<template>
  <transition name="drawer-fade">
    <div v-if="open" class="drawer-layer" @click.self="$emit('close')">
      <aside class="drawer">
        <header class="drawer__header">
          <div class="drawer__title">
            <span class="drawer__title-icon">
              <svg v-if="mode === 'transcript'" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M7 4.5h7l4 4V19a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-13a1 1 0 0 1 1-1zm7 0V9h4"
                  fill="none"
                  stroke="currentColor"
                  stroke-linejoin="round"
                  stroke-width="1.7"
                />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path
                  d="M12 3.5v3m0 11v3M4.6 12h3m8.8 0h3M6.2 6.2l2.1 2.1m7.4 7.4 2.1 2.1m0-11.6-2.1 2.1m-7.4 7.4-2.1 2.1M12 8.1A3.9 3.9 0 1 0 15.9 12 3.9 3.9 0 0 0 12 8.1Z"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.7"
                />
              </svg>
            </span>
            <h2>{{ mode === 'transcript' ? '全文文字提取' : 'AI 智能总结' }}</h2>
          </div>

          <button class="drawer__close" type="button" @click="$emit('close')">×</button>
        </header>

        <div class="drawer__body">
          <template v-if="mode === 'transcript'">
            <div v-if="transcriptText" class="content-panel content-panel--transcript">
              <p>{{ transcriptText }}</p>
            </div>

            <div v-else class="empty-state">
              <h3>还没有提取结果</h3>
              <p>{{ previewMode ? '演示任务只展示界面结构，登录并上传后会生成真实的文字提取结果。' : '先执行分析，再查看完整的文字提取内容。' }}</p>
              <button v-if="!previewMode" class="drawer-button" type="button" @click="$emit('analyze')">
                开始分析
              </button>
            </div>
          </template>

          <template v-else>
            <section v-if="summaryText" class="summary-block">
              <h3>核心摘要</h3>
              <p>{{ summaryText }}</p>
            </section>

            <section v-if="segments.length" class="summary-block">
              <h3>深度洞察</h3>
              <article v-for="segment in segments" :key="segment.id" class="segment-card">
                <div class="segment-card__time">{{ segment.startSecond }}s - {{ segment.endSecond }}s</div>
                <h4>{{ segment.title || '镜头片段' }}</h4>
                <p>{{ segment.sceneSummary }}</p>
                <ul class="segment-card__meta">
                  <li v-if="segment.actions">动作：{{ segment.actions }}</li>
                  <li v-if="segment.shotType">镜头：{{ segment.shotType }}</li>
                  <li v-if="segment.emotion">情绪：{{ segment.emotion }}</li>
                  <li v-if="segment.creativeHook">钩子：{{ segment.creativeHook }}</li>
                </ul>
              </article>
            </section>

            <section v-if="latestDraft" class="summary-block">
              <h3>改写方向</h3>
              <div class="draft-stack">
                <div v-if="latestDraft.scriptMarkdown" class="draft-card">
                  <h4>脚本草案</h4>
                  <pre>{{ latestDraft.scriptMarkdown }}</pre>
                </div>
                <div v-if="latestDraft.storyboardMarkdown" class="draft-card">
                  <h4>分镜建议</h4>
                  <pre>{{ latestDraft.storyboardMarkdown }}</pre>
                </div>
                <div v-if="latestDraft.promptBundle" class="draft-card">
                  <h4>提示词包</h4>
                  <pre>{{ latestDraft.promptBundle }}</pre>
                </div>
              </div>
            </section>

            <div v-if="!summaryText && !segments.length && !latestDraft" class="empty-state">
              <h3>还没有 AI 结果</h3>
              <p>{{ previewMode ? '演示项目只展示交互框架，真实任务分析后会在这里展示摘要、镜头拆解和改写方向。' : '先执行分析，系统会在这里展示摘要、动作拆解和可复用结构。' }}</p>
              <button v-if="!previewMode" class="drawer-button" type="button" @click="$emit('analyze')">
                开始分析
              </button>
            </div>
          </template>
        </div>
      </aside>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  open: Boolean,
  mode: {
    type: String,
    default: 'summary',
  },
  project: {
    type: Object,
    default: null,
  },
  detail: {
    type: Object,
    default: null,
  },
  previewMode: Boolean,
})

defineEmits(['close', 'analyze'])

const transcriptText = computed(() => props.detail?.project?.transcriptText?.trim?.() || '')
const summaryText = computed(() => props.detail?.project?.aiSummary?.trim?.() || props.project?.aiSummary?.trim?.() || '')
const segments = computed(() => props.detail?.segments || [])
const latestDraft = computed(() => props.detail?.rewriteDrafts?.[0] || null)
</script>

<style scoped>
.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.22s ease;
}

.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

.drawer-layer {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(2, 3, 4, 0.42);
  backdrop-filter: blur(3px);
}

.drawer {
  position: absolute;
  top: 0;
  right: 0;
  width: min(780px, 39vw);
  min-width: 420px;
  height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr;
  border-left: 2px solid var(--lime);
  background: rgba(17, 19, 24, 0.98);
  box-shadow: -24px 0 64px rgba(0, 0, 0, 0.42);
}

.drawer__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 26px 34px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(12, 14, 18, 0.98);
}

.drawer__title {
  display: flex;
  align-items: center;
  gap: 14px;
}

.drawer__title h2 {
  margin: 0;
  font-size: 2rem;
  font-weight: 800;
}

.drawer__title-icon {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  color: var(--lime);
}

.drawer__title-icon svg {
  width: 28px;
  height: 28px;
}

.drawer__close {
  width: 32px;
  height: 32px;
  padding: 0;
  background: transparent;
  color: rgba(243, 244, 246, 0.42);
  font-size: 1.6rem;
}

.drawer__body {
  overflow: auto;
  padding: 28px 34px 40px;
}

.content-panel,
.summary-block,
.empty-state {
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(8, 9, 12, 0.45);
}

.content-panel {
  padding: 28px 26px;
}

.content-panel p {
  margin: 0;
  color: rgba(243, 244, 246, 0.92);
  line-height: 2;
  white-space: pre-wrap;
}

.summary-block + .summary-block {
  margin-top: 24px;
}

.summary-block {
  padding: 30px 26px;
}

.summary-block h3,
.empty-state h3 {
  margin: 0 0 18px;
  color: var(--lime);
  font-size: 1.2rem;
}

.summary-block > p,
.empty-state p {
  margin: 0;
  color: rgba(243, 244, 246, 0.86);
  line-height: 1.9;
}

.segment-card + .segment-card {
  margin-top: 22px;
  padding-top: 22px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.segment-card__time {
  color: rgba(243, 244, 246, 0.38);
  font-family: var(--font-display);
  font-size: 0.88rem;
}

.segment-card h4 {
  margin: 8px 0 10px;
  font-size: 1.45rem;
  color: var(--lime);
}

.segment-card p {
  margin: 0;
  color: rgba(243, 244, 246, 0.86);
  line-height: 1.8;
}

.segment-card__meta {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
  color: rgba(243, 244, 246, 0.64);
}

.draft-stack {
  display: grid;
  gap: 16px;
}

.draft-card {
  padding: 18px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.02);
}

.draft-card h4 {
  margin: 0 0 12px;
  font-size: 1rem;
}

.draft-card pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: rgba(243, 244, 246, 0.78);
  line-height: 1.8;
  font-family: var(--font-body);
}

.empty-state {
  padding: 34px 26px;
}

.drawer-button {
  margin-top: 20px;
  height: 46px;
  padding: 0 18px;
  border-radius: 10px;
  border: 1px solid rgba(201, 168, 255, 0.3);
  background: rgba(201, 168, 255, 0.08);
  color: var(--lime);
  font-family: var(--font-display);
}

@media (max-width: 1080px) {
  .drawer {
    width: min(760px, 100vw);
    min-width: 0;
  }
}

@media (max-width: 760px) {
  .drawer__header,
  .drawer__body {
    padding-left: 20px;
    padding-right: 20px;
  }

  .drawer__title h2 {
    font-size: 1.5rem;
  }
}
</style>
