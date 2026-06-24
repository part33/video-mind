<template>
  <div class="page-shell">
    <div ref="appShellRef" class="app-shell">
      <AmbientBackground class="bg-grainient" />
      <div class="bg-texture"></div>
      <PromptBackdrop :lines="promptWallLines" />
      <div class="bg-mask"></div>

      <header class="topbar">
        <div class="brand">
          <span class="brand__logo">Video <span>Mind</span> AI</span>
          <span class="brand__pro">创作台</span>
        </div>

        <div class="topbar__right">
          <div v-if="currentUser" class="account">
            <span class="account__name">{{ currentUser.nickname || currentUser.username }}</span>
            <button class="icon-button" @click="logout">退出</button>
          </div>
          <button v-else class="login-button" @click="openAuthModal('login')">登录 / 注册</button>
          <div class="system-chip">
            <span class="system-chip__dot"></span>
            <span>{{ isSystemBusy ? '系统处理中' : '系统就绪' }}</span>
          </div>
        </div>
      </header>

      <main class="main-layout">
        <section class="hero">
          <div class="section-kicker">AI VIDEO CREATION WORKSPACE</div>
          <h1 class="hero-title">
            <span class="hero-title__line">VIDEO MIND</span>
          </h1>
          <p class="hero-subtitle">Analyze · Segment · Rewrite</p>

          <div class="hero-upload motion-panel">
            <UploadComposer
              :busy="uploadBusy"
              :disabled="!currentUser"
              @file-upload="handleFileUpload"
              @url-upload="handleUrlUpload"
              @request-auth="openAuthModal('login')"
            />
          </div>
        </section>

        <section class="workspace">
          <div class="workspace__intro">
            <div class="section-kicker">项目工作台</div>
            <h2 class="section-display">创作面板</h2>
            <p class="workspace__lead">
              把上传、分析、摘要查看和结果复用放进同一屏幕，方便围绕一个参考视频连续完成理解、拆解和改写。
            </p>
          </div>

          <div class="workspace__head">
            <div class="workspace__title">
              <h3>工作台</h3>
              <span>{{ taskCountLabel }}</span>
            </div>

            <div class="workspace__actions">
              <button class="toolbar-button" @click="refreshSelectedProject">刷新项目</button>
              <button
                class="toolbar-button toolbar-button--lime"
                :disabled="!currentUser || showDemoPreview || analysisPending"
                @click="analyzeSelectedProject"
              >
                {{ analysisPending ? '分析中...' : '开始分析' }}
              </button>
            </div>
          </div>

          <div class="workspace__layout">
            <aside class="workspace__side">
              <article class="workspace-panel workspace-panel--active">
                <span class="workspace-panel__eyebrow">当前项目</span>
                <h3>{{ selectedProjectTitle }}</h3>
                <div class="workspace-panel__status">
                  <span class="workspace-panel__badge">{{ selectedProjectStatus }}</span>
                  <span>{{ selectedProjectTimestamp }}</span>
                </div>
                <p class="workspace-panel__summary">{{ selectedProjectSummary }}</p>
                <div class="workspace-panel__meta">
                  <span v-for="item in selectedProjectMeta" :key="item">{{ item }}</span>
                </div>
                <div class="workspace-panel__actions">
                  <button
                    class="toolbar-button toolbar-button--lime"
                    :disabled="!effectiveSelectedProjectId"
                    @click="openSelectedSummary"
                  >
                    查看 AI 总结
                  </button>
                  <button
                    class="toolbar-button"
                    :disabled="!effectiveSelectedProjectId"
                    @click="openDrawer('transcript', effectiveSelectedProjectId)"
                  >
                    查看文字提取
                  </button>
                </div>
              </article>

              <article class="workspace-panel">
                <span class="workspace-panel__eyebrow">处理流程</span>
                <ol class="workspace-flow">
                  <li v-for="step in workflowSteps" :key="step.id" class="workspace-flow__item">
                    <span class="workspace-flow__index">{{ step.id }}</span>
                    <div>
                      <strong>{{ step.title }}</strong>
                      <p>{{ step.description }}</p>
                    </div>
                  </li>
                </ol>
              </article>
            </aside>

            <div class="workspace__main">
              <TaskGrid
                class="task-grid-wrap"
                :projects="displayedProjects"
                :selected-id="effectiveSelectedProjectId"
                :preview-mode="showDemoPreview"
                @select="handleProjectSelect"
                @delete="deleteProject"
                @open-transcript="openDrawer('transcript', $event)"
                @open-summary="openDrawer('summary', $event)"
              />
            </div>
          </div>
        </section>
      </main>

      <InsightDrawer
        :open="drawerOpen"
        :mode="drawerMode"
        :project="drawerProject"
        :detail="drawerDetail"
        :preview-mode="showDemoPreview"
        @close="drawerOpen = false"
        @analyze="analyzeSelectedProject"
      />

      <AuthModal
        :open="authOpen"
        :mode="authMode"
        :loading="authLoading"
        :message="authMessage"
        :error="authError"
        @close="authOpen = false"
        @switch-mode="switchAuthMode"
        @submit="submitAuth"
      />

      <transition name="toast">
        <div v-if="toast.message" class="toast" :class="`toast--${toast.kind}`">
          {{ toast.message }}
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import AuthModal from './components/AuthModal.vue'
import AmbientBackground from './components/AmbientBackground.vue'
import InsightDrawer from './components/InsightDrawer.vue'
import PromptBackdrop from './components/PromptBackdrop.vue'
import TaskGrid from './components/TaskGrid.vue'
import UploadComposer from './components/UploadComposer.vue'
import { setupMagicSurface, setupSectionSpotlight } from './lib/magicSurface'
import { api } from './lib/api'

