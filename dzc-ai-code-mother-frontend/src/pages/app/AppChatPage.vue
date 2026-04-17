<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" color="green" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
      </div>
      <div class="header-right">
        <a-button
          v-if="isOwner && !isReadonly"
          type="default"
          @click="shareModalVisible = true"
          :disabled="messages.length < 2"
        >
          <template #icon>
            <ShareAltOutlined />
          </template>
          分享
        </a-button>
        <a-button
          v-if="!isReadonly"
          type="default"
          @click="showVersionHistory"
          :disabled="!isOwner"
        >
          <template #icon>
            <HistoryOutlined />
          </template>
          历史版本
        </a-button>
        <a-button v-if="!isReadonly" type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button
          v-if="!isReadonly"
          type="primary"
          ghost
          @click="downloadCode"
          :loading="downloading"
          :disabled="!isOwner"
        >
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <a-button v-if="!isReadonly" type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar || defaultAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="chatAvatar" />
              </div>
              <div class="message-content">
                <MarkdownRenderer v-if="message.content" :content="message.content" />
                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert
          v-if="selectedElementInfo"
          class="selected-element-alert"
          type="info"
          closable
          @close="clearSelectedElement"
        >
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div v-if="!isReadonly" class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea
                v-model:value="userInput"
                :placeholder="getInputPlaceholder()"
                :rows="4"
                :maxlength="1000"
                @keydown.enter.prevent="sendMessage"
                :disabled="isGenerating || !isOwner"
              />
            </a-tooltip>
            <a-textarea
              v-else
              v-model:value="userInput"
              :placeholder="getInputPlaceholder()"
              :rows="4"
              :maxlength="1000"
              @keydown.enter.prevent="sendMessage"
              :disabled="isGenerating"
            />
            <div class="input-actions">
              <a-button
                type="primary"
                @click="sendMessage"
                :loading="isGenerating"
                :disabled="!isOwner"
              >
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>生成后的网页展示</h3>
          <div v-if="showInteraction" class="interaction-buttons">
            <a-button type="text" @click="handleToggleLike" :class="{ liked: isLiked }">
              <template #icon>
                <LikeOutlined
                  :style="{
                    color: isLiked ? '#ff4d4f' : '#888',
                    fontSize: '18px',
                    transition: 'all 0.3s',
                  }"
                />
              </template>
              <span :style="{ color: isLiked ? '#ff4d4f' : '#888' }">{{ likeCount }}</span>
            </a-button>
            <a-button type="text" @click="handleToggleCollect" :class="{ collected: isCollected }">
              <template #icon>
                <StarOutlined
                  :style="{
                    color: isCollected ? '#faad14' : '#888',
                    fontSize: '18px',
                    transition: 'all 0.3s',
                  }"
                />
              </template>
              <span :style="{ color: isCollected ? '#faad14' : '#888' }">{{ collectCount }}</span>
            </a-button>
          </div>
          <div class="preview-actions">
            <a-button
              v-if="isOwner && previewUrl"
              type="link"
              :danger="isEditMode"
              @click="toggleEditMode"
              :class="{ 'edit-mode-active': isEditMode }"
              style="padding: 0; height: auto; margin-right: 12px"
            >
              <template #icon>
                <EditOutlined />
              </template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </a-button>
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
            <a-button v-if="showInteraction" type="link" @click="openCommentDrawer">
              <template #icon>
                <MessageOutlined />
              </template>
              评论
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="!previewUrl && !isGenerating" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <div v-else-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <iframe
            v-else
            :src="previewUrl"
            class="preview-iframe"
            frameborder="0"
            @load="onIframeLoad"
          ></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal
      v-model:open="appDetailVisible"
      :app="appInfo"
      :show-actions="isOwner || isAdmin"
      @edit="editApp"
      @delete="deleteApp"
    />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal
      v-model:open="deployModalVisible"
      :deploy-url="deployUrl"
      @open-site="openDeployedSite"
    />

    <!-- 版本历史抽屉 -->
    <VersionHistoryDrawer
      v-model:open="versionHistoryVisible"
      :app-id="appId"
      @rollback-success="handleRollbackSuccess"
    />

    <ShareToChatRoomModal
      v-model:open="shareModalVisible"
      :app-id="appId"
      :app-name="appInfo?.appName"
      :cover="appInfo?.cover"
      :code-gen-type="appInfo?.codeGenType"
      :version-no="appInfo?.versionNo"
      @share-success="handleShareSuccess"
    />

    <AppCommentDrawer
      v-model:open="commentDrawerVisible"
      :app-id="appId as number"
      :app-name="appInfo?.appName"
      :highlight-comment-id="highlightCommentId"
      @comment-added="handleCommentAdded"
    />
  </div>
