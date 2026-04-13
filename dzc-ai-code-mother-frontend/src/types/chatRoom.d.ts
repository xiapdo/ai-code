declare namespace API {
  // 群聊
  interface ChatRoomVO {
    id?: number
    roomName?: string
    roomAvatar?: string
    description?: string
    inviteCode?: string
    inviteExpiryTime?: string
    ownerUserId?: number
    memberCount?: number
    status?: number
    createTime?: string
    updateTime?: string
  }

  // 群聊成员
  interface ChatRoomMemberVO {
    id?: number
    roomId?: number
    userId?: number
    memberRole?: string
    joinTime?: string
    lastActiveTime?: string
    isMuted?: number
    onlineStatus?: number
    userName?: string
    userAvatar?: string
  }

  // 群聊消息
  interface ChatRoomMessageVO {
    id?: number
    roomId?: number
    userId?: number
    messageContent?: string
    messageType?: string
    fileUrl?: string
    replyToId?: number
    isRevoked?: number
    revokedTime?: string
    createTime?: string
    userName?: string
    userAvatar?: string
  }

  // 创建群聊请求
  interface ChatRoomCreateRequest {
    roomName?: string
    description?: string
  }

  // 更新群聊请求
  interface ChatRoomUpdateRequest {
    id?: number
    roomName?: string
    roomAvatar?: string
    description?: string
  }

  // 查询群聊请求
  interface ChatRoomQueryRequest {
    id?: number
    roomName?: string
    ownerUserId?: number
    status?: number
    pageNum?: number
    pageSize?: number
  }

  // 加入群聊请求
  interface ChatInviteJoinRequest {
    inviteCode?: string
  }

  // 邀请码信息
  interface InviteInfoVO {
    inviteCode?: string
    valid?: boolean
    expiryMinutes?: number
    roomId?: number
    roomName?: string
    ownerName?: string
    ownerUserId?: number
    memberCount?: number
    description?: string
  }

  // 分页结果
  interface PageChatRoomVO {
    records: ChatRoomVO[]
    total: number
    size: number
    current: number
    pages: number
  }

  // 消息分页
  interface PageChatRoomMessageVO {
    records: ChatRoomMessageVO[]
    total: number
    size: number
    current: number
    pages: number
  }

  // 删除请求
  interface DeleteRequest {
    id?: number
  }

  // WebSocket 消息
  interface ChatMessageRequest {
    content: string
    messageType: string
    replyToId?: number
  }

  // WebSocket 消息响应
  interface ChatMessageVO {
    messageId?: number
    roomId?: number
    senderId?: number
    senderName?: string
    senderAvatar?: string
    content?: string
    messageType?: string
    timestamp?: number
    isRevoked?: boolean
  }
}
