import request from '@/request'

export const getNotificationList = (params?: API.NotificationQueryRequest) => {
  return request<API.BaseResponse<API.UserNotificationVO[]>>('/notification/list', {
    method: 'GET',
    params,
  })
}

export const getUnreadCount = () => {
  return request<API.BaseResponseLong>('/notification/unread/count', { method: 'GET' })
}

export const markAsRead = (notificationId: number) => {
  return request<API.BaseResponseBoolean>('/notification/read', {
    method: 'POST',
    params: { notificationId },
  })
}

export const markAllAsRead = () => {
  return request<API.BaseResponseBoolean>('/notification/read/all', { method: 'POST' })
}

export const clearAllNotifications = () => {
  return request<API.BaseResponseBoolean>('/notification/clear/all', { method: 'POST' })
}