</template>

<script setup lang="ts">
import {
    deleteApp as deleteAppApi,
    deployApp as deployAppApi,
    getAppVoById,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { getInteractionStatus, toggleCollect, toggleLike } from '@/api/interactionController'
import request from '@/request'
import { useLoginUserStore } from '@/stores/loginUser'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import { message } from 'ant-design-vue'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { default as chatAvatar } from '@/assets/12.jpg'
import { default as defaultAvatar } from '@/assets/aiAvatar.png'
import AppCommentDrawer from '@/components/AppCommentDrawer.vue'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import ShareToChatRoomModal from '@/components/ShareToChatRoomModal.vue'
import VersionHistoryDrawer from '@/components/VersionHistoryDrawer.vue'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

import {
    CloudUploadOutlined,
    DownloadOutlined,
    EditOutlined,
    ExportOutlined,
    HistoryOutlined,
    InfoCircleOutlined,
    LikeOutlined,
    MessageOutlined,
    SendOutlined,
    ShareAltOutlined,
    StarOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<any>()

// 对话相关
interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const isReadonly = computed(() => {
  return route.query.readonly === 'true'
})

const showInteraction = computed(() => {
  return route.query.showInteraction === 'true'
})

const exitReadonly = () => {
  router.replace({ query: {} })
}

// 应用详情相关
const appDetailVisible = ref(false)

// 版本历史相关
const versionHistoryVisible = ref(false)

// 评论抽屉相关
const commentDrawerVisible = ref(false)
const highlightCommentId = ref<number | undefined>(undefined)

// 互动状态
const likeCount = ref(0)
const collectCount = ref(0)
const isLiked = ref(false)
const isCollected = ref(false)

// 分享相关
const shareModalVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

// 显示版本历史
const showVersionHistory = () => {
  versionHistoryVisible.value = true
}

// 打开群聊创建/加入页面（新窗口）
const goToChatRoom = () => {
  const url = window.location.origin + '/chat/create-or-join'
  window.open(url, '_blank')
}

// 版本回退成功后刷新页面
const handleRollbackSuccess = () => {
  fetchAppInfo()
  loadChatHistory()
}

const handleShareSuccess = (roomId: string | number, roomName: string) => {
  message.success(`已分享到 ${roomName}`)
}

// 互动相关方法
const openCommentDrawer = () => {
  commentDrawerVisible.value = true
}

const handleCommentAdded = () => {
  fetchInteractionStatus()
}

const fetchInteractionStatus = async () => {
  if (!appId.value) return
  try {
    const res = await getInteractionStatus(appId.value as number)
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      likeCount.value = data.likeCount || 0
      collectCount.value = data.collectCount || 0
      isLiked.value = data.isLiked || false
      isCollected.value = data.isCollected || false
    }
  } catch (error) {
    console.error('获取互动状态失败:', error)
  }
}

const handleToggleLike = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }
  try {
    const res = await toggleLike(appId.value as number)
    if (res.data.code === 0 && res.data.data !== undefined) {
      isLiked.value = res.data.data
      likeCount.value += isLiked.value ? 1 : -1
      if (isLiked.value) {
        message.success({
          content: '点赞成功！',
          icon: '👍',
        })
      } else {
        message.info('已取消点赞')
      }
    }
  } catch (error) {
    message.error('操作失败')
  }
}

