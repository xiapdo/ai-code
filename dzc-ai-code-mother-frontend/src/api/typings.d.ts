declare namespace API {
  type AppAddRequest = {
    initPrompt?: string
  }

  type AppAdminUpdateRequest = {
    id?: number
    appName?: string
    cover?: string
    priority?: number
  }

  type AppDeployRequest = {
    appId?: number
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    priority?: number
    userId?: number
  }

  type AppUpdateRequest = {
    id?: number
    appName?: string
  }

  type AppVersionSnapshot = {
    id?: number
    appId?: number
    versionNo?: number
    snapshotData?: string
    thumbnail?: string
    changeSummary?: string
    chatHistoryId?: number
    userId?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type AppVersionSnapshotVO = {
    id?: number
    appId?: number
    versionNo?: number
    thumbnail?: string
    changeSummary?: string
    chatHistoryId?: number
    createTime?: string
    isCurrent?: boolean
  }

  type AppVO = {
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: number
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type BaseResponseAppVersionSnapshot = {
    code?: number
    data?: AppVersionSnapshot
    message?: string
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseChatRoom = {
    code?: number
    data?: ChatRoom
    message?: string
  }

  type BaseResponseInviteInfoVO = {
    code?: number
    data?: InviteInfoVO
    message?: string
  }

  type BaseResponseListChatRoomMember = {
    code?: number
    data?: ChatRoomMember[]
    message?: string
  }

  type BaseResponseListChatRoomMessage = {
    code?: number
    data?: ChatRoomMessage[]
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageAppVersionSnapshotVO = {
    code?: number
    data?: PageAppVersionSnapshotVO
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistory = {
    code?: number
    data?: PageChatHistory
    message?: string
  }

  type BaseResponsePageChatRoom = {
    code?: number
    data?: PageChatRoom
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type ChatHistory = {
    id?: number
    message?: string
    messageType?: string
    appId?: number
    userId?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ChatHistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    message?: string
    messageType?: string
    appId?: number
    userId?: number
    lastCreateTime?: string
  }

  type ChatInviteJoinRequest = {
    inviteCode?: string
  }

  type ChatMessageSendRequest = {
    roomId?: number
    messageContent?: string
    messageType?: string
    fileUrl?: string
    replyToId?: number
  }

  type ChatRoom = {
    id?: number
    roomName?: string
    roomAvatar?: string
    description?: string
    inviteCode?: string
    inviteExpiryTime?: string
    ownerUserId?: number
    memberCount?: number
    status?: number
    isDelete?: number
    createTime?: string
    updateTime?: string
  }

  type ChatRoomCreateRequest = {
    roomName?: string
    description?: string
    searchText?: string
  }

  type ChatRoomMember = {
    id?: number
    roomId?: number
    userId?: number
    memberRole?: string
    joinTime?: string
    lastReadTime?: string
    lastActiveTime?: string
    isMuted?: number
    onlineStatus?: number
    isDelete?: number
  }

  type ChatRoomMessage = {
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
    updateTime?: string
    isDelete?: number
  }

  type ChatRoomQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    roomName?: string
    ownerUserId?: number
    status?: number
  }

  type ChatRoomUpdateRequest = {
    id?: number
    roomName?: string
    roomAvatar?: string
    description?: string
  }

  type chatToGenCodeParams = {
    appId: number
    message: string
  }

  type DeleteRequest = {
    id?: number
  }

  type DiagramTask = {
    mermaidCode?: string
    description?: string
  }

  type downloadAppCodeParams = {
    appId: number
  }

  type executeWorkflowParams = {
    prompt: string
  }

  type executeWorkflowWithFluxParams = {
    prompt: string
  }

  type executeWorkflowWithSseParams = {
    prompt: string
  }

  type generateInviteCodeParams = {
    roomId: number
  }

  type getAppVOByIdByAdminParams = {
    id: number
  }

  type getAppVOByIdParams = {
    id: number
  }

  type getChatMessagesParams = {
    roomId: number
  }

  type getChatRoomByIdParams = {
    id: number
  }

  type getInviteInfoParams = {
    inviteCode: string
  }

  type getUserByIdParams = {
    id: number
  }

  type getUserVOByIdParams = {
    id: number
  }

  type getVersionDetailParams = {
    versionId: number
  }

  type IllustrationTask = {
    query?: string
  }

  type ImageCollectionPlan = {
    contentImageTasks?: ImageSearchTask[]
    illustrationTasks?: IllustrationTask[]
    diagramTasks?: DiagramTask[]
    logoTasks?: LogoTask[]
  }

  type ImageResource = {
    category?: 'CONTENT' | 'LOGO' | 'ILLUSTRATION' | 'ARCHITECTURE'
    description?: string
    url?: string
  }

  type ImageSearchTask = {
    query?: string
  }

  type InviteInfoVO = {
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

  type kickMemberParams = {
    roomId: number
    memberUserId: number
  }

  type leaveRoomParams = {
    roomId: number
  }

  type listAppChatHistoryParams = {
    appId: number
    pageSize?: number
    lastCreateTime?: string
  }

  type listMyAllRoomsParams = {
    chatRoomQueryRequest: ChatRoomQueryRequest
  }

  type listMyCreatedRoomsParams = {
    chatRoomQueryRequest: ChatRoomQueryRequest
  }

  type listMyJoinedRoomsParams = {
    chatRoomQueryRequest: ChatRoomQueryRequest
  }

  type listRoomMembersParams = {
    roomId: number
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    phone?: string
    email?: string
    createTime?: string
    updateTime?: string
  }

  type LogoTask = {
    description?: string
  }

  type PageAppVersionSnapshotVO = {
    records?: AppVersionSnapshotVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatHistory = {
    records?: ChatHistory[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatRoom = {
    records?: ChatRoom[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type QualityResult = {
    isValid?: boolean
    errors?: string[]
    suggestions?: string[]
  }

  type ServerSentEventString = true

  type serveStaticResourceParams = {
    deployKey: string
  }

  type SseEmitter = {
    timeout?: number
  }

  type User = {
    id?: number
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserEditRequest = {
    userName?: string
    userAvatar?: string
    userProfile?: string
    phone?: string
    email?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userName?: string
    userPassword?: string
    checkPassword?: string
    phone?: string
    email?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }

  type VersionQueryRequest = {
    appId?: number
    pageNum?: number
    pageSize?: number
  }

  type VersionRollbackRequest = {
    versionId?: number
    appId?: number
    deleteAfterRollback?: boolean
  }

  type WorkflowContext = {
    currentStep?: string
    originalPrompt?: string
    imageListStr?: string
    imageList?: ImageResource[]
    enhancedPrompt?: string
    generationType?: 'HTML' | 'MULTI_FILE' | 'VUE_PROJECT'
    generatedCodeDir?: string
    buildResultDir?: string
    qualityResult?: QualityResult
    errorMessage?: string
    imageCollectionPlan?: ImageCollectionPlan
    contentImages?: ImageResource[]
    illustrations?: ImageResource[]
    diagrams?: ImageResource[]
    logos?: ImageResource[]
  }
}