gsap.registerPlugin(ScrollTrigger)

const promptWallLines = [
  '视频解析 / 场景拆解 / 人设改写',
  '参考视频 / 动作线索 / 台词结构',
  '镜头节奏 / 分镜输出 / 脚本草稿',
  '创作整理 / 内容分析 / 项目归档',
  '把参考视频整理成可继续创作的结构化结果',
  '上传素材后即可在同一工作台查看分析结果',
]

const demoProjects = [
  {
    id: 9001,
    projectTitle: '街访口播拆解',
    filename: 'street-talk.mp4',
    platform: 'B站',
    sourceType: 'URL',
    analysisStatus: 'REWRITTEN',
    aiSummary: '开场先用反差问题抓注意力，中段通过动作停顿切分信息，适合迁移为虚拟人镜头脚本。',
    updatedAt: '2026-06-18T21:10:00',
  },
  {
    id: 9002,
    projectTitle: '产品安利节奏',
    filename: 'product-rhythm.mp4',
    platform: 'YouTube',
    sourceType: 'URL',
    analysisStatus: 'SEGMENTED',
    aiSummary: '镜头节奏短促，依赖触发句和体验递进推动观看，适合转成高密度短视频结构。',
    updatedAt: '2026-06-18T19:20:00',
  },
  {
    id: 9003,
    projectTitle: '外景素材样片.mp4',
    filename: 'sample-footage.mp4',
    platform: '本地上传',
    sourceType: 'UPLOAD',
    analysisStatus: 'PENDING',
    aiSummary: '',
    updatedAt: '2026-06-19T09:35:00',
  },
]

const demoDetails = {
  9001: {
    project: {
      ...demoProjects[0],
      transcriptText:
        '第一句先抛出一个反常识问题，让观众必须继续听下去。随后通过停顿、指镜头和前倾动作切开信息层级，最后用一句人设化收尾形成记忆点。',
      errorMessage: '',
    },
    segments: [
      {
        id: 1,
        title: '问题钩子先行',
        startSecond: 0,
        endSecond: 4,
        sceneSummary: '开场不急着解释，而是先丢出一个带冲突的问题，把观众停留在第一秒。',
        actions: '停顿、前倾、指向镜头',
        creativeHook: '先问后答',
        shotType: '近景口播',
        emotion: '笃定',
      },
      {
        id: 2,
        title: '动作辅助解释',
        startSecond: 5,
        endSecond: 10,
        sceneSummary: '通过摆手和横向位移，把连续信息切成更好吸收的几拍。',
        actions: '摆手、摊掌、轻微侧身',
        creativeHook: '动作领先语言',
        shotType: '半身移动镜头',
        emotion: '流畅',
      },
      {
        id: 3,
        title: '人设式收尾',
        startSecond: 11,
        endSecond: 16,
        sceneSummary: '结尾不做普通总结，而是把角色气质和口播风格一起打进去。',
        actions: '微笑、侧头、收尾抬手',
        creativeHook: '结尾做人物记忆点',
        shotType: '特写',
        emotion: '轻盈',
      },
    ],
    rewriteDrafts: [
      {
        id: 1,
        scriptMarkdown:
          '### 改写脚本\n\n1. 第一秒先抛问题。\n2. 每个信息点都配一个明确动作。\n3. 结尾切到 AI 虚拟人角色语气收口。',
        storyboardMarkdown:
          '### 分镜建议\n\n- 镜头一：正面近景 + 半秒停顿\n- 镜头二：半身移动镜头\n- 镜头三：特写收尾',
        promptBundle:
          '角色：未来感虚拟主持人\n场景：深色背景 + 轮廓冷光\n动作：停顿、前倾、指镜头、收尾抬手\n镜头：近景 -> 半身 -> 特写\n语气：直接、清晰、节奏感强',
      },
    ],
  },
  9002: {
    project: {
      ...demoProjects[1],
      transcriptText:
        '这一类视频不依赖大段台词，而是依赖镜头切换、体验差异和触发句推动观看，非常适合改造成节奏型短视频。',
      errorMessage: '',
    },
    segments: [
      {
        id: 11,
        title: '体验先于说明',
        startSecond: 0,
        endSecond: 6,
        sceneSummary: '先让观众感知变化，再解释变化本身。',
        actions: '局部特写、慢动作展示',
        creativeHook: '先看再懂',
        shotType: '局部特写',
        emotion: '好奇',
      },
    ],
    rewriteDrafts: [],
  },
  9003: {
    project: {
      ...demoProjects[2],
      transcriptText: '',
      errorMessage: '',
    },
    segments: [],
    rewriteDrafts: [],
  },
}

