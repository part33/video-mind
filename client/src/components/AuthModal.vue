<template>
  <teleport to="body">
    <div v-if="open" class="overlay" @click.self="$emit('close')">
      <div class="panel">
        <div class="panel__header">
          <h2>{{ mode === 'login' ? '用户登录' : '新用户注册' }}</h2>
          <button class="panel__close" @click="$emit('close')">×</button>
        </div>

        <form class="panel__body" @submit.prevent="handleSubmit">
          <label class="field">
            <span>用户名</span>
            <input v-model.trim="form.username" type="text" placeholder="请输入用户名" />
          </label>

          <label class="field">
            <span>密码</span>
            <input v-model.trim="form.password" type="password" placeholder="请输入密码" />
          </label>

          <label v-if="mode === 'register'" class="field">
            <span>昵称</span>
            <input v-model.trim="form.nickname" type="text" placeholder="请输入昵称" />
          </label>

          <p v-if="message" class="message" :class="{ 'message--error': error }">
            {{ message }}
          </p>

          <button class="submit" type="submit" :disabled="loading">
            <span>{{ loading ? '提交中...' : mode === 'login' ? '提交登录' : '提交注册' }}</span>
          </button>

          <button class="switcher" type="button" @click="$emit('switch-mode')">
            {{ mode === 'login' ? '没有账号？ 去注册' : '已有账号？ 去登录' }}
          </button>
        </form>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({
  open: Boolean,
  mode: {
    type: String,
    default: 'login',
  },
  loading: Boolean,
  message: {
    type: String,
    default: '',
  },
  error: Boolean,
})

const emit = defineEmits(['close', 'switch-mode', 'submit'])

const form = reactive({
  username: '',
  password: '',
  nickname: '',
})

watch(
  () => props.open,
  (open) => {
    if (open) {
      form.username = ''
      form.password = ''
      form.nickname = ''
    }
  }
)

function handleSubmit() {
  emit('submit', { ...form })
}
</script>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(2, 3, 5, 0.7);
  backdrop-filter: blur(10px);
  z-index: 120;
}

.panel {
  width: min(568px, 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-top: 2px solid var(--lime);
  background: linear-gradient(180deg, rgba(14, 16, 22, 0.98), rgba(18, 20, 26, 0.98));
  box-shadow: 0 28px 70px rgba(0, 0, 0, 0.45);
}

.panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 28px 28px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.panel__header h2 {
  margin: 0;
  font-size: 1.95rem;
  font-weight: 800;
}

.panel__close {
  width: 36px;
  height: 36px;
  padding: 0;
  color: var(--text-soft);
  background: transparent;
  font-size: 1.5rem;
}

.panel__body {
  display: grid;
  gap: 22px;
  padding: 32px 42px 34px;
}

.field {
  display: grid;
  gap: 12px;
}

.field span {
  font-family: var(--font-display);
  font-size: 0.84rem;
  color: rgba(243, 244, 246, 0.36);
  letter-spacing: 0.1em;
  text-align: center;
}

.field input {
  width: 100%;
  min-height: 66px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: #2b3548;
  padding: 0 18px;
  color: var(--text-strong);
  font-size: 1rem;
}

.field input::placeholder {
  color: rgba(243, 244, 246, 0.28);
}

.field input:focus {
  border-color: var(--lime);
  background: #060708;
  box-shadow: inset 0 0 0 1px rgba(201, 168, 255, 0.45);
}

.message {
  margin: 0;
  color: var(--lime);
  text-align: center;
}

.message--error {
  color: var(--danger);
}

.submit {
  display: grid;
  align-items: center;
  min-height: 66px;
  padding: 0 22px;
  border: 0;
  background: transparent;
  color: #0f1013;
  font-weight: 800;
}

.submit span {
  display: grid;
  place-items: center;
  min-height: 66px;
  background: rgba(244, 244, 245, 0.92);
  clip-path: polygon(5% 0, 100% 0, 95% 100%, 0 100%);
  transition: transform 0.2s ease;
}

.submit:hover span {
  transform: translateY(-1px);
}

.submit:disabled {
  opacity: 0.62;
}

.switcher {
  background: transparent;
  color: var(--text-soft);
  padding: 0;
  justify-self: center;
}

.switcher:hover {
  color: var(--lime);
}

@media (max-width: 640px) {
  .panel__body {
    padding: 24px 20px 24px;
  }

  .panel__header {
    padding: 20px;
  }
}
</style>
