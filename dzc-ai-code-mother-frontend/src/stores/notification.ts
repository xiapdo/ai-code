import { getUnreadCount } from '@/api/notificationController'
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  let pollInterval: number | null = null

  const fetchUnreadCount = async () => {
    try {
      const res = await getUnreadCount()
      if (res.data.code === 0 && res.data.data !== undefined) {
        unreadCount.value = res.data.data as number
      }
    } catch (error) {
      console.error('获取未读通知数失败:', error)
    }
  }

  const startPolling = () => {
    if (pollInterval) return
    fetchUnreadCount()
    pollInterval = window.setInterval(() => {
      fetchUnreadCount()
    }, 30000)
  }

  const stopPolling = () => {
    if (pollInterval) {
      clearInterval(pollInterval)
      pollInterval = null
    }
  }

  return {
    unreadCount,
    fetchUnreadCount,
    startPolling,
    stopPolling,
  }
})