const workflowSteps = [
  {
    id: '01',
    title: '导入素材',
    description: '上传本地视频或粘贴网页链接，任务会立即进入工作台。',
  },
  {
    id: '02',
    title: '触发分析',
    description: '在当前页启动分析，无需跳转到新页面等待处理完成。',
  },
  {
    id: '03',
    title: '查看结果',
    description: '通过右侧抽屉查看 AI 总结、文字提取和改写方向。',
  },
]

const currentUser = ref(null)
const projects = ref([])
const selectedProjectId = ref(null)
const projectDetail = ref(null)

const uploadBusy = ref(false)
const loadingProjects = ref(false)
const loadingDetail = ref(false)
const analysisPending = ref(false)

const authOpen = ref(false)
const authMode = ref('login')
const authLoading = ref(false)
const authMessage = ref('')
const authError = ref(false)

const drawerOpen = ref(false)
const drawerMode = ref('summary')
const drawerProjectId = ref(null)

const toast = reactive({
  message: '',
  kind: 'info',
})

const appShellRef = ref(null)

let toastTimer = null
let projectPoller = null
let motionContext = null
let motionEnabled = false
let magicCleanups = []

const showDemoPreview = computed(() => !projects.value.length)
const displayedProjects = computed(() => (showDemoPreview.value ? demoProjects : projects.value))
const isSystemBusy = computed(
  () => uploadBusy.value || loadingProjects.value || loadingDetail.value || analysisPending.value
)
const effectiveSelectedProjectId = computed(() => {
  const selected = displayedProjects.value.find((item) => item.id === selectedProjectId.value)
  return selected?.id || displayedProjects.value[0]?.id || null
})

const selectedProject = computed(
  () => displayedProjects.value.find((item) => item.id === effectiveSelectedProjectId.value) || null
)

const drawerProject = computed(() =>
  displayedProjects.value.find((item) => item.id === (drawerProjectId.value || effectiveSelectedProjectId.value)) || null
)

const drawerDetail = computed(() => {
  const targetId = drawerProjectId.value || effectiveSelectedProjectId.value
  if (showDemoPreview.value) {
    return demoDetails[targetId] || null
  }
  if (projectDetail.value?.project?.id === targetId) {
    return projectDetail.value
  }
  return null
})

const selectedProjectTitle = computed(
  () => selectedProject.value?.projectTitle || selectedProject.value?.filename || '等待新任务'
)
const selectedProjectStatus = computed(() => getStatusLabel(selectedProject.value?.analysisStatus))
const selectedProjectTimestamp = computed(() => formatProjectTime(selectedProject.value?.updatedAt))
const selectedProjectSummary = computed(() => {
  const detailSummary = drawerDetail.value?.project?.aiSummary?.trim?.()
  const projectSummary = selectedProject.value?.aiSummary?.trim?.()

  if (detailSummary) return detailSummary
  if (projectSummary) return projectSummary
  if (showDemoPreview.value) {
    return '当前是演示工作台。你可以先感受任务流和结果抽屉的交互，登录并上传后会自动切到真实项目。'
  }

  return '上传并分析后，这里会展示当前任务的摘要、镜头结构和下一步建议。'
})
const selectedProjectMeta = computed(() => {
  const project = selectedProject.value
  if (!project) {
    return ['等待上传素材', '支持本地视频与网页链接', '结果会在右侧抽屉查看']
  }

  return [
    project.sourceType === 'URL' ? '链接导入' : '本地上传',
    project.platform || '多平台素材',
    selectedProjectStatus.value,
  ].filter(Boolean)
})

onMounted(() => {
  const savedUser = localStorage.getItem('video-mind-ai-user')
  if (savedUser) {
    try {
      currentUser.value = JSON.parse(savedUser)
    } catch {
      localStorage.removeItem('video-mind-ai-user')
    }
  }

  if (currentUser.value) {
    loadProjects(true)
  } else {
    selectedProjectId.value = demoProjects[0].id
  }

  nextTick(() => {
    initMotionSystem()
  })
})

onBeforeUnmount(() => {
  clearInterval(projectPoller)
  clearTimeout(toastTimer)
  destroyMotionSystem()
})

watch(
  () => displayedProjects.value.map((item) => item.id).join(','),
  async () => {
    if (!motionEnabled) {
      return
    }

    await nextTick()
    cascadeTaskCards()
    ScrollTrigger.refresh()
  }
)

