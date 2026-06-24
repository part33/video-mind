<template>
  <section class="upload-shell">
    <div class="upload-head">
      <div class="upload-head__label">素材导入</div>
      <div class="upload-head__hint">
        {{ disabled ? '登录后可上传素材或导入链接' : busy ? '素材处理中...' : '支持本地视频 / 公开网页视频链接' }}
      </div>
    </div>

    <div class="upload-board" :class="{ 'upload-board--disabled': disabled || busy }">
      <label class="upload-pane upload-pane--file">
        <input type="file" accept="video/*" hidden :disabled="disabled || busy" @change="handleFileChange" />
        <span class="upload-pane__icon">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M12 16V5m0 0-4 4m4-4 4 4M5 15v3a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-3"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.8"
            />
          </svg>
        </span>
        <div class="upload-pane__title">本地视频</div>
        <p>点击 / 拖拽本地视频文件</p>
      </label>

      <form class="upload-pane upload-pane--link" @submit.prevent="submitUrl">
        <span class="upload-pane__icon">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path
              d="M12 20c4.418 0 8-3.582 8-8m-8 8c-4.418 0-8-3.582-8-8m8 8c1.933 0 3.5-3.582 3.5-8S13.933 4 12 4m0 16c-1.933 0-3.5-3.582-3.5-8S10.067 4 12 4m0 0c4.418 0 8 3.582 8 8m-16 0h16"
              fill="none"
              stroke="currentColor"
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="1.6"
            />
          </svg>
        </span>
        <div class="upload-pane__title">网页链接</div>
        <p>B站 / YouTube / 抖音</p>

        <div class="url-entry">
          <input
            v-model.trim="urlValue"
            type="text"
            placeholder="粘贴视频链接..."
            :disabled="busy"
          />
          <button type="submit" :disabled="busy" aria-label="导入链接">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="m8 5 8 7-8 7"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
              />
            </svg>
          </button>
        </div>
      </form>
    </div>

    <div class="upload-foot">
      <span>输出：文字提取 / AI 总结 / 动作拆解 / 分镜改写</span>
      <button v-if="disabled" class="upload-foot__auth" type="button" @click="$emit('request-auth')">
        去登录
      </button>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  busy: Boolean,
  disabled: Boolean,
})

const emit = defineEmits(['file-upload', 'url-upload', 'request-auth'])

const urlValue = ref('')

function handleFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) {
    return
  }
  if (props.disabled) {
    emit('request-auth')
    return
  }
  emit('file-upload', file)
}

function submitUrl() {
  if (props.disabled) {
    emit('request-auth')
    return
  }
  if (!urlValue.value) {
    return
  }
  emit('url-upload', urlValue.value)
  urlValue.value = ''
}
</script>

<style scoped>
.upload-shell {
  display: grid;
  gap: 16px;
}

.upload-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

.upload-head__label,
.upload-head__hint {
  font-family: var(--font-display);
  letter-spacing: 0.12em;
}

.upload-head__label {
  font-size: 0.92rem;
  color: rgba(243, 244, 246, 0.92);
}

.upload-head__hint {
  color: var(--text-soft);
  font-size: 0.88rem;
}

.upload-board {
  position: relative;
  display: flex;
  min-height: 392px;
  overflow: hidden;
  border-radius: 30px;
  border: 1px solid rgba(201, 168, 255, 0.28);
  background:
    linear-gradient(180deg, rgba(16, 18, 24, 0.96), rgba(12, 14, 18, 0.98)),
    rgba(12, 14, 18, 0.96);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.04),
    0 24px 64px rgba(0, 0, 0, 0.35);
  perspective: 1400px;
}

.upload-board::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 28% 28%, rgba(201, 168, 255, 0.1), transparent 16%),
    radial-gradient(circle at 78% 26%, rgba(201, 168, 255, 0.08), transparent 18%);
  pointer-events: none;
}

