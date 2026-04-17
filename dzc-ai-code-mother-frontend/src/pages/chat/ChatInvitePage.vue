<template>
  <div id="chatInvitePage">
    <div class="invite-container">
      <a-card class="invite-card">
        <div class="invite-header">
          <div class="invite-icon">
            <TeamOutlined style="font-size: 48px; color: var(--color-primary, #15803d)" />
          </div>
          <h1>群聊邀请</h1>
          <p class="invite-desc">您收到了一个群聊邀请</p>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="invite-loading">
          <a-spin size="large" />
          <p>正在验证邀请码...</p>
        </div>

        <!-- 邀请无效 -->
        <div v-else-if="!inviteValid" class="invite-error">
          <CloseCircleOutlined style="font-size: 48px; color: #ff4d4f" />
          <h2>邀请码无效</h2>
          <p>该邀请码已过期或不存在</p>
          <a-button type="primary" @click="goHome">返回首页</a-button>
        </div>

        <!-- 邀请有效 -->
        <div v-else class="invite-content">
          <div class="room-info">
            <a-avatar :size="80" :src="inviteInfo?.roomAvatar || defaultAvatar" />
            <h2>{{ inviteInfo?.roomName }}</h2>
            <p class="owner-info">群主：{{ inviteInfo?.ownerName }}</p>
          </div>

          <div class="confirm-section">
            <p>确认加入该群聊吗？</p>
            <div class="action-buttons">
              <a-button size="large" @click="goHome">取消</a-button>
              <a-button type="primary" size="large" :loading="joinLoading" @click="handleJoin">
                确认加入
              </a-button>
            </div>
          </div>
        </div>

        <!-- 已加入 -->
        <div v-if="joined" class="joined-success">
          <CheckCircleOutlined style="font-size: 48px; color: #52c41a" />
          <h2>加入成功</h2>
          <p>您已成功加入群聊</p>
          <a-button type="primary" @click="enterRoom">进入群聊</a-button>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getInviteInfo, joinRoomByInviteCode } from '@/api/chatRoomController'
import defaultAvatar from '@/assets/logo.png'
import { useLoginUserStore } from '@/stores/loginUser'
import { CheckCircleOutlined, CloseCircleOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const inviteCode = route.params.code as string

// 状态
const loading = ref(true)
const inviteValid = ref(false)
const inviteInfo = ref<API.InviteInfoVO>()
const joinLoading = ref(false)
const joined = ref(false)
let roomId: number | undefined

// 检查是否登录
const checkLogin = () => {
  if (!loginUserStore.loginUser || !loginUserStore.loginUser.id) {
    // 保存邀请码，跳转登录后自动加入
    sessionStorage.setItem('pendingInviteCode', inviteCode)
    router.push(`/user/login?redirect=/chat/invite/${inviteCode}`)
    return false
  }
  return true
}

// 获取邀请信息
const fetchInviteInfo = async () => {
  if (!checkLogin()) return

  loading.value = true
  try {
    const res = await getInviteInfo({ inviteCode })
    if (res.data?.code === 0 && res.data.data?.valid) {
      inviteValid.value = true
      inviteInfo.value = res.data.data
      roomId = res.data.data.roomId
    } else {
      inviteValid.value = false
    }
  } catch (error) {
    inviteValid.value = false
  } finally {
    loading.value = false
  }
}

// 加入群聊
const handleJoin = async () => {
  if (!checkLogin()) return

  joinLoading.value = true
  try {
    const res = await joinRoomByInviteCode({ inviteCode })
    if (res.data?.code === 0) {
      joined.value = true
      message.success('加入成功')
    } else {
      message.error(res.data?.message || '加入失败')
    }
  } catch (error) {
    message.error('加入失败')
  } finally {
    joinLoading.value = false
  }
}

// 进入群聊
const enterRoom = () => {
  if (roomId) {
    router.push(`/chat/room/${roomId}`)
  } else {
    router.push('/chat/room')
  }
}

// 返回首页
const goHome = () => {
  router.push('/')
}

onMounted(() => {
  if (!inviteCode) {
    inviteValid.value = false
    loading.value = false
    return
  }
  fetchInviteInfo()
})
</script>

<style scoped>
#chatInvitePage {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.invite-container {
  width: 100%;
  max-width: 480px;
}

.invite-card {
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.invite-header {
  text-align: center;
  padding: 24px 0;
}

.invite-icon {
  margin-bottom: 16px;
}

.invite-header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.invite-desc {
  color: #666;
  margin-top: 8px;
}

.invite-loading,
.invite-error,
.joined-success {
  text-align: center;
  padding: 48px 24px;
}

.invite-content {
  padding: 24px;
}

.room-info {
  text-align: center;
  margin-bottom: 32px;
}

.room-info h2 {
  margin: 16px 0 8px;
  font-size: 20px;
}

.owner-info {
  color: #666;
}

.confirm-section {
  text-align: center;
}

.confirm-section p {
  color: #666;
  margin-bottom: 24px;
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center;
}
</style>