function getStatusLabel(status) {
  if (status === 'FAILED') return '失败'
  if (status === 'REWRITTEN') return '已完成'
  if (status === 'SEGMENTED') return '已拆解'
  if (status === 'TRANSCRIBED') return '已转写'
  return '待处理'
}

function formatProjectTime(value) {
  if (!value) {
    return '刚刚待命'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '时间待同步'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)
}

function openAuthModal(mode = 'login') {
  authMode.value = mode
  authMessage.value = ''
  authError.value = false
  authOpen.value = true
}

function switchAuthMode() {
  authMode.value = authMode.value === 'login' ? 'register' : 'login'
  authMessage.value = ''
  authError.value = false
}

async function submitAuth(form) {
  authLoading.value = true
  authMessage.value = ''
  authError.value = false

  const endpoint = authMode.value === 'login' ? '/user/login' : '/user/register'

  try {
    const result = await api.postJson(endpoint, form)
    if (result.code !== 200) {
      throw new Error(result.msg || '请求失败')
    }

    if (authMode.value === 'login') {
      currentUser.value = result.userInfo
      localStorage.setItem('video-mind-ai-user', JSON.stringify(result.userInfo))
      authOpen.value = false
      showToast(`欢迎回来，${result.userInfo.nickname || result.userInfo.username}`, 'success')
      await loadProjects(true)
      return
    }

    authMessage.value = '注册成功，请登录。'
    authMode.value = 'login'
  } catch (error) {
    authMessage.value = error.message
    authError.value = true
  } finally {
    authLoading.value = false
  }
}

function logout() {
  currentUser.value = null
  projects.value = []
  selectedProjectId.value = demoProjects[0].id
  projectDetail.value = null
  drawerOpen.value = false
  localStorage.removeItem('video-mind-ai-user')
  clearInterval(projectPoller)
  showToast('你已退出登录。', 'info')
}

async function loadProjects(selectFirst = false) {
  if (!currentUser.value) {
    projects.value = []
    projectDetail.value = null
    selectedProjectId.value = demoProjects[0].id
    return
  }

  loadingProjects.value = true
  try {
    const result = await api.getJson(`/projects?userId=${currentUser.value.id}`)
    projects.value = Array.isArray(result) ? result : []

    if (!projects.value.length) {
      projectDetail.value = null
      selectedProjectId.value = demoProjects[0].id
      return
    }

    const shouldSelectNewest =
      selectFirst ||
      !selectedProjectId.value ||
      !projects.value.some((item) => item.id === selectedProjectId.value)

    await selectProject(shouldSelectNewest ? projects.value[0].id : selectedProjectId.value)
  } catch (error) {
    showToast(error.message, 'error')
  } finally {
    loadingProjects.value = false
  }
}

async function handleProjectSelect(projectId) {
  if (showDemoPreview.value) {
    selectedProjectId.value = projectId
    return
  }
  await selectProject(projectId)
}

async function selectProject(projectId) {
  if (!projectId || !currentUser.value) {
    return
  }

  selectedProjectId.value = projectId
  loadingDetail.value = true

  try {
    const detail = await api.getJson(`/projects/${projectId}?userId=${currentUser.value.id}`)
    projectDetail.value = detail
    syncProjectSnapshot(detail?.project)
  } catch (error) {
    showToast(error.message, 'error')
  } finally {
    loadingDetail.value = false
  }
}

async function openDrawer(mode, projectId) {
  drawerMode.value = mode
  drawerProjectId.value = projectId

  if (!showDemoPreview.value && currentUser.value && projectId !== selectedProjectId.value) {
    await selectProject(projectId)
  } else if (showDemoPreview.value) {
    selectedProjectId.value = projectId
  }

  drawerOpen.value = true
}

function openSelectedSummary() {
  if (!effectiveSelectedProjectId.value) {
    return
  }

  openDrawer('summary', effectiveSelectedProjectId.value)
}

async function refreshSelectedProject() {
  if (showDemoPreview.value) {
    showToast('当前是演示视图，登录并上传后会自动切换成真实项目。', 'info')
    return
  }
  if (!selectedProjectId.value) {
    return
  }
  await selectProject(selectedProjectId.value)
}

async function handleFileUpload(file) {
  if (!currentUser.value) {
    openAuthModal('login')
    return
  }

  uploadBusy.value = true
  try {
    await uploadFileInChunks(file)
    showToast('视频已上传。', 'success')
    await loadProjects(true)
  } catch (error) {
    showToast(error.message, 'error')
  } finally {
    uploadBusy.value = false
  }
}

