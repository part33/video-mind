<template>
  <section class="workspace-card liquid-glass-panel">
    <div v-if="!detail?.project" class="empty-state">
      <p class="eyebrow">创作工作区</p>
      <h2>选择一个项目，开始拆解视频</h2>
      <p>这里会展示视频摘要、转写、场景结构和 AI 改写结果。</p>
    </div>

    <template v-else>
      <header class="workspace-header">
        <div>
          <div class="workspace-header__row">
            <p class="eyebrow">{{ previewMode ? '演示工作区' : '项目工作区' }}</p>
            <span class="mode-chip">{{ previewMode ? '演示数据' : '真实数据' }}</span>
          </div>
          <h2>{{ detail.project.projectTitle || detail.project.filename }}</h2>
          <p class="workspace-subtitle">
            {{ formatPlatform(detail.project.platform) }} · {{ formatSourceType(detail.project.sourceType) }} · {{ formatStatus(detail.project.analysisStatus) }}
          </p>
        </div>

        <div class="workspace-actions">
          <button class="soft-button" @click="$emit('refresh')">刷新</button>
          <button class="primary-button" :disabled="analysisPending || previewMode" @click="$emit('analyze')">
            {{ previewMode ? '上传真实项目后可分析' : analysisPending ? '分析中...' : '开始分析' }}
          </button>
        </div>
      </header>

      <div v-if="loading" class="loading">正在加载项目详情...</div>

      <div v-else class="workspace-sections">
        <section class="panel panel--summary">
          <div class="panel__heading">
            <h3>摘要与转写</h3>
            <span v-if="detail.project.errorMessage" class="error-pill">{{ detail.project.errorMessage }}</span>
          </div>

          <div class="summary-grid">
            <article>
              <h4>AI 摘要</h4>
              <p>{{ detail.project.aiSummary || '还没有摘要，请先执行分析。' }}</p>
            </article>
            <article>
              <h4>视频转写</h4>
              <pre>{{ detail.project.transcriptText || '还没有转写内容。' }}</pre>
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel__heading">
            <h3>场景拆解</h3>
            <span>{{ detail.segments?.length || 0 }} 个片段</span>
          </div>

          <div v-if="!detail.segments?.length" class="empty-inline">
            分析完成后，这里会展示镜头结构、动作、情绪和创意钩子。
          </div>

          <div v-else class="segment-list">
            <article v-for="segment in detail.segments" :key="segment.id || segment.segmentIndex" class="segment-card">
              <div class="segment-card__meta">
                <strong>{{ segment.title }}</strong>
                <span>{{ segment.startSecond }}s - {{ segment.endSecond }}s</span>
              </div>
              <p>{{ segment.sceneSummary }}</p>
              <dl>
                <div>
                  <dt>动作</dt>
                  <dd>{{ segment.actions }}</dd>
                </div>
                <div>
                  <dt>镜头</dt>
                  <dd>{{ segment.shotType }}</dd>
                </div>
                <div>
                  <dt>钩子</dt>
                  <dd>{{ segment.creativeHook }}</dd>
                </div>
                <div>
                  <dt>情绪</dt>
                  <dd>{{ segment.emotion }}</dd>
                </div>
              </dl>
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel__heading">
            <div>
              <h3>改写工作台</h3>
              <p class="panel__desc">把拆解结果改写成适合你的 AI 人物设定的脚本与提示词。</p>
            </div>
          </div>

          <form class="rewrite-form" @submit.prevent="submitDraft">
            <label>
              <span>目标平台</span>
              <input v-model.trim="form.targetPlatform" type="text" placeholder="小红书 / 抖音 / 多平台" />
            </label>
            <label>
              <span>AI 人设</span>
              <input v-model.trim="form.persona" type="text" placeholder="例如：未来感虚拟主持人" />
            </label>
            <label>
              <span>语气风格</span>
              <input v-model.trim="form.tone" type="text" placeholder="例如：利落、轻挑、有节奏" />
            </label>
            <label>
              <span>目标受众</span>
              <input v-model.trim="form.audience" type="text" placeholder="例如：短视频创作者、品牌团队" />
            </label>
            <label>
              <span>时长（秒）</span>
              <input v-model.number="form.durationSeconds" type="number" min="10" max="180" />
            </label>

            <button class="primary-button" type="submit" :disabled="rewritePending || previewMode">
              {{ previewMode ? '上传真实项目后可生成' : rewritePending ? '生成中...' : '生成改写稿' }}
            </button>
          </form>

          <div v-if="latestDraft" class="draft-grid">
            <article class="draft-panel">
              <h4>脚本</h4>
              <div class="markdown" v-html="renderMarkdown(latestDraft.scriptMarkdown)"></div>
            </article>
            <article class="draft-panel">
              <h4>分镜</h4>
              <div class="markdown" v-html="renderMarkdown(latestDraft.storyboardMarkdown)"></div>
            </article>
            <article class="draft-panel draft-panel--full">
              <h4>提示词包</h4>
              <pre>{{ latestDraft.promptBundle }}</pre>
            </article>
          </div>
        </section>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { marked } from 'marked'

