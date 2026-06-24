<template>
  <section class="footer-cta liquid-glass-panel">
    <p class="eyebrow">Start Creating</p>
    <h2>把你想复刻的视频，拆成你自己的 AI 创作方法</h2>
    <p class="footer-cta__copy">
      现在就上传一个参考视频，让工作台自动输出结构、分镜、脚本和提示词。
    </p>

    <div class="footer-cta__actions">
      <button class="footer-cta__primary" @click="$emit('primary')">
        {{ currentUser ? '上传参考视频' : '登录后开始' }}
      </button>
      <button class="footer-cta__secondary" @click="$emit('secondary')">查看工作台</button>
    </div>
  </section>
</template>

<script setup>
defineProps({
  currentUser: {
    type: Object,
    default: null,
  },
})

defineEmits(['primary', 'secondary'])
</script>

<style scoped>
.footer-cta {
  border-radius: 40px;
  padding: 42px 34px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--text-faint);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-size: 0.74rem;
}

.footer-cta h2,
.footer-cta__copy {
  margin: 0;
}

.footer-cta h2 {
  font-family: var(--font-display);
  font-size: clamp(2.2rem, 4vw, 4rem);
  max-width: 16ch;
}

.footer-cta__copy {
  margin-top: 16px;
  max-width: 40rem;
  color: var(--text-soft);
  line-height: 1.8;
}

.footer-cta__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 24px;
}

.footer-cta__primary,
.footer-cta__secondary {
  border-radius: 999px;
  padding: 15px 24px;
}

.footer-cta__primary {
  background: linear-gradient(135deg, var(--amber), #f58a6f);
  color: #111216;
  font-weight: 800;
}

.footer-cta__secondary {
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-strong);
}

@media (max-width: 760px) {
  .footer-cta {
    padding: 24px 18px;
    border-radius: 28px;
  }
}
</style>