async function uploadFileInChunks(file) {
  const chunkSize = 5 * 1024 * 1024
  const totalChunks = Math.ceil(file.size / chunkSize)
  const initForm = new FormData()
  initForm.append('filename', file.name)
  initForm.append('fileSize', file.size)
  initForm.append('totalChunks', totalChunks)
  initForm.append('userId', currentUser.value.id)

  const initResult = await api.postForm('/media/init-upload', initForm)
  const uploadId = initResult.uploadId
  const uploaded = new Set(initResult.uploadedChunks || [])

  for (let chunkIndex = 0; chunkIndex < totalChunks; chunkIndex += 1) {
    if (uploaded.has(chunkIndex)) {
      continue
    }
    const start = chunkIndex * chunkSize
    const chunk = file.slice(start, Math.min(file.size, start + chunkSize))
    const chunkForm = new FormData()
    chunkForm.append('uploadId', uploadId)
    chunkForm.append('chunkIndex', chunkIndex)
    chunkForm.append('file', chunk, `${file.name}.part${chunkIndex}`)
    await api.postForm('/media/upload-chunk', chunkForm)
  }

  const mergeForm = new FormData()
  mergeForm.append('uploadId', uploadId)
  mergeForm.append('filename', file.name)
  mergeForm.append('totalChunks', totalChunks)
  mergeForm.append('userId', currentUser.value.id)
  return api.postForm('/media/merge-upload', mergeForm)
}

async function handleUrlUpload(url) {
  if (!currentUser.value) {
    openAuthModal('login')
    return
  }

  const formData = new FormData()
  formData.append('url', url)
  formData.append('userId', currentUser.value.id)

  uploadBusy.value = true
  try {
    await api.postForm('/media/upload-url', formData)
    showToast('链接已导入。', 'success')
    await loadProjects(true)
  } catch (error) {
    showToast(error.message, 'error')
  } finally {
    uploadBusy.value = false
  }
}

async function deleteProject(projectId) {
  if (!currentUser.value || showDemoPreview.value) {
    return
  }

  const target = projects.value.find((item) => item.id === projectId)
  if (!target) {
    return
  }

  if (!window.confirm(`确认删除项目“${target.projectTitle || target.filename}”吗？`)) {
    return
  }

  try {
    await api.deleteText(`/media/delete?id=${projectId}&userId=${currentUser.value.id}`)
    showToast('项目已删除。', 'success')
    await loadProjects(true)
  } catch (error) {
    showToast(error.message, 'error')
  }
}

async function analyzeSelectedProject() {
  if (showDemoPreview.value) {
    showToast('先上传你的真实视频，再启动分析。', 'info')
    return
  }
  if (!currentUser.value || !selectedProjectId.value) {
    return
  }

  analysisPending.value = true
  try {
    const message = await api.postText(`/projects/${selectedProjectId.value}/analyze?userId=${currentUser.value.id}`)
    showToast(message || '分析任务已提交。', 'info')
    startProjectPolling()
    await refreshSelectedProject()
  } catch (error) {
    analysisPending.value = false
    showToast(error.message, 'error')
  }
}

function startProjectPolling() {
  clearInterval(projectPoller)
  projectPoller = window.setInterval(async () => {
    if (!selectedProjectId.value || !currentUser.value) {
      clearInterval(projectPoller)
      return
    }

    try {
      const detail = await api.getJson(`/projects/${selectedProjectId.value}?userId=${currentUser.value.id}`)
      projectDetail.value = detail
      syncProjectSnapshot(detail?.project)

      const status = detail?.project?.analysisStatus
      if (status === 'SEGMENTED' || status === 'REWRITTEN' || status === 'FAILED') {
        analysisPending.value = false
        clearInterval(projectPoller)
        showToast(status === 'FAILED' ? '分析失败，请检查后端服务。' : '分析完成。', status === 'FAILED' ? 'error' : 'success')
      }
    } catch (error) {
      analysisPending.value = false
      clearInterval(projectPoller)
      showToast(error.message, 'error')
    }
  }, 4000)
}

function syncProjectSnapshot(project) {
  if (!project) {
    return
  }
  projects.value = projects.value.map((item) => (item.id === project.id ? { ...item, ...project } : item))
}

function showToast(message, kind = 'info') {
  toast.message = message
  toast.kind = kind
  clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.message = ''
  }, 3000)
}