.upload-board::after {
  content: '';
  position: absolute;
  top: -10%;
  bottom: -10%;
  left: 50%;
  width: 2px;
  background: linear-gradient(180deg, transparent, var(--lime), transparent);
  transform: skewX(-10deg);
  box-shadow: 0 0 24px rgba(201, 168, 255, 0.46);
}

.upload-board--disabled {
  opacity: 0.6;
}

.upload-pane {
  --glow-x: 50%;
  --glow-y: 50%;
  --glow-intensity: 0;
  --glow-radius: 260px;
  --glow-color: 201, 168, 255;
  position: relative;
  z-index: 1;
  flex: 1;
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 14px;
  padding: 36px 44px;
  text-align: center;
  color: var(--text-strong);
  transform-style: preserve-3d;
  transition: background 0.22s ease;
  will-change: transform, opacity;
}

.upload-pane::before {
  content: '';
  position: absolute;
  inset: -16%;
  background: radial-gradient(
    circle at var(--glow-x) var(--glow-y),
    rgba(var(--glow-color), calc(var(--glow-intensity) * 0.16)) 0%,
    rgba(var(--glow-color), calc(var(--glow-intensity) * 0.06)) 28%,
    transparent 60%
  );
  mix-blend-mode: screen;
  pointer-events: none;
}

.upload-pane::after {
  content: '';
  position: absolute;
  inset: 0;
  padding: 1px;
  border-radius: inherit;
  background: radial-gradient(
    var(--glow-radius) circle at var(--glow-x) var(--glow-y),
    rgba(var(--glow-color), calc(var(--glow-intensity) * 0.92)) 0%,
    rgba(var(--glow-color), calc(var(--glow-intensity) * 0.24)) 34%,
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
}

.upload-pane:hover {
  background: rgba(255, 255, 255, 0.02);
}

.upload-pane--file {
  cursor: pointer;
}

.upload-pane__icon {
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  color: var(--lime);
  filter: drop-shadow(0 0 14px rgba(201, 168, 255, 0.42));
  transform: translateZ(20px);
}

.upload-pane__icon svg {
  width: 44px;
  height: 44px;
}

.upload-pane__title {
  font-family: var(--font-display);
  font-size: clamp(2rem, 4vw, 3.35rem);
  font-weight: 800;
  letter-spacing: -0.03em;
  transform: translateZ(16px);
}

.upload-pane p {
  margin: 0;
  color: var(--text-faint);
  font-size: 1rem;
  transform: translateZ(12px);
}

.url-entry {
  width: min(300px, 100%);
  margin-top: 18px;
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 14px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(201, 168, 255, 0.28);
  transform: translateZ(18px);
}

.url-entry input {
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-strong);
  font-size: 1.05rem;
}

.url-entry input::placeholder {
  color: rgba(243, 244, 246, 0.28);
}

.url-entry button {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: var(--lime);
  background: transparent;
}

.url-entry button svg {
  width: 22px;
  height: 22px;
}

.url-entry button:disabled {
  opacity: 0.55;
}

.upload-foot {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  color: var(--text-soft);
  font-size: 0.9rem;
}

.upload-foot__auth {
  height: 40px;
  padding: 0 16px;
  border-radius: 10px;
  border: 1px solid rgba(201, 168, 255, 0.34);
  background: rgba(201, 168, 255, 0.08);
  color: var(--lime);
  font-family: var(--font-display);
}

@media (max-width: 900px) {
  .upload-board {
    min-height: auto;
    flex-direction: column;
  }

  .upload-board::after {
    top: 50%;
    bottom: auto;
    left: 8%;
    right: 8%;
    width: auto;
    height: 2px;
    transform: none;
  }

  .upload-pane {
    min-height: 240px;
  }
}

@media (max-width: 760px) {
  .upload-head,
  .upload-foot {
    display: grid;
  }

  .upload-pane {
    padding: 28px 20px;
  }

  .upload-pane__title {
    font-size: clamp(1.8rem, 11vw, 2.8rem);
  }
}
</style>
