<template>
  <section class="showcase-block liquid-glass-panel">
    <div class="showcase-head">
      <div>
        <p class="eyebrow">结构化输出</p>
        <h2>系统最终输出的不只是摘要，而是能继续创作的结构</h2>
      </div>
      <p>
        {{ previewMode ? '下面展示的是演示项目的输出形式，真实项目接通后端后会自动替换。' : '这里显示的是当前项目的真实结构化输出。' }}
      </p>
    </div>

    <div class="showcase-grid">
      <article class="spotlight-card">
        <span>场景拆解</span>
        <div v-if="segments.length" class="segment-stack">
          <div v-for="segment in segments" :key="segment.id || segment.segmentIndex" class="segment-line">
            <strong>{{ segment.title }}</strong>
            <small>{{ segment.actions }}</small>
          </div>
        </div>
        <p v-else>分析完成后，这里会自动列出场景节点、镜头意图和动作设计。</p>
      </article>

      <article class="spotlight-card">
        <span>脚本改写</span>
        <div v-if="latestDraft">
          <h3>AI 人设适配稿</h3>
          <p>{{ extractFirstParagraph(latestDraft.scriptMarkdown) }}</p>
        </div>
        <p v-else>当你提交人设、平台和语气后，这里会生成适配后的脚本主线。</p>
      </article>

      <article class="spotlight-card spotlight-card--wide">
        <span>提示词包</span>
        <p>{{ latestDraft ? latestDraft.promptBundle : '角色、场景、动作、镜头和语气会被统一打包成可复用提示词。' }}</p>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  detail: {
    type: Object,
    default: null,
  },
  previewMode: Boolean,
})

const segments = computed(() => props.detail?.segments?.slice(0, 3) || [])
const latestDraft = computed(() => props.detail?.rewriteDrafts?.[0] || null)

function extractFirstParagraph(value) {
  if (!value) {
    return ''
  }
  return value
    .replace(/[#*_`>-]/g, '')
    .split('\n')
    .map((item) => item.trim())
    .find(Boolean)
}
</script>

<style scoped>
.showcase-block {
  border-radius: 40px;
  padding: 34px;
  margin-bottom: 28px;
}

.showcase-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: end;
  margin-bottom: 22px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--text-faint);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.74rem;
}

.showcase-head h2,
.showcase-head p,
.spotlight-card p,
.spotlight-card h3 {
  margin: 0;
}

.showcase-head h2 {
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.5rem);
}

.showcase-head > p {
  max-width: 26rem;
  color: var(--text-soft);
  line-height: 1.7;
}

.showcase-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.spotlight-card {
  border-radius: 28px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.spotlight-card span {
  display: inline-flex;
  margin-bottom: 16px;
  color: var(--text-faint);
  font-size: 0.74rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.spotlight-card h3 {
  margin-bottom: 12px;
}

.spotlight-card p {
  color: var(--text-soft);
  line-height: 1.7;
}

.segment-stack {
  display: grid;
  gap: 12px;
}

.segment-line {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
}

.segment-line strong,
.segment-line small {
  display: block;
}

.segment-line small {
  margin-top: 6px;
  color: var(--text-soft);
  line-height: 1.6;
}

.spotlight-card--wide {
  grid-column: span 1;
}

@media (max-width: 980px) {
  .showcase-head,
  .showcase-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}

@media (max-width: 760px) {
  .showcase-block {
    padding: 18px;
    border-radius: 28px;
  }
}
</style>