function initMotionSystem() {
  if (!appShellRef.value || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    return
  }

  motionEnabled = true
  magicCleanups = []
  motionContext = gsap.context(() => {
    const defaultEase = 'power4.out'

    gsap.set('.hero-title__line', {
      transformPerspective: 1600,
      transformOrigin: '50% 100%',
      willChange: 'transform, opacity',
    })

    gsap.set('.task-card, .upload-pane, .upload-board, .workspace__head, .workspace__intro, .workspace-panel', {
      willChange: 'transform, opacity',
    })

    const introTl = gsap.timeline({
      defaults: {
        ease: defaultEase,
      },
    })

    introTl
      .from('.topbar', {
        y: -24,
        autoAlpha: 0,
        duration: 1.3,
      })
      .from(
        '.prompt-wall__column',
        {
          yPercent: 8,
          autoAlpha: 0,
          duration: 2.1,
          stagger: 0.08,
        },
        0.06
      )
      .from(
        '.hero .section-kicker',
        {
          y: 30,
          autoAlpha: 0,
          duration: 1.05,
        },
        0.12
      )
      .from(
        '.hero-title__line',
        {
          yPercent: 132,
          rotateX: -76,
          scale: 1.18,
          autoAlpha: 0,
          duration: 1.95,
          stagger: 0.13,
        },
        0.2
      )
      .from(
        '.hero-subtitle',
        {
          y: 36,
          filter: 'blur(12px)',
          autoAlpha: 0,
          duration: 1.2,
        },
        0.82
      )
      .from(
        '.hero .upload-head',
        {
          y: 26,
          autoAlpha: 0,
          duration: 1.05,
        },
        0.98
      )
      .from(
        '.hero .upload-board',
        {
          y: 96,
          scale: 0.93,
          rotateX: 14,
          autoAlpha: 0,
          duration: 1.9,
        },
        0.92
      )
      .from(
        '.hero .upload-pane',
        {
          yPercent: 18,
          scale: 0.97,
          autoAlpha: 0,
          clipPath: 'inset(0 0 100% 0 round 30px)',
          duration: 1.65,
          stagger: 0.12,
        },
        1.02
      )
      .from(
        '.hero .upload-foot',
        {
          y: 22,
          autoAlpha: 0,
          duration: 1,
        },
        1.26
      )

    gsap.to('.bg-grainient', {
      yPercent: 6,
      scale: 1.04,
      ease: 'none',
      scrollTrigger: {
        trigger: '.app-shell',
        start: 'top top',
        end: 'bottom bottom',
        scrub: 1.8,
      },
    })

    gsap.to('.bg-texture', {
      yPercent: 8,
      ease: 'none',
      scrollTrigger: {
        trigger: '.app-shell',
        start: 'top top',
        end: 'bottom bottom',
        scrub: 1.4,
      },
    })

    gsap.to('.prompt-wall__column--1', {
      yPercent: -12,
      ease: 'none',
      scrollTrigger: {
        trigger: '.app-shell',
        start: 'top top',
        end: 'bottom bottom',
        scrub: 1.1,
      },
    })

    gsap.to('.prompt-wall__column--2', {
      yPercent: 10,
      ease: 'none',
      scrollTrigger: {
        trigger: '.app-shell',
        start: 'top top',
        end: 'bottom bottom',
        scrub: 1.4,
      },
    })

    gsap.to('.prompt-wall__column--3', {
      yPercent: -8,
      ease: 'none',
      scrollTrigger: {
        trigger: '.app-shell',
        start: 'top top',
        end: 'bottom bottom',
        scrub: 1.6,
      },
    })

    gsap.to('.prompt-wall__column--4', {
      yPercent: 14,
      ease: 'none',
      scrollTrigger: {
        trigger: '.app-shell',
        start: 'top top',
        end: 'bottom bottom',
        scrub: 1.9,
      },
    })

    gsap.to('.hero-upload', {
      y: -32,
      ease: 'none',
      scrollTrigger: {
        trigger: '.hero',
        start: 'top top',
        end: 'bottom top',
        scrub: 1.4,
      },
    })

    ScrollTrigger.create({
      trigger: '.workspace',
      start: 'top 78%',
      once: true,
      onEnter: () => {
        const workspaceTl = gsap.timeline({
          defaults: {
            ease: defaultEase,
          },
        })

        workspaceTl
          .from('.workspace .section-kicker', {
            y: 28,
            autoAlpha: 0,
            duration: 1,
          })
          .from(
            '.workspace .section-display',
            {
              yPercent: 116,
              rotateX: -70,
              scale: 1.12,
              autoAlpha: 0,
              duration: 1.75,
            },
            0.08
          )
          .from(
            '.workspace .workspace__lead, .workspace .workspace__head',
            {
              y: 42,
              autoAlpha: 0,
              duration: 1.05,
              stagger: 0.08,
            },
            0.52
          )
          .from(
            '.workspace .workspace-panel',
            {
              y: 48,
              autoAlpha: 0,
              duration: 1.2,
              stagger: 0.08,
            },
            0.58
          )
          .from(
            '.workspace .task-card',
            {
              y: 88,
              rotateX: -18,
              scale: 0.92,
              autoAlpha: 0,
              clipPath: 'inset(0 0 22% 0 round 22px)',
              duration: 1.5,
              stagger: 0.1,
            },
            0.62
          )
          .from(
            '.workspace .action-button',
            {
              y: 18,
              autoAlpha: 0,
              duration: 0.9,
              stagger: 0.04,
            },
            0.84
          )
      },
    })

    gsap.to('.workspace .task-grid-wrap', {
      y: -26,
      ease: 'none',
      scrollTrigger: {
        trigger: '.workspace',
        start: 'top bottom',
        end: 'bottom top',
        scrub: 1.35,
      },
    })

    magicCleanups = [
      setupMagicSurface('.hero .upload-pane', {
        glowColor: '201, 168, 255',
        glowRadius: 280,
        tiltAmount: 7,
        magnetism: 0.024,
      }),
      setupMagicSurface('.workspace .task-card', {
        glowColor: '132, 0, 255',
        glowRadius: 320,
        tiltAmount: 9,
        magnetism: 0.022,
      }),
      setupSectionSpotlight('.hero-upload', '.upload-pane', {
        glowColor: '201, 168, 255',
        spotlightRadius: 280,
        maxOpacity: 0.45,
      }),
      setupSectionSpotlight('.workspace', '.task-card', {
        glowColor: '132, 0, 255',
        spotlightRadius: 360,
        maxOpacity: 0.68,
      }),
    ]
  }, appShellRef.value)
}

