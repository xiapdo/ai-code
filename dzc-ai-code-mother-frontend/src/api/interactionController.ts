import request from '@/request'

export const getInteractionStatus = (appId: number) => {
  return request<API.BaseResponse<API.InteractionStatusVO>>('/interaction/status', {
    method: 'GET',
    params: { appId },
  })
}

export const toggleLike = (appId: number) => {
  return request<API.BaseResponseBoolean>('/interaction/like', {
    method: 'POST',
    params: { appId },
  })
}

export const toggleCollect = (appId: number) => {
  return request<API.BaseResponseBoolean>('/interaction/collect', {
    method: 'POST',
    params: { appId },
  })
}

export const addComment = (data: API.CommentAddRequest) => {
  return request<API.BaseResponse<API.CommentVO>>('/interaction/comment/add', {
    method: 'POST',
    data,
  })
}

export const getCommentList = (params: API.CommentQueryRequest) => {
  return request<API.BaseResponseListCommentVO>('/interaction/comment/list', {
    method: 'GET',
    params,
  })
}

/**
 * 获取用户点赞的应用列表
 */
export const getUserLikedApps = () => {
  return request<API.BaseResponseListAppVO>('/interaction/liked', {
    method: 'GET',
  })
}

/**
 * 获取用户收藏的应用列表
 */
export const getUserCollectedApps = () => {
  return request<API.BaseResponseListAppVO>('/interaction/collected', {
    method: 'GET',
  })
}
