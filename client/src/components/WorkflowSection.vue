<template>
  <section class="workflow-block liquid-glass-panel">
    <div class="workflow-head">
      <div>
        <p class="eyebrow">How It Works</p>
        <h2>不是“模仿表面”，而是把视频表达方法拆出来</h2>
      </div>
      <p class="workflow-copy">
        这套流程把参考视频从素材变成结构，再从结构变成适配 AI 人设的可执行创作稿。
      </p>
    </div>

    <div class="workflow-grid">
      <article v-for="step in steps" :key="step.title" class="workflow-card">
        <span class="workflow-card__index">0{{ step.id }}</span>
        <h3>{{ step.title }}</h3>
        <p>{{ step.body }}</p>
      </article>
    </div>
  </section>
</template>

<script setup>
const steps = [
  {
    id: 1,
    title: '导入参考视频',
    body: '支持本地上传和公开视频链接导入，把想复刻的表达样本先放进同一个工作流。',
  },
  {
    id: 2,
    title: '自动提取转写与摘要',
    body: '先把视频里的口播与核心信息抽出来，形成后续拆解的文字地基。',
  },
  {
    id: 3,
    title: '拆解动作与分镜',
    body: '把动作、镜头、停顿、情绪和钩子结构化，而不是只看表面台词。',
  },
  {
    id: 4,
    title: '适配 AI 人设改写',
    body: '根据目标平台、角色设定和语气风格，把内容改写成你自己的 AI 角色版本。',
  },
  {
    id: 5,
    title: '导出脚本与提示词',
    body: '最后沉淀成脚本、分镜建议和视频生成提示词，能直接进入下一步创作。',
  },
]
</script>

<style scoped>
.workflow-block {
  border-radius: 40px;
  padding: 34px;
  margin-bottom: 28px;
}

.workflow-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
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

.workflow-head h2,
.workflow-card h3,
.workflow-card p {
  margin: 0;
}

.workflow-head h2 {
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.6rem);
  letter-spacing: -0.03em;
}

.workflow-copy {
  max-width: 24rem;
  color: var(--text-soft);
  line-height: 1.7;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.workflow-card {
  border-radius: 28px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.workflow-card__index {
  display: inline-flex;
  margin-bottom: 16px;
  color: var(--amber);
  font-size: 0.82rem;
  letter-spacing: 0.14em;
}

.workflow-card h3 {
  margin-bottom: 10px;
  font-size: 1.02rem;
}

.workflow-card p {
  color: var(--text-soft);
  line-height: 1.7;
  font-size: 0.94rem;
}

@media (max-width: 1100px) {
  .workflow-head,
  .workflow-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}

@media (max-width: 760px) {
  .workflow-block {
    padding: 18px;
    border-radius: 28px;
  }
}
</style>