function cascadeTaskCards() {
  const cards = gsap.utils.toArray('.workspace .task-card')
  if (!cards.length) {
    return
  }

  gsap.fromTo(
    cards,
    {
      autoAlpha: 0,
      y: 32,
      scale: 0.97,
    },
    {
      autoAlpha: 1,
      y: 0,
      scale: 1,
      duration: 1.1,
      ease: 'power3.out',
      stagger: 0.05,
      overwrite: true,
    }
  )
}

function destroyMotionSystem() {
  magicCleanups.forEach((cleanup) => cleanup?.())
  magicCleanups = []
  motionContext?.revert()
  motionContext = null
  motionEnabled = false
}
</script>

<style scoped>
.page-shell {
  padding: 4px;
}

.app-shell {
  position: relative;
  min-height: calc(100vh - 8px);
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: var(--shadow);
}

.bg-grainient,
.bg-texture,
.bg-mask {
  position: absolute;
  inset: 0;
}

.bg-grainient {
  z-index: 0;
  opacity: 0.94;
  filter: saturate(1.08) contrast(1.06);
}

.bg-texture {
  z-index: 1;
  opacity: 0.42;
  background:
    radial-gradient(circle at 52% 24%, rgba(201, 168, 255, 0.22), transparent 18%),
    radial-gradient(circle at 18% 76%, rgba(255, 255, 255, 0.035), transparent 24%),
    linear-gradient(180deg, rgba(4, 5, 8, 0.06), rgba(4, 5, 8, 0.28));
  mix-blend-mode: screen;
}

.bg-texture::before,
.bg-texture::after {
  content: '';
  position: absolute;
  inset: 0;
}

.bg-texture::before {
  background:
    linear-gradient(rgba(255, 255, 255, 0.018) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.018) 1px, transparent 1px);
  background-size: 72px 72px;
  opacity: 0.2;
}

.bg-texture::after {
  background:
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.06) 0.7px, transparent 0.8px);
  background-size: 10px 10px;
  mix-blend-mode: soft-light;
  opacity: 0.14;
}

.bg-mask {
  z-index: 3;
  background:
    radial-gradient(circle at 50% 16%, rgba(201, 168, 255, 0.12), transparent 20%),
    linear-gradient(180deg, rgba(7, 9, 13, 0.52), rgba(7, 9, 13, 0.9));
}

.topbar,
.main-layout {
  position: relative;
  z-index: 4;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 96px;
  padding: 0 64px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(10, 12, 17, 0.64);
  backdrop-filter: blur(10px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand__logo {
  font-family: var(--font-display);
  font-size: 2.2rem;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.brand__logo span {
  color: rgba(243, 244, 246, 0.88);
  font-weight: 400;
}

.brand__pro {
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--lime);
  color: #11130f;
  font-family: var(--font-display);
  font-weight: 800;
  box-shadow: 0 0 18px rgba(201, 168, 255, 0.45);
}

.topbar__right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.account {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--lime);
  font-family: var(--font-display);
}

.account__name {
  letter-spacing: 0.08em;
}

.icon-button,
.login-button {
  height: 44px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  background: rgba(18, 21, 27, 0.84);
  color: var(--text-soft);
}

.system-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 48px;
  padding: 0 18px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(18, 21, 27, 0.84);
}

.system-chip__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--lime);
  box-shadow: 0 0 12px rgba(201, 168, 255, 0.52);
}

.main-layout {
  padding: 54px 72px 72px;
}

.hero,
.workspace {
  position: relative;
  overflow: clip;
}

.hero {
  text-align: center;
}

.section-kicker {
  margin-bottom: 18px;
  color: rgba(243, 244, 246, 0.56);
  font-family: var(--font-display);
  font-size: 0.88rem;
  letter-spacing: 0.18em;
}

.hero-title {
  margin: 42px 0 12px;
}

.hero-title__line {
  display: block;
  font-family: var(--font-display);
  font-size: clamp(4rem, 9vw, 8.8rem);
  line-height: 0.92;
  letter-spacing: -0.055em;
  text-shadow: 0 0 26px rgba(255, 255, 255, 0.04);
}

.hero-subtitle {
  margin: 0;
  color: var(--text-faint);
  font-size: 0.98rem;
  letter-spacing: 0.12em;
}

