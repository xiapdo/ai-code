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
        <a-button type="text" @click="showMembers = !showMembers">
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
          <div v-for="(msg, index) in messages" :key="msg.id || index" class="message-item">
            <!-- 系统消息 -->
            <div v-if="msg.messageType === 'system'" class="system-message">
              <div class="system-content">{{ msg.messageContent }}</div>
              <div class="system-time">{{ formatTime(msg.createTime) }}</div>
            </div>

            <!-- 用户消息 -->
            <div v-else class="user-message-wrapper">
              <div class="user-message" :class="{ 'my-message': msg.userId === currentUserId }">
                <div class="message-avatar">
                  <a-avatar :src="msg.userAvatar || defaultAvatar" />
                </div>
                <div class="message-content">
                  <div class="message-header">
                    <span class="user-name">{{ msg.userName || '用户' }}</span>
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
          <a-textarea
            v-model:value="userInput"
            placeholder="输入消息..."
            :rows="2"
            :maxlength="1000"
            @keydown.enter.prevent="sendMessage"
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
        <div class="member-item" v-for="member in members" :key="member.id">
          <a-avatar :src="member.userAvatar || defaultAvatar" />
          <div class="member-info">
            <div class="member-name">{{ member.userName }}</div>
            <div class="member-role">
              <span v-if="member.memberRole === 'owner'" class="owner-badge">群主</span>
            </div>
          </div>
        </div>
      </div>
    </a-drawer>

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
import { default as defaultAvatar } from '@/assets/aiAvatar.png'
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

// 状态
const roomId = ref<string>('')
const roomInfo = ref<any>(null)
const members = ref<any[]>([])
const messages = ref<any[]>([])
const userInput = ref('')
const sendingMessage = ref(false)
const loadingMessages = ref(false)
const showMembers = ref(false)
const showInviteModal = ref(false)

// Refs
const messagesContainer = ref<HTMLElement>()

// WebSocket
let stompClient: any = null

// 邀请链接
const inviteLink = ref('')

// 当前用户ID
const currentUserId = computed(() => loginUserStore.loginUser?.id)

// 是否为群主
const isOwner = computed(() => {
  return roomInfo.value?.ownerUserId === currentUserId.value
})

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 连接 WebSocket
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

          // 检查是否已存在
          const exists = messages.value.some((m) => m.id === messageData.id)
          if (!exists) {
            messages.value.push(messageData)
            console.log('[WebSocket] 消息已添加，总数:', messages.value.length)
            scrollToBottom()
          }
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

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || sendingMessage.value) return

  const content = userInput.value.trim()
  sendingMessage.value = true

  try {
    // 通过 WebSocket 发送
    if (stompClient && stompClient.connected) {
      const messageData = {
        content: content,
        messageType: 'text',
      }
      stompClient.send(`/app/chat/${roomId.value}/send`, {}, JSON.stringify(messageData))
      userInput.value = ''
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

// 加载群聊信息
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

// 加载成员列表
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

// 加载消息历史
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

// 显示邀请链接
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

// 复制邀请链接
const copyInviteLink = async () => {
  try {
    await navigator.clipboard.writeText(inviteLink.value)
    message.success('邀请链接已复制到剪贴板')
    showInviteModal.value = false
  } catch (error) {
    message.error('复制失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 初始化
onMounted(async () => {
  roomId.value = route.params.id as string
  console.log('[页面] roomId:', roomId.value)

  if (!roomId.value || roomId.value === '0') {
    message.error('群聊ID无效')
    router.back()
    return
  }

  await Promise.all([loadRoomInfo(), loadMembers(), loadMessages()])

  // 连接 WebSocket
  setTimeout(() => {
    connectWebSocket()
  }, 500)
})

// 清理
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
  background: #f5f5f5;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
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
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 主要内容 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 消息容器 */
.messages-container {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #fafafa;
}

.loading-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.empty-messages {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

/* 消息项 */
.message-item {
  margin-bottom: 16px;
}

/* 系统消息 */
.system-message {
  text-align: center;
  margin: 16px 0;
}

.system-content {
  display: inline-block;
  padding: 4px 12px;
  background: #f0f0f0;
  border-radius: 16px;
  color: #666;
  font-size: 12px;
}

.system-time {
  font-size: 10px;
  color: #999;
  margin-top: 4px;
}

/* 用户消息 */
.user-message-wrapper {
  display: flex;
  margin-bottom: 12px;
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
}

.message-content {
  background: white;
  border-radius: 12px;
  padding: 10px 14px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.my-message .message-content {
  background: #1890ff;
  color: white;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
}

.user-name {
  font-weight: 500;
}

.message-time {
  opacity: 0.8;
  margin-left: 8px;
}

.message-body {
  word-wrap: break-word;
  line-height: 1.5;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  background: white;
  border-top: 1px solid #e8e8e8;
}

.input-wrapper {
  position: relative;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* 成员侧边栏 */
.members-drawer .member-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.member-info {
  flex: 1;
  margin-left: 12px;
}

.member-name {
  font-weight: 500;
}

.member-role {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.owner-badge {
  display: inline-block;
  background: #f0f5ff;
  color: #1890ff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
}

/* 邀请链接弹窗 */
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
  color: #666;
  text-align: center;
}
</style>
