// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /chat-room/create */
export async function createChatRoom(
  body: API.ChatRoomCreateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseChatRoom>('/chat-room/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chat-room/dissolve */
export async function dissolveRoom(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/chat-room/dissolve', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chat-room/get */
export async function getChatRoomById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getChatRoomByIdParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseChatRoom>('/chat-room/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chat-room/invite/generate */
export async function generateInviteCode(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.generateInviteCodeParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/chat-room/invite/generate', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chat-room/invite/info */
export async function getInviteInfo(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getInviteInfoParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseInviteInfoVO>('/chat-room/invite/info', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chat-room/join */
export async function joinRoomByInviteCode(
  body: API.ChatInviteJoinRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chat-room/join', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chat-room/leave */
export async function leaveRoom(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.leaveRoomParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chat-room/leave', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chat-room/list/my-all */
export async function listMyAllRooms(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMyAllRoomsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatRoom>('/chat-room/list/my-all', {
    method: 'GET',
    params: {
      ...params,
      chatRoomQueryRequest: undefined,
      ...params['chatRoomQueryRequest'],
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chat-room/list/my-created */
export async function listMyCreatedRooms(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMyCreatedRoomsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatRoom>('/chat-room/list/my-created', {
    method: 'GET',
    params: {
      ...params,
      chatRoomQueryRequest: undefined,
      ...params['chatRoomQueryRequest'],
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chat-room/list/my-joined */
export async function listMyJoinedRooms(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMyJoinedRoomsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatRoom>('/chat-room/list/my-joined', {
    method: 'GET',
    params: {
      ...params,
      chatRoomQueryRequest: undefined,
      ...params['chatRoomQueryRequest'],
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chat-room/member/kick */
export async function kickMember(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.kickMemberParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chat-room/member/kick', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chat-room/members/list */
export async function listRoomMembers(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listRoomMembersParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListChatRoomMember>('/chat-room/members/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chat-room/message/send */
export async function sendChatMessage(
  body: API.ChatMessageSendRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong>('/chat-room/message/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /chat-room/messages */
export async function getChatMessages(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getChatMessagesParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListChatRoomMessage>('/chat-room/messages', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 管理员分页获取群聊列表 POST /chat-room/admin/list/page/vo */
export async function listChatRoomByPageForAdmin(
  body: API.ChatRoomQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatRoomVO>('/chat-room/admin/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员解散群聊 POST /chat-room/admin/dissolve */
export async function dissolveRoomByAdmin(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chat-room/admin/dissolve', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 管理员获取群聊详情 GET /chat-room/admin/get/vo */
export async function getChatRoomVOByAdmin(
  params: API.getChatRoomVOByAdminParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseChatRoomVO>('/chat-room/admin/get/vo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /chat-room/update */
export async function updateChatRoom(
  body: API.ChatRoomUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/chat-room/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
