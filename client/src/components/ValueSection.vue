<template>
  <section class="value-block liquid-glass-panel">
    <div class="value-head">
      <div>
        <p class="eyebrow">Why It Matters</p>
        <h2>把参考视频拆成结构化创作方法，再迁移到自己的 AI 角色表达里</h2>
      </div>
    </div>

    <div class="value-grid">
      <article v-for="item in items" :key="item.title" class="value-card">
        <span>{{ item.tag }}</span>
        <h3>{{ item.title }}</h3>
        <p>{{ item.body }}</p>
      </article>
    </div>
  </section>
</template>

<script setup>
const items = [
  {
    tag: '创作者视角',
    title: '从视频里提取可复用结构',
    body: '系统关注镜头节奏、动作节点、台词信息和情绪变化，帮助创作者快速理解一条视频为什么有效。',
  },
  {
    tag: 'AI 人设视角',
    title: '把表达方法迁移到新角色',
    body: '保留内容结构和镜头逻辑，再替换为自己的角色设定、口吻风格和视觉语义，形成新的创作版本。',
  },
  {
    tag: '产品视角',
    title: '把分析、改写、输出放进统一工作流',
    body: '从素材导入到脚本、分镜和提示词输出都在同一个工作台完成，便于展示完整的全栈与 AI 应用能力。',
  },
]
</script>

<style scoped>
.value-block {
  border-radius: 40px;
  padding: 34px;
  margin-bottom: 28px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--text-faint);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.74rem;
}

.value-head h2 {
  margin: 0 0 22px;
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.4rem);
}

.value-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.value-card {
  border-radius: 28px;
  padding: 22px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.value-card span {
  display: inline-flex;
  margin-bottom: 16px;
  color: var(--amber);
  font-size: 0.74rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.value-card h3,
.value-card p {
  margin: 0;
}

.value-card h3 {
  margin-bottom: 12px;
}

.value-card p {
  color: var(--text-soft);
  line-height: 1.75;
}

@media (max-width: 980px) {
  .value-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .value-block {
    padding: 18px;
    border-radius: 28px;
  }
}
</style>
