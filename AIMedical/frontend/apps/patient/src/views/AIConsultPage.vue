<template>
  <div class="consult-container">
    <div class="page-header">
      <el-button type="default" @click="router.push('/home')">← 返回首页</el-button>
      <h2>AI 病情咨询</h2>
    </div>

    <!-- 免责声明 -->
    <DisclaimerBanner />

    <!-- 聊天区域 -->
    <div class="chat-area" ref="chatAreaRef">
      <div v-if="messages.length === 0" class="chat-empty">
        <span class="empty-icon">🤖</span>
        <p>您好，我是 AI 健康助手，有什么可以帮您？</p>
        <p class="empty-hint">可以向我描述症状、检查结果，或咨询健康问题</p>
      </div>

      <div
        v-for="(msg, idx) in messages"
        :key="msg.id"
        class="chat-message"
        :class="msg.role"
      >
        <div class="msg-avatar">
          <span v-if="msg.role === 'user'">👤</span>
          <span v-else>🤖</span>
        </div>
        <div class="msg-bubble" :class="msg.role">
          <div class="msg-text">{{ msg.text }}</div>

          <!-- 相关问题推荐 -->
          <div v-if="msg.relatedQuestions?.length" class="related-questions">
            <p class="related-title">您可能还想问：</p>
            <el-button
              v-for="q in msg.relatedQuestions"
              :key="q"
              size="small"
              text
              type="primary"
              @click="sendMessage(q)"
            >
              {{ q }}
            </el-button>
          </div>

          <!-- 错误重试 -->
          <div v-if="msg.error" class="msg-error">
            <span>{{ msg.text }}</span>
            <el-button size="small" type="primary" text @click="retryMessage(idx)">
              重试
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="loading" class="chat-message ai">
        <div class="msg-avatar"><span>🤖</span></div>
        <div class="msg-bubble ai">
          <span class="typing-dots">正在输入…</span>
        </div>
      </div>
    </div>

    <!-- 底部输入框 -->
    <div class="input-bar">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        :maxlength="500"
        placeholder="请输入您的问题…"
        :disabled="loading"
        @keydown.enter.exact.prevent="handleSend"
      />
      <div class="input-actions">
        <span class="char-count">{{ inputText.length }} / 500</span>
        <el-button
          type="primary"
          :disabled="!inputText.trim() || loading"
          :loading="loading"
          @click="handleSend"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { consultApi, type ConsultRequest, type ConsultResponse, type BusinessError } from '@aimedical/shared'
import DisclaimerBanner from '../components/DisclaimerBanner.vue'

interface ChatMessage {
  id: number
  role: 'user' | 'ai'
  text: string
  relatedQuestions?: string[]
  error?: boolean
}

let msgSeq = 0

const router = useRouter()
const chatAreaRef = ref<HTMLElement>()
const inputText = ref('')
const loading = ref(false)
const sessionId = ref<string | null>(null)

const messages = ref<ChatMessage[]>([])

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''

  messages.value.push({ id: ++msgSeq, role: 'user', text })
  scrollToBottom()

  loading.value = true
  try {
    const req: ConsultRequest = {
      question: text,
      session_id: sessionId.value ?? undefined,
    }
    const result = await consultApi.ask(req)

    if ((result as BusinessError).isBusinessError) {
      const err = result as BusinessError
      if (err.code === 'QA_AI_UNAVAILABLE') {
        messages.value.push({
          id: ++msgSeq,
          role: 'ai',
          text: '暂不可用，请稍后重试',
          error: true,
        })
      } else {
        messages.value.push({ id: ++msgSeq, role: 'ai', text: err.message || '服务异常', error: true })
      }
    } else {
      const data = result as ConsultResponse
      if (data.session_id) {
        sessionId.value = data.session_id
      }
      messages.value.push({
        id: ++msgSeq,
        role: 'ai',
        text: data.answer,
        relatedQuestions: data.related_questions,
      })
    }
  } catch {
    messages.value.push({
      id: ++msgSeq,
      role: 'ai',
      text: '网络异常，请稍后重试',
      error: true,
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function sendMessage(text: string) {
  inputText.value = text
  handleSend()
}

function retryMessage(idx: number) {
  // Remove the error message and resend the preceding user message
  const prevUser = messages.value.slice(0, idx).filter(m => m.role === 'user').pop()
  messages.value = messages.value.slice(0, idx)
  if (prevUser) {
    inputText.value = prevUser.text
    handleSend()
  }
}

function scrollToBottom() {
  nextTick(() => {
    const el = chatAreaRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.consult-container {
  max-width: 780px;
  margin: 0 auto;
  padding: 24px 16px 60px;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 48px);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.page-header h2 {
  margin: 0;
  flex: 1;
}

.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
  margin-bottom: 12px;
}

.chat-empty {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-icon {
  font-size: 48px;
}

.empty-hint {
  font-size: 13px;
  margin-top: 4px;
}

.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}

.msg-bubble {
  max-width: 75%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.msg-bubble.user {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg-bubble.ai {
  background: #f5f7fa;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.msg-text {
  white-space: pre-wrap;
}

.typing-dots {
  color: #909399;
  font-style: italic;
}

.related-questions {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #e4e7ed;
}

.related-title {
  margin: 0 0 4px 0;
  font-size: 12px;
  color: #909399;
}

.msg-error {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f56c6c;
}

.input-bar {
  flex-shrink: 0;
  border-top: 1px solid #ebeef5;
  padding-top: 10px;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.char-count {
  font-size: 12px;
  color: #909399;
}
</style>