.hero-upload {
  width: min(1140px, 100%);
  margin: 28px auto 0;
  transform-style: preserve-3d;
}

.workspace {
  margin-top: 86px;
}

.workspace__intro {
  margin-bottom: 28px;
}

.workspace__lead {
  max-width: 820px;
  margin: 18px 0 0;
  color: var(--text-soft);
  line-height: 1.85;
}

.section-display {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(3rem, 7vw, 6rem);
  line-height: 0.88;
  letter-spacing: -0.05em;
}

.workspace__head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding-bottom: 18px;
  margin-bottom: 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.14);
}

.workspace__title {
  display: flex;
  align-items: center;
  gap: 18px;
}

.workspace__title h3,
.workspace__title span {
  margin: 0;
}

.workspace__title h3 {
  font-size: 2.1rem;
  font-weight: 800;
}

.workspace__title span {
  padding: 8px 14px;
  border-radius: 10px;
  background: rgba(112, 118, 132, 0.28);
  color: rgba(243, 244, 246, 0.84);
  font-family: var(--font-display);
}

.workspace__actions {
  display: flex;
  gap: 12px;
}

.workspace__layout {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.workspace__side {
  display: grid;
  gap: 18px;
  position: sticky;
  top: 28px;
}

.workspace__main {
  min-width: 0;
}

.workspace-panel {
  padding: 24px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background:
    linear-gradient(180deg, rgba(15, 18, 23, 0.88), rgba(11, 13, 17, 0.96)),
    rgba(12, 14, 18, 0.94);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.02),
    0 20px 44px rgba(0, 0, 0, 0.22);
}

.workspace-panel--active {
  border-color: rgba(201, 168, 255, 0.22);
}

.workspace-panel__eyebrow {
  display: inline-flex;
  margin-bottom: 14px;
  color: rgba(243, 244, 246, 0.46);
  font-family: var(--font-display);
  font-size: 0.82rem;
  letter-spacing: 0.18em;
}

.workspace-panel h3 {
  margin: 0;
  font-size: 1.6rem;
  line-height: 1.2;
  word-break: break-word;
}

.workspace-panel__status {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
  color: var(--text-faint);
}

.workspace-panel__badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(201, 168, 255, 0.28);
  color: var(--lime);
  font-family: var(--font-display);
  font-size: 0.88rem;
}

.workspace-panel__summary {
  margin: 18px 0 0;
  color: rgba(243, 244, 246, 0.86);
  line-height: 1.85;
}

.workspace-panel__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.workspace-panel__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(243, 244, 246, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.workspace-panel__actions {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}

.workspace-flow {
  display: grid;
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.workspace-flow__item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  align-items: start;
}

.workspace-flow__index {
  display: inline-grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(201, 168, 255, 0.26);
  color: var(--lime);
  font-family: var(--font-display);
}

.workspace-flow strong {
  display: block;
  margin-bottom: 6px;
  font-size: 1rem;
}

.workspace-flow p {
  margin: 0;
  color: var(--text-soft);
  line-height: 1.7;
}

.toolbar-button {
  height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(18, 21, 27, 0.86);
  color: var(--text-soft);
}

.toolbar-button--lime {
  border-color: rgba(201, 168, 255, 0.34);
  color: var(--lime);
}

.toolbar-button:disabled {
  opacity: 0.48;
  cursor: not-allowed;
}

.task-grid-wrap {
  transform-style: preserve-3d;
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.22s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.toast {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 90;
  padding: 14px 18px;
  border-radius: 10px;
  color: #0c0f0c;
  font-weight: 700;
}

:deep(.magic-section-spotlight) {
  position: fixed;
  width: 820px;
  height: 820px;
  border-radius: 999px;
  pointer-events: none;
  transform: translate(-50%, -50%);
  mix-blend-mode: screen;
  opacity: 0;
  z-index: 6;
  will-change: transform, opacity;
}

:deep(.magic-ripple) {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  z-index: 5;
}

.toast--info,
.toast--success {
  background: var(--lime);
}

.toast--error {
  background: var(--danger);
}

@media (prefers-reduced-motion: reduce) {
  .hero-upload,
  .bg-grainient,
  .bg-texture,
  .task-grid-wrap {
    transform: none !important;
  }
}

@media (max-width: 1200px) {
  .topbar {
    padding: 0 24px;
  }

  .main-layout {
    padding: 30px 20px 40px;
  }

  .workspace__layout {
    grid-template-columns: 1fr;
  }

  .workspace__side {
    position: static;
  }
}

@media (max-width: 760px) {
  .topbar,
  .workspace__head {
    display: grid;
    justify-content: stretch;
  }

  .topbar {
    padding: 16px;
  }

  .hero-title__line {
    font-size: clamp(3rem, 16vw, 5.4rem);
  }

  .workspace__title {
    justify-content: space-between;
  }

  .section-display {
    font-size: clamp(2.6rem, 16vw, 4.4rem);
  }

  .workspace-panel {
    padding: 20px;
  }
}
</style>
