<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { api } from './api'

const sessions = ref([])
const current = ref(null)
const message = ref('')
const search = ref('')
const loading = ref(false)
const generating = ref(false)
const error = ref('')
const showCreate = ref(false)
const chatBody = ref(null)
const draft = ref({ pageCount: 10, requirement: '' })

const filteredSessions = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  if (!keyword) return sessions.value
  return sessions.value.filter((item) => item.title.toLowerCase().includes(keyword))
})

const statusText = {
  CLARIFYING: '需求沟通中',
  GENERATING: '正在生成',
  GENERATED: '已生成',
  REVISING: '等待生成新版'
}

async function loadSessions() {
  try {
    sessions.value = await api.listSessions()
  } catch (e) {
    showError(e)
  }
}

async function openSession(id) {
  loading.value = true
  error.value = ''
  try {
    current.value = await api.getSession(id)
    await scrollToBottom()
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}

async function createSession() {
  if (!draft.value.requirement.trim() || loading.value) return
  loading.value = true
  error.value = ''
  try {
    current.value = await api.createSession({
      pageCount: Number(draft.value.pageCount),
      requirement: draft.value.requirement.trim()
    })
    draft.value = { pageCount: 10, requirement: '' }
    showCreate.value = false
    await loadSessions()
    await scrollToBottom()
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}

async function sendMessage() {
  const text = message.value.trim()
  if (!text || !current.value || loading.value || generating.value) return
  message.value = ''
  current.value.messages.push({
    role: 'user',
    content: text,
    createdAt: new Date().toISOString()
  })
  loading.value = true
  await scrollToBottom()
  try {
    const response = await api.sendMessage(current.value.id, text)
    current.value.messages.push({
      role: 'assistant',
      content: response.reply,
      createdAt: new Date().toISOString()
    })
    current.value.status = current.value.deckSpec ? 'REVISING' : 'CLARIFYING'
    await loadSessions()
    await scrollToBottom()
  } catch (e) {
    current.value.messages.pop()
    message.value = text
    showError(e)
  } finally {
    loading.value = false
  }
}

async function generatePpt() {
  if (!current.value || generating.value) return
  generating.value = true
  error.value = ''
  try {
    const result = await api.generate(current.value.id)
    current.value = await api.getSession(current.value.id)
    await loadSessions()
    await scrollToBottom()
    window.location.href = result.downloadUrl
  } catch (e) {
    showError(e)
  } finally {
    generating.value = false
  }
}

async function deleteSession(event, id) {
  event.stopPropagation()
  if (!window.confirm('确定删除这条会话记录吗？PPT 文件不会自动删除。')) return
  try {
    await api.removeSession(id)
    if (current.value?.id === id) current.value = null
    await loadSessions()
  } catch (e) {
    showError(e)
  }
}

function downloadUrl(version) {
  return `/api/sessions/${current.value.id}/versions/${version}/download`
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  const today = new Date()
  if (date.toDateString() === today.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function showError(e) {
  error.value = e.message || '发生未知错误'
  window.setTimeout(() => {
    if (error.value === e.message) error.value = ''
  }, 5000)
}

async function scrollToBottom() {
  await nextTick()
  if (chatBody.value) chatBody.value.scrollTop = chatBody.value.scrollHeight
}

function handleEnter(event) {
  if (event.isComposing) return
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

onMounted(loadSessions)
</script>

<template>
  <main class="workspace">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark"><span></span><span></span><span></span></div>
        <div>
          <strong>DeckFlow</strong>
          <small>AI PRESENTATION STUDIO</small>
        </div>
      </div>

      <button class="new-button" @click="showCreate = true">
        <span class="plus">＋</span>
        新建演示文稿
      </button>

      <label class="search-box">
        <span>⌕</span>
        <input v-model="search" placeholder="搜索历史会话" aria-label="搜索历史会话" />
      </label>

      <div class="history-heading">
        <span>最近会话</span>
        <b>{{ filteredSessions.length }}</b>
      </div>
      <div class="session-list">
        <button
          v-for="item in filteredSessions"
          :key="item.id"
          class="session-item"
          :class="{ active: current?.id === item.id }"
          @click="openSession(item.id)"
        >
          <span class="session-icon">▤</span>
          <span class="session-copy">
            <strong>{{ item.title }}</strong>
            <small>{{ item.pageCount }} 页 · {{ statusText[item.status] || item.status }}</small>
          </span>
          <span class="session-meta">
            <small>{{ formatDate(item.updatedAt) }}</small>
            <span class="delete" title="删除会话" @click="deleteSession($event, item.id)">×</span>
          </span>
        </button>
        <p v-if="!filteredSessions.length" class="empty-history">还没有会话，从一份新 PPT 开始吧。</p>
      </div>

      <div class="sidebar-footer">
        <span class="status-dot"></span>
        <span>DeepSeek V4 Flash</span>
        <small>Redis 已连接</small>
      </div>
    </aside>

    <section v-if="current" class="conversation">
      <header class="topbar">
        <div>
          <span class="eyebrow">CURRENT PROJECT</span>
          <h1>{{ current.title }}</h1>
        </div>
        <div class="topbar-actions">
          <span class="page-badge">{{ current.pageCount }} PAGES</span>
          <button class="generate-small" :disabled="generating || loading" @click="generatePpt">
            {{ generating ? '生成中…' : current.versions.length ? '生成新版本' : '开始生成' }}
          </button>
        </div>
      </header>

      <div ref="chatBody" class="chat-body">
        <div class="project-strip">
          <div class="project-number">{{ String(current.pageCount).padStart(2, '0') }}</div>
          <div>
            <span>目标页数</span>
            <strong>{{ current.pageCount }} 页演示文稿</strong>
          </div>
          <div class="strip-divider"></div>
          <div>
            <span>当前阶段</span>
            <strong>{{ statusText[current.status] || current.status }}</strong>
          </div>
          <div v-if="current.versions.length" class="version-count">
            {{ current.versions.length }} 个版本
          </div>
        </div>

        <article
          v-for="(item, index) in current.messages"
          :key="`${item.createdAt}-${index}`"
          class="message-row"
          :class="item.role"
        >
          <div class="avatar">{{ item.role === 'assistant' ? 'D' : '你' }}</div>
          <div class="message-content">
            <div class="message-label">
              <strong>{{ item.role === 'assistant' ? 'DeckFlow AI' : '你' }}</strong>
              <time>{{ formatDate(item.createdAt) }}</time>
            </div>
            <div class="bubble">{{ item.content }}</div>
          </div>
        </article>

        <article v-if="loading" class="message-row assistant">
          <div class="avatar">D</div>
          <div class="message-content">
            <div class="message-label"><strong>DeckFlow AI</strong></div>
            <div class="bubble typing"><i></i><i></i><i></i></div>
          </div>
        </article>

        <div v-if="current.versions.length" class="versions">
          <div class="versions-title">
            <span>生成记录</span>
            <small>所有历史版本均可下载</small>
          </div>
          <a
            v-for="version in [...current.versions].reverse()"
            :key="version.version"
            class="version-card"
            :href="downloadUrl(version.version)"
          >
            <span class="ppt-icon">P</span>
            <span>
              <strong>版本 {{ version.version }}</strong>
              <small>{{ version.fileName }}</small>
            </span>
            <b>下载 ↓</b>
          </a>
        </div>
      </div>

      <footer class="composer-area">
        <div class="quick-actions">
          <button @click="message = '请把整体风格调整得更专业、简洁一些'">更专业</button>
          <button @click="message = '请增加数据和案例支撑'">增加案例</button>
          <button @click="message = '请精简每页文字，突出核心观点'">精简内容</button>
        </div>
        <div class="composer">
          <textarea
            v-model="message"
            rows="1"
            placeholder="继续补充要求，或描述你希望修改的地方…"
            aria-label="输入消息"
            @keydown="handleEnter"
          ></textarea>
          <button :disabled="!message.trim() || loading || generating" aria-label="发送消息" @click="sendMessage">
            ↑
          </button>
        </div>
        <small class="composer-hint">Enter 发送 · Shift + Enter 换行 · AI 生成内容请人工复核</small>
      </footer>
    </section>

    <section v-else class="welcome">
      <div class="welcome-grid"></div>
      <div class="welcome-content">
        <span class="welcome-tag"><i></i> SPRING AI × DEEPSEEK</span>
        <h1>把想法，变成一份<br /><em>有说服力的演示。</em></h1>
        <p>告诉我主题与目标。我们会通过几轮对话厘清内容，然后为你生成可继续修改的 PowerPoint。</p>
        <button @click="showCreate = true">创建第一份演示文稿 <span>→</span></button>
        <div class="feature-row">
          <span><b>01</b> 多轮需求澄清</span>
          <span><b>02</b> 可编辑 PPTX</span>
          <span><b>03</b> 版本持续迭代</span>
        </div>
      </div>
      <div class="deck-preview">
        <div class="preview-card rear"></div>
        <div class="preview-card middle"></div>
        <div class="preview-card front">
          <span>STRATEGY / 2026</span>
          <h2>从洞察<br />到行动</h2>
          <i></i>
          <small>DECKFLOW AI</small>
        </div>
      </div>
    </section>

    <div v-if="showCreate" class="modal-backdrop" @mousedown.self="showCreate = false">
      <form class="create-modal" @submit.prevent="createSession">
        <div class="modal-head">
          <div>
            <span class="eyebrow">NEW PRESENTATION</span>
            <h2>从一个清晰的目标开始</h2>
          </div>
          <button type="button" aria-label="关闭" @click="showCreate = false">×</button>
        </div>

        <label class="field-label">你想制作什么 PPT？</label>
        <textarea
          v-model="draft.requirement"
          rows="6"
          autofocus
          placeholder="例如：为公司管理层制作一份新能源汽车市场进入策略汇报，重点分析行业机会、竞争格局和未来三年行动计划，风格专业、简洁、有数据支撑。"
        ></textarea>

        <div class="page-select-row">
          <div>
            <label class="field-label" for="pages">目标页数</label>
            <p>包含封面和总结页，后续仍可调整。</p>
          </div>
          <div class="stepper">
            <button type="button" @click="draft.pageCount = Math.max(3, draft.pageCount - 1)">−</button>
            <input id="pages" v-model.number="draft.pageCount" type="number" min="3" max="30" />
            <span>页</span>
            <button type="button" @click="draft.pageCount = Math.min(30, draft.pageCount + 1)">＋</button>
          </div>
        </div>

        <button class="start-chat" :disabled="!draft.requirement.trim() || loading" type="submit">
          {{ loading ? '正在理解你的需求…' : '开始与 AI 沟通' }}
          <span>→</span>
        </button>
      </form>
    </div>

    <div v-if="error" class="toast">
      <strong>操作没有完成</strong>
      <span>{{ error }}</span>
      <button @click="error = ''">×</button>
    </div>

    <div v-if="generating" class="generating-overlay">
      <div class="generation-card">
        <div class="generation-mark"><span></span><span></span><span></span></div>
        <span class="eyebrow">CRAFTING YOUR DECK</span>
        <h2>正在编排内容与版式</h2>
        <p>DeepSeek 正在梳理完整会话，生成 {{ current.pageCount }} 页可编辑演示文稿。</p>
        <div class="progress"><i></i></div>
      </div>
    </div>
  </main>
</template>