const handleToggleCollect = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }
  try {
    const res = await toggleCollect(appId.value as number)
    if (res.data.code === 0 && res.data.data !== undefined) {
      isCollected.value = res.data.data
      collectCount.value += isCollected.value ? 1 : -1
      if (isCollected.value) {
        message.success({
          content: '收藏成功！',
          icon: '⭐',
        })
      } else {
        message.info('已取消收藏')
      }
    }
  } catch (error) {
    message.error('操作失败')
  }
}

// 滚动到指定的消息索引
const scrollToMessageIndex = (index: number) => {
  if (messagesContainer.value) {
    const messageElements = messagesContainer.value.querySelectorAll('.message-item')
    if (messageElements[index]) {
      const containerRect = messagesContainer.value.getBoundingClientRect()
      const elementRect = messageElements[index].getBoundingClientRect()
      const scrollTop = messagesContainer.value.scrollTop
      const targetScrollTop = scrollTop + elementRect.top - containerRect.top - 20
      messagesContainer.value.scrollTo({
        top: targetScrollTop,
        behavior: 'smooth',
      })
      return true
    }
  }
  return false
}

// 根据 versionIndex 加载消息并滚动
const handleVersionIndexScroll = async (versionIndex: number, retryCount = 0) => {
  const maxRetries = 10
  const targetMessageIndex = versionIndex * 2

  if (messages.value[targetMessageIndex]) {
    nextTick(() => {
      scrollToMessageIndex(targetMessageIndex)
    })
    return
  }

  if (hasMoreHistory.value && retryCount < maxRetries) {
    await loadMoreHistory()
    await new Promise((resolve) => setTimeout(resolve, 300))
    await handleVersionIndexScroll(versionIndex, retryCount + 1)
    return
  }

  if (messages.value[targetMessageIndex]) {
    nextTick(() => {
      scrollToMessageIndex(targetMessageIndex)
    })
  }
}

// 监听路由参数 versionIndex
watch(
  () => route.query.versionIndex,
  async (newVersionIndex) => {
    if (newVersionIndex && !isNaN(Number(newVersionIndex))) {
      const versionIndex = Number(newVersionIndex)
      if (!historyLoaded.value) {
        await new Promise((resolve) => setTimeout(resolve, 1000))
      }
      await handleVersionIndexScroll(versionIndex)
      router.replace({ query: {} })
    }
  },
  { immediate: true },
)

// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
          .map((chat) => ({
            type: (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai',
            content: chat.message || '',
            createTime: chat.createTime,
          }))
          .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
        appInfo.value.initPrompt &&
        isOwner.value &&
        messages.value.length === 0 &&
        historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false

  try {
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    let fullContent = ''

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        // 拼接内容
        if (content !== undefined && content !== null) {
          fullContent += content
          messages.value[aiMessageIndex].content = fullContent
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理done事件
    eventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // 延迟更新预览，确保后端已完成处理
      setTimeout(async () => {
        await fetchAppInfo()
        updatePreview()
      }, 1000)
    })

    // 处理business-error事件（后端限流等错误）
    eventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        console.error('SSE业务错误事件:', errorData)

        // 显示具体的错误信息
        const errorMessage = errorData.message || '生成过程中出现错误'
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()
      } catch (parseError) {
        console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
        handleError(new Error('服务器返回错误'), aiMessageIndex)
      }
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()
  fetchInteractionStatus()

  // 检查是否需要自动打开评论抽屉
  if (route.query.openCommentDrawer === 'true') {
    commentDrawerVisible.value = true
  }
  if (route.query.highlightCommentId) {
    highlightCommentId.value = Number(route.query.highlightCommentId)
  }

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  // EventSource 会在组件卸载时自动清理
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background:
    radial-gradient(ellipse at 10% 90%, rgba(34, 197, 94, 0.08) 0, transparent 50%),
    radial-gradient(ellipse at 90% 10%, rgba(5, 150, 105, 0.06) 0, transparent 50%), #f0fdf4;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.72);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(16px);
  box-shadow: 0 4px 20px rgba(21, 128, 61, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-gen-type-tag {
  font-size: 12px;
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-primary-dark, #166534);
  font-family: 'Lora', serif;
}

.header-right {
  display: flex;
  gap: 8px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.65);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(16px);
  box-shadow: 0 4px 24px rgba(21, 128, 61, 0.06);
  overflow: hidden;
}