const props = defineProps({
  detail: {
    type: Object,
    default: null,
  },
  loading: Boolean,
  rewritePending: Boolean,
  analysisPending: Boolean,
  previewMode: Boolean,
})

const emit = defineEmits(['refresh', 'analyze', 'submit-draft'])

const form = reactive({
  targetPlatform: '多平台',
  persona: '',
  tone: '自然、有镜头节奏',
  audience: '短视频创作者',
  durationSeconds: 30,
})

const latestDraft = computed(() => props.detail?.rewriteDrafts?.[0] || null)

function submitDraft() {
  emit('submit-draft', { ...form })
}

function renderMarkdown(value) {
  return marked.parse(value || '')
}

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
</script>

<style scoped>
.workspace-card {
  min-height: 780px;
  border-radius: var(--radius-xl);
  padding: 24px;
}

.empty-state,
.loading,
.empty-inline {
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--text-soft);
}

.empty-state {
  min-height: 520px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--text-faint);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.74rem;
}

.workspace-header h2,
.panel__heading h3,
.draft-panel h4 {
  margin: 0;
}

.workspace-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: start;
  margin-bottom: 22px;
}

.workspace-header__row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.workspace-header h2 {
  font-family: var(--font-display);
  font-size: 2.4rem;
}

.workspace-subtitle,
.panel__desc {
  margin: 10px 0 0;
  color: var(--text-soft);
}

.mode-chip,
.error-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 0.78rem;
}

.mode-chip {
  background: rgba(119, 213, 206, 0.1);
  color: var(--teal);
}

.error-pill {
  background: rgba(255, 146, 121, 0.12);
  color: var(--rose);
}

.workspace-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.soft-button,
.primary-button {
  border-radius: 999px;
  padding: 12px 18px;
}

.soft-button {
  color: var(--text-strong);
  background: rgba(255, 255, 255, 0.08);
}

.primary-button {
  color: #101216;
  font-weight: 800;
  background: linear-gradient(135deg, var(--amber), #f58a6f);
}

.primary-button:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.workspace-sections {
  display: grid;
  gap: 18px;
}

.panel {
  border-radius: 28px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.panel__heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.summary-grid,
.draft-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.summary-grid article,
.draft-panel,
.segment-card {
  border-radius: 22px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.summary-grid p,
.summary-grid pre,
.draft-panel pre {
  margin: 12px 0 0;
  color: var(--text-soft);
  line-height: 1.75;
  white-space: pre-wrap;
}

.segment-list {
  display: grid;
  gap: 14px;
}

.segment-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: start;
  margin-bottom: 10px;
}

.segment-card__meta span,
.segment-card p,
.segment-card dt,
.segment-card dd {
  color: var(--text-soft);
}

.segment-card dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 16px 0 0;
}

.segment-card dt {
  margin-bottom: 4px;
  font-size: 0.76rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-faint);
}

.segment-card dd {
  margin: 0;
}

.rewrite-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.rewrite-form label {
  display: grid;
  gap: 8px;
}

.rewrite-form span {
  color: var(--text-faint);
  font-size: 0.84rem;
}

.rewrite-form input {
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 12px 14px;
  color: var(--text-strong);
  background: rgba(255, 255, 255, 0.04);
}

.rewrite-form input::placeholder {
  color: var(--text-faint);
}

.rewrite-form button {
  align-self: end;
}

.draft-panel--full {
  grid-column: 1 / -1;
}

.markdown :deep(h1),
.markdown :deep(h2),
.markdown :deep(h3),
.markdown :deep(h4) {
  margin: 0 0 12px;
}

.markdown :deep(p),
.markdown :deep(li) {
  color: var(--text-soft);
  line-height: 1.75;
}

.markdown :deep(ul),
.markdown :deep(ol) {
  padding-left: 18px;
}

@media (max-width: 980px) {
  .workspace-header,
  .summary-grid,
  .rewrite-form,
  .draft-grid,
  .segment-card dl {
    grid-template-columns: 1fr;
    display: grid;
  }

  .workspace-card {
    padding: 18px;
  }
}
</style>
