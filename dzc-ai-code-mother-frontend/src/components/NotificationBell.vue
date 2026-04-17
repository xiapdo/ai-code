<template>
  <div class="notification-bell">
    <a-badge :count="unreadCount" :overflow-count="99">
      <div class="bell-icon-wrapper" @click="toggleDropdown">
        <BellOutlined />
      </div>
    </a-badge>

    <a-dropdown v-model:open="dropdownVisible" :trigger="['click']" placement="bottomRight">
      <div></div>
      <template #overlay>
        <div class="notification-dropdown">
          <div class="notification-header">
            <span class="notification-title">通知</span>
            <a-button v-if="unreadCount > 0" type="link" size="small" @click="handleMarkAllRead">
              全部已读
            </a-button>
          </div>

          <a-spin :spinning="loading">
            <div v-if="notificationList.length === 0 && !loading" class="notification-empty">
              <a-empty description="暂无通知" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
            </div>

            <div v-else class="notification-list">
              <div
                v-for="notification in notificationList"
                :key="notification.id"
                class="notification-item"
                :class="{ 'notification-item--unread': !notification.isRead }"
                @click="handleNotificationClick(notification)"
              >
                <a-avatar :src="notification.actorUserAvatar || defaultAvatar" :size="36">
                  {{ notification.actorUserName?.charAt(0) || 'U' }}
                </a-avatar>
                <div class="notification-content">
                  <div class="notification-text">{{ notification.content }}</div>
                  <div class="notification-time">{{ formatTime(notification.createTime) }}</div>
                </div>
                <div v-if="!notification.isRead" class="unread-dot"></div>
              </div>
            </div>
          </a-spin>

          <div v-if="notificationList.length > 0" class="notification-footer">
            <a-button type="link" size="small" @click="goToNotificationPage">
              查看全部通知
            </a-button>
          </div>
        </div>
      </template>
    </a-dropdown>
  </div>
</template>

<script setup lang="ts">
import { getNotificationList, markAllAsRead, markAsRead } from '@/api/notificationController'
import { useNotificationStore } from '@/stores/notification'
import { formatTime } from '@/utils/time'
import { BellOutlined } from '@ant-design/icons-vue'
import { Empty, message } from 'ant-design-vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const notificationStore = useNotificationStore()

const dropdownVisible = ref(false)
const loading = ref(false)
const notificationList = ref<API.UserNotificationVO[]>([])

const stompClient: any = null

const unreadCount = computed(() => notificationStore.unreadCount)
const defaultAvatar = new URL('@/assets/aiAvatar.png', import.meta.url).href

const loadNotifications = async () => {
  loading.value = true
  try {
    const res = await getNotificationList({ pageNum: 1, pageSize: 10 })
    if (res.data.code === 0 && res.data.data) {
      notificationList.value = res.data.data
      notificationStore.fetchUnreadCount()
    }
  } catch (error) {
    console.error('加载通知列表失败:', error)
  } finally {
    loading.value = false
  }
}

const toggleDropdown = async () => {
  if (!dropdownVisible.value) {
    await loadNotifications()
  }
  dropdownVisible.value = !dropdownVisible.value
}

const handleNotificationClick = async (notification: API.UserNotificationVO) => {
  if (!notification.isRead) {
    try {
      await markAsRead(notification.id as number)
      notification.isRead = true
      notificationStore.fetchUnreadCount()
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }

  if (notification.appId) {
    if (notification.type === 'COMMENT' || notification.type === 'REPLY') {
      const highlightCommentId = (notification as any).commentId
      if (highlightCommentId) {
        router.push(`/app/chat/${notification.appId}?openCommentDrawer=true&highlightCommentId=${highlightCommentId}&showInteraction=true`)
      } else {
        router.push(`/app/chat/${notification.appId}?openCommentDrawer=true&showInteraction=true`)
      }
    } else {
      router.push(`/app/chat/${notification.appId}?showInteraction=true`)
    }
    dropdownVisible.value = false
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    message.success('已全部标记为已读')
    notificationList.value.forEach(n => (n.isRead = true))
    notificationStore.fetchUnreadCount()
  } catch (error) {
    message.error('操作失败')
  }
}

const goToNotificationPage = () => {
  router.push('/notification')
  dropdownVisible.value = false
}

const initWebSocket = () => {
}

onMounted(() => {
  notificationStore.startPolling()
})

onUnmounted(() => {
  notificationStore.stopPolling()
})
</script>

<style scoped>
.notification-bell {
  position: relative;
  display: inline-block;
}

.bell-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-primary, #15803d);
  font-size: 16px;
  line-height: 1;
}

.bell-icon-wrapper:hover {
  background: transparent !important;
}

.notification-dropdown {
  width: 360px;
  max-height: 480px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.notification-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.notification-title {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.notification-empty {
  padding: 40px 0;
}

.notification-list {
  max-height: 360px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #f5f5f5;
}

.notification-item--unread {
  background-color: #f0f7ff;
}

.notification-item--unread:hover {
  background-color: #e6f0ff;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notification-time {
  font-size: 12px;
  color: #999;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #1677ff;
  flex-shrink: 0;
  margin-top: 6px;
}

.notification-footer {
  text-align: center;
  padding: 8px;
  border-top: 1px solid #f0f0f0;
}
</style>