.messages-container {
  flex: 0.9;
  padding: 20px;
  overflow-y: auto;
  scroll-behavior: smooth;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(187, 247, 208, 0.4) 0, transparent 50%),
    radial-gradient(ellipse at 80% 100%, rgba(167, 243, 208, 0.3) 0, transparent 50%),
    linear-gradient(
      180deg,
      rgba(240, 253, 244, 0.6) 0%,
      #f0fdf4 30%,
      #f0fdf4 70%,
      rgba(240, 253, 244, 0.6) 100%
    );
}

.message-item {
  margin-bottom: 16px;
  animation: messageSlideIn 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes messageSlideIn {
  0% {
    opacity: 0;
    transform: translateY(14px);
  }
  50% {
    opacity: 1;
    transform: translateY(-3px);
  }
  70% {
    transform: translateY(1px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.user-message .message-content {
  transition: box-shadow 0.3s ease;
  box-shadow: 0 2px 6px rgba(21, 128, 61, 0.08);
}

.user-message:hover .message-content {
  box-shadow:
    0 8px 24px rgba(21, 128, 61, 0.2),
    0 2px 8px rgba(21, 128, 61, 0.12);
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.ai-message .message-content {
  transition: box-shadow 0.3s ease;
  box-shadow: 0 2px 6px rgba(21, 128, 61, 0.06);
}

.ai-message:hover .message-content {
  box-shadow:
    0 8px 24px rgba(21, 128, 61, 0.14),
    0 2px 8px rgba(21, 128, 61, 0.08);
}

.message-content {
  max-width: 70%;
  padding: 12px 18px;
  border-radius: 18px;
  line-height: 1.6;
  word-wrap: break-word;
  font-size: 14px;
}

.user-message .message-content {
  background: linear-gradient(
    135deg,
    var(--color-primary, #15803d),
    var(--color-primary-dark, #166534)
  );
  color: white;
  border-bottom-right-radius: 6px;
  box-shadow: 0 4px 12px rgba(21, 128, 61, 0.25);
}

.ai-message .message-content {
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.9), rgba(220, 252, 231, 0.85));
  color: var(--color-primary-dark, #166534);
  padding: 14px 18px;
  border-bottom-left-radius: 6px;
  border: 1px solid rgba(187, 247, 208, 0.5);
  box-shadow: 0 2px 8px rgba(21, 128, 61, 0.08);
}

.message-avatar {
  flex-shrink: 0;
}

.ai-avatar-digital {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(134, 239, 172, 0.9), rgba(21, 128, 61, 0.9));
  position: relative;
  box-shadow:
    0 0 12px rgba(34, 197, 94, 0.35),
    inset 0 0 14px rgba(134, 239, 172, 0.6);
  animation: ai-avatar-float 4s ease-in-out infinite;
  overflow: hidden;
}

.ai-avatar-digital .air-wave {
  position: absolute;
  width: 120%;
  height: 120%;
  top: -10%;
  left: -10%;
  border: 1px solid rgba(250, 250, 250, 0.5);
  border-radius: 50%;
  animation: ai-avatar-pulse 3s ease-in-out infinite;
}

.ai-avatar-digital .air-core {
  width: 48%;
  height: 48%;
  border-radius: 50%;
  background: conic-gradient(from 90deg, rgba(34, 197, 94, 0.8), rgba(5, 150, 105, 0.8));
  animation: ai-avatar-rotate 5s linear infinite;
  box-shadow: 0 0 10px rgba(34, 197, 94, 0.8);
}

@keyframes ai-avatar-pulse {
  0% {
    transform: scale(0.75);
    opacity: 0.4;
  }
  50% {
    transform: scale(1);
    opacity: 0.65;
  }
  100% {
    transform: scale(0.85);
    opacity: 0.4;
  }
}

@keyframes ai-avatar-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes ai-avatar-float {
  0%,
  100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-4px);
  }
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-primary, #15803d);
  font-size: 13px;
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

.load-more-container :deep(.ant-btn-link) {
  color: var(--color-primary, #15803d);
}

/* 输入区域 */
.input-container {
  padding: 16px 20px;
  background: rgba(240, 253, 244, 0.5);
  border-top: 1px solid rgba(187, 247, 208, 0.3);
}

.input-wrapper {
  position: relative;
  background: rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-md);
  border: 1px solid rgba(187, 247, 208, 0.4);
  overflow: hidden;
  transition: all var(--transition-fast);
}

.input-wrapper:focus-within {
  border-color: var(--color-primary-light, #22c55e);
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.12);
}

.input-wrapper .ant-input {
  padding-right: 60px;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.65);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(16px);
  box-shadow: 0 4px 24px rgba(21, 128, 61, 0.06);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(187, 247, 208, 0.3);
  background: rgba(240, 253, 244, 0.4);
}

.preview-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-primary-dark, #166534);
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #ffffff;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-secondary, #059669);
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-primary, #15803d);
}

.preview-loading p {
  margin-top: 16px;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  /* 选中元素信息样式 */
  .selected-element-alert {
    margin: 0 16px;
  }

  .selected-element-info {
    line-height: 1.4;
  }

  .element-header {
    margin-bottom: 8px;
  }

  .element-details {
    margin-top: 8px;
  }

  .element-item {
    margin-bottom: 4px;
    font-size: 13px;
  }

  .element-item:last-child {
    margin-bottom: 0;
  }

  .element-tag {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    font-weight: 600;
    color: var(--color-primary, #15803d);
  }

  .element-id {
    color: var(--color-secondary, #059669);
    margin-left: 4px;
  }

  .element-class {
    color: var(--color-accent, #d97706);
    margin-left: 4px;
  }

  .element-selector-code {
    font-family: 'Monaco', 'Menlo', monospace;
    background: rgba(240, 253, 244, 0.8);
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 12px;
    color: #dc2626;
    border: 1px solid rgba(187, 247, 208, 0.4);
  }

  /* 编辑模式按钮样式 */
  .edit-mode-active {
    background-color: var(--color-primary, #15803d) !important;
    border-color: var(--color-primary, #15803d) !important;
    color: white !important;
  }

  .edit-mode-active:hover {
    background-color: var(--color-primary-light, #22c55e) !important;
    border-color: var(--color-primary-light, #22c55e) !important;
  }

  /* 互动按钮样式 */
  .interaction-buttons {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-right: 12px;
  }

  .interaction-buttons .ant-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    border-radius: 6px;
    transition: all 0.3s ease;
  }

  .interaction-buttons .ant-btn:hover {
    background-color: rgba(0, 0, 0, 0.05);
  }

  .liked {
    animation: likeAnimation 0.5s ease;
  }

  .collected {
    animation: collectAnimation 0.5s ease;
  }

  @keyframes likeAnimation {
    0% {
      transform: scale(1);
    }
    25% {
      transform: scale(1.3);
    }
    50% {
      transform: scale(0.95);
    }
    75% {
      transform: scale(1.1);
    }
    100% {
      transform: scale(1);
    }
  }

  @keyframes collectAnimation {
    0% {
      transform: scale(1) rotate(0deg);
    }
    25% {
      transform: scale(1.2) rotate(-10deg);
    }
    50% {
      transform: scale(0.95) rotate(5deg);
    }
    75% {
      transform: scale(1.1) rotate(-5deg);
    }
    100% {
      transform: scale(1) rotate(0deg);
    }
  }
}
</style>
