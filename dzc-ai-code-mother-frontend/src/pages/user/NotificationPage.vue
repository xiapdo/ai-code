<template>
  <div class="notification-page">
    <div class="notification-header">
      <h2>我的通知</h2>
      <div class="header-actions">
        <a-button v-if="notificationList.length > 0" danger @click="handleClearAll">
          清除全部
        </a-button>
        <a-button v-if="unreadCount > 0" type="primary" @click="handleMarkAllRead">
          全部已读
        </a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <div v-if="notificationList.length === 0 && !loading" class="empty-notifications">
        <a-empty description="暂无通知" />
      </div>

      <div v-else class="notification-list">
        <div
          v-for="notification in notificationList"
          :key="notification.id"
          class="notification-item"
          :class="{ 'notification-item--unread': !notification.isRead }"
          @click="handleNotificationClick(notification)"
        >
          <a-avatar :src="notification.actorUserAvatar || defaultAvatar" :size="48">
            {{ notification.actorUserName?.charAt(0) || 'U' }}
          </a-avatar>
          <div class="notification-content">
            <div class="notification-text">{{ notification.content }}</div>
            <div class="notification-meta">
              <span class="notification-type">{{ getTypeLabel(notification.type) }}</span>
              <span class="notification-time">{{ formatTime(notification.createTime) }}</span>
            </div>
          </div>
          <div v-if="!notification.isRead" class="unread-dot"></div>
        </div>

        <div v-if="hasMore" class="load-more">
          <a-button v-if="!loadingMore" type="link" @click="loadMore">加载更多</a-button>
          <a-spin v-else />
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { clearAllNotifications, getNotificationList, markAllAsRead, markAsRead } from '@/api/notificationController'
import { formatTime } from '@/utils/time'
import { message } from 'ant-design-vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const defaultAvatar = new URL('@/assets/aiAvatar.png', import.meta.url).href

const loading = ref(false)
const loadingMore = ref(false)
const notificationList = ref<API.UserNotificationVO[]>([])
const unreadCount = ref(0)
const pageNum = ref(1)
const hasMore = ref(true)
const pageSize = 20

const getTypeLabel = (type?: string) => {
  const map: Record<string, string> = {
    LIKE: '点赞',
    COLLECT: '收藏',
    COMMENT: '评论',
    REPLY: '回复',
  }
  return map[type || ''] || type
}

const loadNotifications = async (reset = false) => {
  if (reset) {
    pageNum.value = 1
    hasMore.value = true
  }

  if (!hasMore.value) return

  if (reset) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const res = await getNotificationList({
      pageNum: pageNum.value,
      pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      if (reset) {
        notificationList.value = data
      } else {
        notificationList.value.push(...data)
      }
      hasMore.value = data.length === pageSize
      pageNum.value++
      unreadCount.value = notificationList.value.filter(n => !n.isRead).length
    }
  } catch (error) {
    console.error('加载通知失败:', error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  loadNotifications(false)
}

const handleNotificationClick = async (notification: API.UserNotificationVO) => {
  if (!notification.isRead) {
    try {
      await markAsRead(notification.id as number)
      notification.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
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
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllAsRead()
    message.success('已全部标记为已读')
    notificationList.value.forEach(n => (n.isRead = true))
    unreadCount.value = 0
  } catch (error) {
    message.error('操作失败')
  }
}

const handleClearAll = async () => {
  try {
    await clearAllNotifications()
    message.success('已清除全部通知')
    notificationList.value = []
    unreadCount.value = 0
  } catch (error) {
    message.error('操作失败')
  }
}

onMounted(() => {
  loadNotifications(true)
})
</script>

<style scoped>
.notification-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.notification-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.empty-notifications {
  padding: 60px 0;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.notification-item:hover {
  background: #f5f5f5;
}

.notification-item--unread {
  background: #f0f7ff;
}

.notification-item--unread:hover {
  background: #e6f0ff;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-text {
  font-size: 15px;
  color: #333;
  line-height: 1.6;
  margin-bottom: 8px;
}

.notification-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #999;
}

.notification-type {
  color: #1677ff;
}

.unread-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #1677ff;
  position: absolute;
  top: 16px;
  right: 16px;
}

.load-more {
  text-align: center;
  padding: 16px;
}
</style>
