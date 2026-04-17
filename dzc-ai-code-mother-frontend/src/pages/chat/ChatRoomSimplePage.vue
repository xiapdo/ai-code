<template>
  <div id="chatRoomSimplePage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <a-button type="text" @click="goBack" class="back-btn">
          <LeftOutlined />
        </a-button>
        <h1>{{ roomInfo?.roomName || '群聊' }}</h1>
      </div>
      <div class="header-right">
        <a-button type="text" @click="showInviteLink" v-if="isOwner">
          <ShareAltOutlined />
          邀请
        </a-button>
        <a-button type="text" @click="openMembersDrawer">
          <TeamOutlined />
          成员 ({{ members.length }})
        </a-button>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="main-content">
      <!-- 消息区域 -->
      <div class="messages-container" ref="messagesContainer">
        <div v-if="loadingMessages" class="loading-center">
          <a-spin size="large" />
          <p>加载消息中...</p>
        </div>

        <div v-else-if="messages.length === 0" class="empty-messages">
          <a-empty description="暂无消息，快来发送第一条消息吧！" />
        </div>

        <div v-else>
          <div
            v-for="(msg, index) in displayMessages"
            :key="msg.id || msg.aiMessageId || index"
            class="message-item"
          >
            <!-- 系统消息 -->
            <div v-if="msg.messageType === 'system'" class="system-message">
              <div class="system-content">{{ msg.messageContent }}</div>
              <div class="system-time">{{ formatTime(msg.createTime) }}</div>
            </div>

            <!-- AI对话分享卡片 -->
            <div v-else-if="msg.messageType === 'app_share'" class="share-message-wrapper">
              <SharedAppCard :message="msg" />
            </div>

            <!-- AI 消息 -->
            <div v-else-if="isAiMessageType(msg.messageType)" class="ai-message-wrapper-outer">
              <ChatRoomAiMessage
                :message="msg"
                :streaming-content="msg._streamingContent || ''"
                :is-thinking="msg.messageType === 'ai_start' && !msg._streamingContent"
              />
            </div>

            <!-- 用户消息 -->
            <div v-else class="user-message-wrapper">
              <div class="user-message" :class="{ 'my-message': msg.userId === currentUserId }">
                <div class="message-avatar" @click.stop="handleMessageAvatarClick(msg)">
                  <a-avatar :src="msg.userAvatar || defaultAvatar" />
                </div>
                <div class="message-content">
                  <div class="message-header">
                    <span class="user-name">{{
                      (msg.userName && msg.userName.trim()) || '用户'
                    }}</span>
                    <span class="message-time">{{ formatTime(msg.createTime) }}</span>
                  </div>
                  <div class="message-body">
                    <div v-if="msg.isRevoked === 1" class="revoked-text">该消息已撤回</div>
                    <div v-else>{{ msg.messageContent }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-container">
        <div class="input-wrapper">
          <!-- @AI 提示浮层 -->
          <div v-if="showAiHint" class="ai-hint-popup">
            <div
              v-for="(item, idx) in aiHintOptions"
              :key="item.value"
              class="ai-hint-item"
              :class="{ 'ai-hint-selected': aiHintIndex === idx }"
              @mousedown.prevent="selectAiHint(item.value)"
              @mouseenter="aiHintIndex = idx"
            >
              <span class="ai-hint-icon">{{ item.icon }}</span>
              <span class="ai-hint-text">{{ item.label }}</span>
              <span class="ai-hint-desc">{{ item.desc }}</span>
            </div>
            <div class="ai-hint-footer">
              <span>↑↓ 选择</span>
              <span>Enter 确认</span>
              <span>Esc 关闭</span>
            </div>
          </div>
          <a-textarea
            ref="inputRef"
            v-model:value="userInput"
            placeholder="输入消息... 输入 @ 唤醒 AI 助手"
            :rows="2"
            :maxlength="1000"
            @keydown.enter.prevent="handleInputKeyEnter"
            @keydown.down.prevent="handleInputKeyNav(1)"
            @keydown.up.prevent="handleInputKeyNav(-1)"
            @keydown.escape.prevent="showAiHint = false"
            @input="handleInputChange"
            :disabled="sendingMessage"
          />
          <div class="input-actions">
            <a-button
              type="primary"
              @click="sendMessage"
              :loading="sendingMessage"
              :disabled="!userInput.trim()"
            >
              发送
            </a-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 成员侧边栏 -->
    <a-drawer v-model:open="showMembers" title="群聊成员" placement="right" :width="300">
      <div class="members-drawer">
        <div
          class="member-item"
          v-for="member in members"
          :key="member.id"
          @click="handleMemberClick(member)"
        >
          <div class="member-avatar-wrapper">
            <a-avatar :src="member.userAvatar || defaultAvatar" />
            <span
              class="online-dot"
              :class="member.onlineStatus === 1 ? 'online' : 'offline'"
            ></span>
          </div>
          <div class="member-info">
            <div class="member-name">
              {{ (member.userName && member.userName.trim()) || '用户' }}
            </div>
            <div class="member-role">
              <span v-if="member.memberRole === 'owner'" class="owner-badge">群主</span>
              <span class="status-text" :class="member.onlineStatus === 1 ? 'online' : 'offline'">
                {{ member.onlineStatus === 1 ? '在线' : '离线' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </a-drawer>

    <!-- 成员详情弹窗 -->
    <a-modal
      v-model:open="showMemberDetail"
      :title="null"
      :footer="null"
      :width="360"
      class="member-detail-modal"
    >
      <div v-if="memberDetailLoading" class="member-detail-loading">
        <a-spin />
      </div>
      <div v-else-if="memberDetail" class="member-detail-content">
        <div class="member-detail-avatar">
          <a-avatar :src="memberDetail.userAvatar || defaultAvatar" :size="80" />
          <span
            class="online-dot large"
            :class="selectedMember?.onlineStatus === 1 ? 'online' : 'offline'"
          ></span>
        </div>
        <div class="member-detail-name">
          {{ (memberDetail.userName && memberDetail.userName.trim()) || '用户' }}
        </div>
        <div class="member-detail-status">
          <span
            class="status-badge"
            :class="selectedMember?.onlineStatus === 1 ? 'online' : 'offline'"
          >
            {{ selectedMember?.onlineStatus === 1 ? '🟢 在线' : '⚫ 离线' }}
          </span>
          <span v-if="selectedMember?.memberRole === 'owner'" class="owner-badge detail">群主</span>
        </div>
        <div v-if="memberDetail.userProfile" class="member-detail-bio">
          <div class="bio-label">简介</div>
          <div class="bio-content">{{ memberDetail.userProfile }}</div>
        </div>
      </div>
    </a-modal>

    <!-- 邀请链接弹窗 -->
    <a-modal
      v-model:open="showInviteModal"
      title="邀请链接"
      @ok="copyInviteLink"
      @cancel="showInviteModal = false"
      ok-text="复制链接"
      cancel-text="关闭"
    >
      <div class="invite-modal-content">
        <a-alert type="info" message="复制下面的链接分享给好友" show-icon />
        <div class="invite-link-container">
          <a-input :value="inviteLink" readonly class="invite-input" />
          <p class="invite-tip">链接有效期为5分钟</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {
    generateInviteCode,
    getChatMessages,
    getChatRoomById,
    listRoomMembers,
} from '@/api/chatRoomController'
import { getUserVoById } from '@/api/userController'
import { default as defaultAvatar } from '@/assets/logo.png'
import ChatRoomAiMessage from '@/components/chat/ChatRoomAiMessage.vue'
import SharedAppCard from '@/components/chat/SharedAppCard.vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { LeftOutlined, ShareAltOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const roomId = ref<string>('')
const roomInfo = ref<any>(null)
const members = ref<any[]>([])
const messages = ref<any[]>([])
const userInput = ref('')
const sendingMessage = ref(false)
const loadingMessages = ref(false)
const showMembers = ref(false)
const showInviteModal = ref(false)
const showMemberDetail = ref(false)
const memberDetailLoading = ref(false)
const memberDetail = ref<any>(null)
const selectedMember = ref<any>(null)
const showAiHint = ref(false)
const aiHintIndex = ref(0)
const aiHintConfirmed = ref(false)
const inputRef = ref<any>(null)

const aiHintOptions = [
  { value: '@AI ', label: '@AI', desc: '闲聊助手' },
  { value: '@AI-前端 ', label: '@AI-前端', desc: '前端分析助手' },
]

const aiStreamingMap = ref<Map<string, { content: string; aiMessageId: string }>>(new Map())

const messagesContainer = ref<HTMLElement>()

let stompClient: any = null

const inviteLink = ref('')

const currentUserId = computed(() => loginUserStore.loginUser?.id)

const isOwner = computed(() => {
  return roomInfo.value?.ownerUserId === currentUserId.value
})

const isAiMessageType = (type: string) => {
  return type === 'ai_start' || type === 'ai_chunk' || type === 'ai_complete'
}

const displayMessages = computed(() => {
  const result: any[] = []
  const seenAiMessageIds = new Set<string>()

  for (const msg of messages.value) {
    if (msg.messageType === 'ai_chunk') {
      continue
    }

    if (msg.messageType === 'ai_start' || msg.messageType === 'ai_complete') {
      const aiId = String(msg.aiMessageId || msg.id)
      if (seenAiMessageIds.has(aiId)) {
        if (msg.messageType === 'ai_complete') {
          const existing = result.find((m) => String(m.aiMessageId || m.id) === aiId)
          if (existing) {
            existing.messageContent = msg.messageContent
            existing.messageType = 'ai_complete'
            existing._streamingContent = msg.messageContent
          }
        }
        continue
      }
      seenAiMessageIds.add(aiId)
      const streaming = aiStreamingMap.value.get(aiId)
      const displayMsg = { ...msg }
      if (streaming) {
        displayMsg._streamingContent = streaming.content
      }
      result.push(displayMsg)
    } else {
      result.push(msg)
    }
  }

  return result
})

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

const goBack = () => {
  router.push('/')
}

const openMembersDrawer = async () => {
  showMembers.value = true
  await loadMembers()
}

const handleMemberClick = async (member: any) => {
  selectedMember.value = member
  showMemberDetail.value = true
  memberDetailLoading.value = true
  memberDetail.value = null

  try {
    const response = await getUserVoById({ id: member.userId })
    if (response.data.code === 0 && response.data.data) {
      memberDetail.value = response.data.data
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    memberDetail.value = {
      userName: member.userName,
      userAvatar: member.userAvatar,
      userProfile: member.userProfile,
    }
  } finally {
    memberDetailLoading.value = false
  }
}

const handleMessageAvatarClick = (msg: any) => {
  if (!msg.userId || msg.userId === currentUserId.value) return
  const existingMember = members.value.find((m: any) => m.userId === msg.userId)
  handleMemberClick({
    userId: msg.userId,
    userName: msg.userName,
    userAvatar: msg.userAvatar,
    memberRole: existingMember?.memberRole,
    onlineStatus: existingMember?.onlineStatus ?? 0,
  })
}

const handleInputChange = () => {
  const input = userInput.value
  if (aiHintConfirmed.value) return

  if (input === '@' || input.endsWith(' @')) {
    showAiHint.value = true
    aiHintIndex.value = 0
  } else if (!input.startsWith('@AI')) {
    showAiHint.value = false
  }
}

const handleInputKeyEnter = () => {
  if (showAiHint.value) {
    selectAiHint(aiHintOptions[aiHintIndex.value].value)
  } else {
    sendMessage()
  }
}

const handleInputKeyNav = (delta: number) => {
  if (!showAiHint.value) return
  aiHintIndex.value = (aiHintIndex.value + delta + aiHintOptions.length) % aiHintOptions.length
}

const selectAiHint = (hint: string) => {
  if (userInput.value === '@') {
    userInput.value = hint
  } else if (userInput.value.endsWith(' @')) {
    userInput.value = userInput.value.slice(0, -1) + hint
  } else {
    userInput.value = hint
  }
  showAiHint.value = false
  aiHintConfirmed.value = true

  nextTick(() => inputRef.value?.focus())
}

const handleWebSocketMessage = (messageData: any) => {
  if (isAiMessageType(messageData.messageType)) {
    handleAiMessage(messageData)
    return
  }

  const exists = messages.value.some((m) => m.id === messageData.id)
  if (!exists) {
    messages.value.push(messageData)
    scrollToBottom()
  }
}

function handleAiMessage(messageData: any) {
  const aiId = String(messageData.aiMessageId || '')

  if (messageData.messageType === 'ai_chunk') {
    const existing = aiStreamingMap.value.get(aiId)
    if (existing) {
      existing.content += messageData.messageContent
    } else {
      aiStreamingMap.value.set(aiId, {
        content: messageData.messageContent,
        aiMessageId: aiId,
      })
    }

    let startMsg = messages.value.find((m) => String(m.aiMessageId) === aiId)
    if (!startMsg) {
      startMsg = {
        id: null,
        roomId: messageData.roomId,
        userId: null,
        userName: 'AI 助手',
        userAvatar: null,
        messageContent: '',
        messageType: 'ai_start',
        fileUrl: null,
        replyToId: null,
        isRevoked: 0,
        revokedTime: null,
        createTime: messageData.createTime,
        aiMessageId: messageData.aiMessageId,
        _streamingContent: '',
      }
      messages.value.push(startMsg)
    }

    const streaming = aiStreamingMap.value.get(aiId)!
    startMsg._streamingContent = streaming.content
    scrollToBottom()
    return
  }

  if (messageData.messageType === 'ai_start') {
    aiStreamingMap.value.delete(aiId)

    const existingStartMsg = messages.value.find((m) => String(m.aiMessageId) === aiId)
    if (existingStartMsg) {
      Object.assign(existingStartMsg, messageData)
      existingStartMsg._streamingContent = ''
    } else {
      messages.value.push({ ...messageData, _streamingContent: '' })
    }
    scrollToBottom()
    return
  }

  if (messageData.messageType === 'ai_complete') {
    aiStreamingMap.value.delete(aiId)

    const existingStartMsg = messages.value.find((m) => String(m.aiMessageId) === aiId)
    if (existingStartMsg) {
      existingStartMsg.messageContent = messageData.messageContent || ''
      existingStartMsg.messageType = 'ai_complete'
      existingStartMsg._streamingContent = messageData.messageContent || ''
      if (messageData.id) {
        existingStartMsg.id = messageData.id
      }
      scrollToBottom()
      return
    }

    messages.value.push({ ...messageData, _streamingContent: messageData.messageContent || '' })
    scrollToBottom()
    return
  }
}

const connectWebSocket = () => {
  console.log('[WebSocket] 开始连接...', 'roomId:', roomId.value)

  const socket = new SockJS('/api/ws')
  stompClient = Stomp.over(socket)

  stompClient.debug = (str: string) => {
    console.log('[STOMP]', str)
  }

  stompClient.connect(
    {},
    (frame: any) => {
      console.log('[WebSocket] ✅ 连接成功')

      const subscribePath = `/topic/chat/${roomId.value}`
      console.log('[WebSocket] 订阅路径:', subscribePath)

      stompClient.subscribe(subscribePath, (msg: any) => {
        console.log('[WebSocket] 📩 收到消息:', msg.body)
        try {
          const messageData = JSON.parse(msg.body)
          console.log('[WebSocket] 解析后:', messageData)
          handleWebSocketMessage(messageData)
        } catch (e) {
          console.error('[WebSocket] 解析失败:', e)
        }
      })
    },
    (error: any) => {
      console.error('[WebSocket] ❌ 连接失败:', error)
    },
  )
}

const sendMessage = async () => {
  if (!userInput.value.trim() || sendingMessage.value) return

  const content = userInput.value.trim()
  sendingMessage.value = true
  showAiHint.value = false

  try {
    if (stompClient && stompClient.connected) {
      const messageData = {
        content: content,
        messageType: 'text',
      }
      stompClient.send(`/app/chat/${roomId.value}/send`, {}, JSON.stringify(messageData))
      userInput.value = ''
      aiHintConfirmed.value = false
      console.log('[WebSocket] 消息已发送')
    } else {
      message.error('WebSocket 未连接')
    }
  } catch (error) {
    console.error('发送失败:', error)
    message.error('发送失败')
  } finally {
    sendingMessage.value = false
  }
}

const loadRoomInfo = async () => {
  if (!roomId.value || roomId.value === '0') {
    message.error('群聊ID无效')
    router.back()
    return
  }

  try {
    const response = await getChatRoomById({ id: roomId.value as any })
    if (response.data.code === 0 && response.data.data) {
      roomInfo.value = response.data.data
    } else {
      message.error(response.data.message || '获取群聊信息失败')
      router.back()
    }
  } catch (error: any) {
    console.error('获取群聊信息失败:', error)
    message.error(error?.response?.data?.message || error?.message || '获取群聊信息失败')
    router.back()
  }
}

const loadMembers = async () => {
  if (!roomId.value || roomId.value === '0') {
    console.error('群聊ID无效,无法加载成员列表')
    return
  }

  try {
    const response = await listRoomMembers({ roomId: roomId.value as any })
    if (response.data.code === 0 && response.data.data) {
      members.value = response.data.data
    }
  } catch (error) {
    console.error('获取成员列表失败:', error)
  }
}

const loadMessages = async () => {
  if (!roomId.value || roomId.value === '0') {
    console.error('群聊ID无效,无法加载消息历史')
    loadingMessages.value = false
    return
  }

  loadingMessages.value = true
  try {
    const response = await getChatMessages({ roomId: roomId.value as any })
    if (response.data.code === 0 && response.data.data) {
      messages.value = response.data.data
      scrollToBottom()
    }
  } catch (error) {
    console.error('获取消息历史失败:', error)
    message.error('获取消息历史失败')
  } finally {
    loadingMessages.value = false
  }
}

const showInviteLink = async () => {
  if (!roomId.value || roomId.value === '0') {
    message.error('群聊ID无效')
    return
  }

  try {
    const response = await generateInviteCode({ roomId: roomId.value as any })
    if (response.data.code === 0) {
      inviteLink.value = `${window.location.origin}/chat/invite/${response.data.data}`
      showInviteModal.value = true
    } else {
      message.error('生成邀请链接失败')
    }
  } catch (error) {
    console.error('生成邀请链接失败:', error)
    message.error('生成邀请链接失败')
  }
}

const copyInviteLink = async () => {
  try {
    await navigator.clipboard.writeText(inviteLink.value)
    message.success('邀请链接已复制到剪贴板')
    showInviteModal.value = false
  } catch (error) {
    message.error('复制失败')
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

onMounted(async () => {
  roomId.value = route.params.id as string
  console.log('[页面] roomId:', roomId.value)

  if (!roomId.value || roomId.value === '0') {
    message.error('群聊ID无效')
    router.back()
    return
  }

  await Promise.all([loadRoomInfo(), loadMembers(), loadMessages()])

  setTimeout(() => {
    connectWebSocket()
  }, 500)
})

onUnmounted(() => {
  if (stompClient) {
    stompClient.disconnect()
  }
})
</script>

<style scoped>
#chatRoomSimplePage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(ellipse at 10% 90%, rgba(34, 197, 94, 0.08) 0, transparent 50%),
    radial-gradient(ellipse at 90% 10%, rgba(5, 150, 105, 0.06) 0, transparent 50%), #f0fdf4;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.72);
  border-bottom: 1px solid rgba(187, 247, 208, 0.3);
  box-shadow: 0 4px 20px rgba(21, 128, 61, 0.08);
  backdrop-filter: blur(16px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-primary-dark, #166534);
  font-family: 'Lora', serif;
}

.back-btn {
  color: var(--color-primary, #15803d) !important;
  transition: all var(--transition-fast);
}

.back-btn:hover {
  color: var(--color-primary-light, #22c55e) !important;
  transform: translateX(-2px);
}

.header-right {
  display: flex;
  gap: 8px;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
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

.loading-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-primary, #15803d);
}

.empty-messages {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.message-item {
  margin-bottom: 16px;
}

.system-message {
  text-align: center;
  margin: 16px 0;
}

.system-content {
  display: inline-block;
  padding: 4px 14px;
  background: rgba(34, 197, 94, 0.1);
  border-radius: 16px;
  color: var(--color-secondary, #059669);
  font-size: 12px;
  border: 1px solid rgba(187, 247, 208, 0.3);
}

.system-time {
  font-size: 10px;
  color: #9ca3af;
  margin-top: 4px;
}

.user-message-wrapper {
  display: flex;
  margin-bottom: 12px;
}

.ai-message-wrapper-outer {
  display: flex;
  margin-bottom: 12px;
}

.share-message-wrapper {
  display: flex;
  justify-content: center;
  margin: 16px 0;
  padding: 0 20px;
}

.user-message {
  display: flex;
  align-items: flex-start;
  max-width: 70%;
}

.user-message.my-message {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-avatar {
  margin: 0 8px;
  cursor: pointer;
}

.message-content {
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.9), rgba(220, 252, 231, 0.85));
  border-radius: 16px;
  padding: 10px 16px;
  box-shadow: 0 2px 8px rgba(21, 128, 61, 0.08);
  border: 1px solid rgba(187, 247, 208, 0.5);
  color: var(--color-primary-dark, #166534);
}

.my-message .message-content {
  background: linear-gradient(
    135deg,
    var(--color-primary, #15803d),
    var(--color-primary-dark, #166534)
  );
  color: white;
  border-bottom-right-radius: 6px;
  box-shadow: 0 4px 12px rgba(21, 128, 61, 0.25);
  border: none;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
}

.user-name {
  font-weight: 600;
  color: var(--color-primary-dark, #166534);
}

.my-message .user-name {
  color: rgba(255, 255, 255, 0.9);
}

.message-time {
  opacity: 0.7;
  margin-left: 8px;
  font-size: 11px;
}

.message-body {
  word-wrap: break-word;
  line-height: 1.5;
  font-size: 14px;
}

.revoked-text {
  font-style: italic;
  opacity: 0.7;
}

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
  overflow: visible;
  transition: all var(--transition-fast);
}

.input-wrapper:focus-within {
  border-color: var(--color-primary-light, #22c55e);
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.12);
}

.input-wrapper :deep(.ant-input) {
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

.ai-hint-popup {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(187, 247, 208, 0.5);
  margin-bottom: 4px;
  overflow: hidden;
  z-index: 10;
}

.ai-hint-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition:
    background 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-left-color 0.2s ease;
  border-left: 3px solid transparent;
}

.ai-hint-item:hover {
  background: rgba(34, 197, 94, 0.08);
  transform: translateX(3px) scale(1.02);
  border-left-color: #86efac;
}

.ai-hint-item:hover .ai-hint-icon {
  transform: scale(1.25);
}

.ai-hint-selected {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.12), rgba(167, 243, 208, 0.1));
  border-left-color: #22c55e;
  box-shadow: inset 0 0 0 1px rgba(34, 197, 94, 0.08);
}

.ai-hint-icon {
  font-size: 16px;
  transition: transform 0.2s ease;
  display: inline-block;
}

.ai-hint-text {
  font-weight: 600;
  color: #15803d;
  font-size: 14px;
}

.ai-hint-desc {
  font-size: 11px;
  color: #9ca3af;
  margin-left: auto;
}

.ai-hint-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 6px 16px 8px;
  font-size: 11px;
  color: #9ca3af;
  border-top: 1px solid rgba(187, 247, 208, 0.3);
}

.members-drawer .member-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid rgba(187, 247, 208, 0.2);
  cursor: pointer;
  transition: background 0.2s;
  border-radius: 8px;
}

.members-drawer .member-item:hover {
  background: rgba(34, 197, 94, 0.06);
}

.member-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.online-dot {
  position: absolute;
  bottom: 1px;
  right: 1px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #fff;
  z-index: 1;
}

.online-dot.online {
  background: #22c55e;
  box-shadow: 0 0 4px rgba(34, 197, 94, 0.5);
}

.online-dot.offline {
  background: #9ca3af;
}

.online-dot.large {
  width: 14px;
  height: 14px;
  border-width: 3px;
  bottom: 2px;
  right: 2px;
}

.member-info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
}

.member-name {
  font-weight: 500;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-role {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.status-text.online {
  color: #22c55e;
}

.status-text.offline {
  color: #9ca3af;
}

.owner-badge {
  display: inline-block;
  background: rgba(217, 119, 6, 0.12);
  color: var(--color-accent, #d97706);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 500;
}

.owner-badge.detail {
  font-size: 12px;
  padding: 2px 10px;
}

.member-detail-loading {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.member-detail-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
}

.member-detail-avatar {
  position: relative;
  margin-bottom: 16px;
}

.member-detail-name {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.member-detail-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.status-badge.online {
  color: #22c55e;
  font-size: 14px;
}

.status-badge.offline {
  color: #9ca3af;
  font-size: 14px;
}

.member-detail-bio {
  width: 100%;
  padding: 12px 16px;
  background: rgba(240, 253, 244, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(187, 247, 208, 0.4);
}

.bio-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.bio-content {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  word-break: break-word;
}

.invite-modal-content {
  padding: 10px 0;
}

.invite-link-container {
  margin-top: 20px;
}

.invite-input {
  margin-bottom: 8px;
}

.invite-tip {
  font-size: 12px;
  color: var(--color-secondary, #059669);
  text-align: center;
}
</style>
