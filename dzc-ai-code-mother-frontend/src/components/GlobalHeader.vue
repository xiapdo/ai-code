<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <div class="logo" aria-label="灵码 logo" role="img"></div>
            <h1 class="site-title">灵码💕秒哒</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col>
        <div class="user-login-status">
          <!-- 群聊按钮 - 点击弹出菜单 -->
          <div class="chat-popover-wrapper" title="群聊" v-if="loginUserStore.loginUser.id">
            <a-button class="chat-btn" @click="toggleChatMenu">
              <TeamOutlined />
            </a-button>
            <transition name="fade">
              <div v-if="chatMenuVisible" class="chat-popover-dropdown">
                <div class="chat-popover-content">
                  <!-- 创建/加入群聊 -->
                  <div class="popover-section-title">群聊操作</div>
                  <div class="popover-menu-item" @click="handleMenuItemClick('create')">
                    <PlusOutlined />
                    <span>创建群聊</span>
                  </div>
                  <div class="popover-menu-item" @click="handleMenuItemClick('join')">
                    <LinkOutlined />
                    <span>加入群聊</span>
                  </div>
                  <a-divider style="margin: 8px 0" />
                  <!-- 我的群聊 -->
                  <div class="popover-menu-item" @click="handleMenuItemClick('my-rooms')">
                    <FolderOutlined />
                    <span>我的群聊</span>
                  </div>
                  <!-- 已加入群聊 -->
                  <div class="popover-menu-item" @click="handleMenuItemClick('joined-rooms')">
                    <TeamOutlined />
                    <span>已加入群聊</span>
                  </div>
                </div>
              </div>
            </transition>
          </div>
          <!-- 通知铃铛 -->
          <div class="notify-btn" title="通知" v-if="loginUserStore.loginUser.id">
            <NotificationBell />
          </div>
          <!-- 用户登录状态 -->
          <a-dropdown v-if="loginUserStore.loginUser.id" overlayClassName="user-dropdown-menu">
            <a-space>
              <a-avatar :src="loginUserStore.loginUser.userAvatar || defaultAvatar" />
              {{ loginUserStore.loginUser.userName ?? '十二' }}
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="goToProfile">
                  <UserOutlined />
                  <span style="margin-left: 10px">个人中心</span>
                </a-menu-item>
                <a-menu-item class="logout-menu-item" @click="doLogout">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>

  <!-- 创建群聊弹窗 -->
  <a-modal
    v-model:open="createModalVisible"
    title="创建群聊"
    @ok="handleCreate"
    :confirm-loading="creating"
    :width="480"
    :destroyOnClose="false"
    :maskClosable="true"
  >
    <a-form ref="createFormRef" :model="createForm" :rules="createRules" layout="vertical">
      <a-form-item label="群聊名称" name="roomName">
        <a-input v-model:value="createForm.roomName" placeholder="请输入群聊名称" size="large" />
      </a-form-item>
      <a-form-item label="群聊描述（可选）" name="description">
        <a-textarea
          v-model:value="createForm.description"
          placeholder="请输入群聊描述"
          :rows="3"
          show-count
          :maxlength="200"
        />
      </a-form-item>
    </a-form>
    <div v-if="createdRoom" class="created-room-info">
      <a-alert type="success" show-icon message="群聊创建成功！" />
      <div class="invite-link-section">
        <p>邀请链接（5分钟内有效）：</p>
        <a-input-group compact>
          <a-input
            :value="getInviteLink(createdRoom.inviteCode)"
            readonly
            style="width: calc(100% - 100px)"
          />
          <a-button type="primary" @click="copyInviteLink">
            <template #icon><CopyOutlined /></template>
            复制
          </a-button>
        </a-input-group>
      </div>
    </div>
  </a-modal>

  <!-- 加入群聊弹窗 -->
  <a-modal
    v-model:open="joinModalVisible"
    title="加入群聊"
    @ok="handleJoin"
    :confirm-loading="joining"
    :width="480"
    :destroyOnClose="false"
    :maskClosable="true"
  >
    <a-form ref="joinFormRef" :model="joinForm" :rules="joinRules" layout="vertical">
      <a-form-item label="邀请链接" name="inviteCode">
        <a-input
          v-model:value="joinForm.inviteCode"
          placeholder="请输入邀请链接或邀请码"
          size="large"
          @input="extractInviteCode"
        >
          <template #prefix><LinkOutlined /></template>
        </a-input>
      </a-form-item>
    </a-form>
    <div v-if="inviteInfo" class="invite-info">
      <a-alert type="info" show-icon :message="`您将被邀请加入：${inviteInfo.roomName}`" />
      <div class="invite-detail">
        <p><strong>创建者：</strong>{{ inviteInfo.creatorName }}</p>
        <p><strong>当前人数：</strong>{{ inviteInfo.memberCount }}人</p>
        <p><strong>描述：</strong>{{ inviteInfo.description || '暂无描述' }}</p>
      </div>
    </div>
  </a-modal>

  <!-- 我的群聊 弹窗 -->
  <a-modal
    v-model:open="myRoomsModalVisible"
    title="我的群聊"
    :footer="null"
    :width="520"
    :destroyOnClose="false"
    :maskClosable="true"
  >
    <a-spin :spinning="myCreatedLoading">
      <div v-if="myCreatedList.length === 0 && !myCreatedLoading" class="empty-tip">
        <a-empty description="暂无创建的群聊" />
      </div>
      <div v-else class="modal-room-list">
        <div
          v-for="room in myCreatedList"
          :key="room.id"
          class="modal-room-item"
          @click="openMyRoomDetail(room)"
        >
          <a-avatar
            :src="room.roomAvatar"
            style="background-color: var(--color-primary, #15803d); flex-shrink: 0"
          >
            {{ room.roomName?.charAt(0) }}
          </a-avatar>
          <div class="modal-room-info">
            <div class="modal-room-name">{{ room.roomName }}</div>
            <div class="modal-room-desc">
              <span>{{ room.description || '暂无描述' }} · {{ room.memberCount }}人</span>
              <span class="room-time">{{ formatTime(room.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-wrapper" v-if="myCreatedTotal > 5">
        <a-pagination
          v-model:current="myCreatedPageNum"
          :total="myCreatedTotal"
          :page-size="5"
          size="small"
          @change="onMyCreatedPageChange"
        />
      </div>
    </a-spin>
  </a-modal>

  <!-- 我的群聊 - 详情弹窗 -->
  <a-modal
    v-model:open="myRoomDetailVisible"
    :title="selectedMyRoom?.roomName"
    :footer="null"
    :width="480"
  >
    <div v-if="selectedMyRoom" class="room-detail">
      <div class="room-detail-info">
        <a-avatar
          :src="selectedMyRoom.roomAvatar"
          :size="48"
          style="background-color: var(--color-primary, #15803d)"
        >
          {{ selectedMyRoom.roomName?.charAt(0) }}
        </a-avatar>
        <div class="room-detail-text">
          <h3>{{ selectedMyRoom.roomName }}</h3>
          <p>{{ selectedMyRoom.description || '暂无描述' }}</p>
          <p>成员：{{ selectedMyRoom.memberCount }}人</p>
        </div>
      </div>
      <div class="room-detail-actions">
        <a-button type="primary" @click="enterRoom(selectedMyRoom)">进入群聊</a-button>
        <a-button @click="handleHeaderInvite(selectedMyRoom)">邀请</a-button>
        <a-button @click="openEditModal">修改</a-button>
        <a-button danger @click="handleDissolve(selectedMyRoom)">解散</a-button>
      </div>
    </div>
  </a-modal>

  <!-- 已加入群聊 弹窗 -->
  <a-modal
    v-model:open="joinedRoomsModalVisible"
    title="已加入群聊"
    :footer="null"
    :width="520"
    :destroyOnClose="false"
    :maskClosable="true"
  >
    <a-spin :spinning="joinedLoading">
      <div v-if="joinedList.length === 0 && !joinedLoading" class="empty-tip">
        <a-empty description="暂无已加入的群聊" />
      </div>
      <div v-else class="modal-room-list">
        <div
          v-for="room in joinedList"
          :key="room.id"
          class="modal-room-item"
          @click="openJoinedRoomDetail(room)"
        >
          <a-avatar :src="room.roomAvatar" style="background-color: #52c41a; flex-shrink: 0">
            {{ room.roomName?.charAt(0) }}
          </a-avatar>
          <div class="modal-room-info">
            <div class="modal-room-name">{{ room.roomName }}</div>
            <div class="modal-room-desc">
              <span>{{ room.description || '暂无描述' }} · {{ room.memberCount }}人</span>
              <span class="room-time">{{ formatTime(room.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-wrapper" v-if="joinedTotal > 5">
        <a-pagination
          v-model:current="joinedPageNum"
          :total="joinedTotal"
          :page-size="5"
          size="small"
          @change="onJoinedPageChange"
        />
      </div>
    </a-spin>
  </a-modal>

  <!-- 已加入群聊 - 详情弹窗 -->
  <a-modal
    v-model:open="joinedRoomDetailVisible"
    :title="selectedJoinedRoom?.roomName"
    :footer="null"
    :width="480"
  >
    <div v-if="selectedJoinedRoom" class="room-detail">
      <div class="room-detail-info">
        <a-avatar :src="selectedJoinedRoom.roomAvatar" :size="48" style="background-color: #52c41a">
          {{ selectedJoinedRoom.roomName?.charAt(0) }}
        </a-avatar>
        <div class="room-detail-text">
          <h3>{{ selectedJoinedRoom.roomName }}</h3>
          <p>{{ selectedJoinedRoom.description || '暂无描述' }}</p>
          <p>成员：{{ selectedJoinedRoom.memberCount }}人</p>
        </div>
      </div>
      <div class="room-detail-actions">
        <a-button type="primary" @click="enterRoom(selectedJoinedRoom)">进入群聊</a-button>
        <a-button danger @click="handleLeaveRoom(selectedJoinedRoom)">退出群聊</a-button>
      </div>
    </div>
  </a-modal>

  <!-- 修改群聊弹窗 -->
  <a-modal
    v-model:open="editModalVisible"
    title="修改群聊信息"
    @ok="handleUpdateRoom"
    :confirm-loading="updating"
  >
    <a-form layout="vertical">
      <a-form-item label="群聊名称">
        <a-input v-model:value="editForm.roomName" placeholder="请输入群聊名称" />
      </a-form-item>
      <a-form-item label="群聊描述">
        <a-textarea v-model:value="editForm.description" placeholder="请输入群聊描述" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 邀请链接弹窗 -->
  <a-modal
    v-model:open="inviteModalVisible"
    title="邀请成员"
    @cancel="closeInviteModal"
    :footer="null"
  >
    <div class="invite-content">
      <a-alert type="info" message="复制下面的链接分享给好友" show-icon />
      <div class="invite-link" style="margin-top: 12px">
        <a-input-group compact>
          <a-input :value="inviteLink" readonly style="width: calc(100% - 100px)" />
          <a-button type="primary" @click="copyHeaderInviteLink">复制</a-button>
        </a-input-group>
        <p class="link-tip">链接有效期为5分钟</p>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import {
  createChatRoom,
  dissolveRoom,
  generateInviteCode as generateInviteCodeApi,
  getInviteInfo,
  joinRoomByInviteCode,
  leaveRoom,
  listMyCreatedRooms,
  listMyJoinedRooms,
  updateChatRoom,
} from '@/api/chatRoomController'
import { userLogout } from '@/api/userController.ts'
import defaultAvatar from '@/assets/aiAvatar.png'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import {
  CopyOutlined,
  FolderOutlined,
  HomeOutlined,
  LinkOutlined,
  LogoutOutlined,
  PlusOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { Modal, message, type MenuProps } from 'ant-design-vue'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import NotificationBell from './NotificationBell.vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()

const selectedKeys = ref<string[]>(['/'])
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/admin/chatRoomManage',
    label: '群聊管理',
    title: '群聊管理',
  },
  {
    key: 'others',
    label: '其他',
    title: '其他',
  },
]

const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  if (key.startsWith('/')) {
    router.push(key)
  }
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

const goToProfile = () => {
  router.push('/user/profile')
}

// ========== 创建/加入群聊状态 ==========
const createModalVisible = ref(false)
const joinModalVisible = ref(false)
const creating = ref(false)
const joining = ref(false)
const createFormRef = ref<FormInstance>()
const joinFormRef = ref<FormInstance>()

const createForm = reactive({
  roomName: '',
  description: '',
})

const createRules = {
  roomName: [
    { required: true, message: '请输入群聊名称', trigger: 'blur' },
    { min: 1, max: 50, message: '群聊名称长度应在1-50个字符之间', trigger: 'blur' },
  ],
}

const joinForm = reactive({
  inviteCode: '',
})

const joinRules = {
  inviteCode: [{ required: true, message: '请输入邀请链接或邀请码', trigger: 'blur' }],
}

const createdRoom = ref<any>(null)
const inviteInfo = ref<any>(null)

// ========== 我的群聊（弹窗）状态 ==========
const myRoomsModalVisible = ref(false)
const myRoomDetailVisible = ref(false)
const myCreatedList = ref<any[]>([])
const myCreatedLoading = ref(false)
const myCreatedPageNum = ref(1)
const myCreatedTotal = ref(0)
const selectedMyRoom = ref<any>(null)

// ========== 已加入群聊（弹窗）状态 ==========
const joinedRoomsModalVisible = ref(false)
const joinedRoomDetailVisible = ref(false)
const joinedList = ref<any[]>([])
const joinedLoading = ref(false)
const joinedPageNum = ref(1)
const joinedTotal = ref(0)
const selectedJoinedRoom = ref<any>(null)

// ========== 修改/邀请状态 ==========
const editModalVisible = ref(false)
const inviteModalVisible = ref(false)
const inviteLink = ref('')
const updating = ref(false)

const editForm = reactive({
  id: '' as any,
  roomName: '',
  description: '',
})

// ========== 数据获取 ==========
const fetchMyCreatedRooms = async () => {
  myCreatedLoading.value = true
  try {
    const res = await listMyCreatedRooms({
      chatRoomQueryRequest: { pageNum: myCreatedPageNum.value, pageSize: 5 },
    } as any)
    if (res.data?.code === 0 && res.data.data) {
      myCreatedList.value = res.data.data.records || []
      myCreatedTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('获取创建的群聊失败', error)
  } finally {
    myCreatedLoading.value = false
  }
}

const fetchJoinedRooms = async () => {
  joinedLoading.value = true
  try {
    const res = await listMyJoinedRooms({
      chatRoomQueryRequest: { pageNum: joinedPageNum.value, pageSize: 5 },
    } as any)
    if (res.data?.code === 0 && res.data.data) {
      joinedList.value = res.data.data.records || []
      joinedTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('获取已加入的群聊失败', error)
  } finally {
    joinedLoading.value = false
  }
}

const onMyCreatedPageChange = (page: number) => {
  myCreatedPageNum.value = page
  fetchMyCreatedRooms()
}

const onJoinedPageChange = (page: number) => {
  joinedPageNum.value = page
  fetchJoinedRooms()
}

// ========== 弹窗打开 ==========
const chatMenuVisible = ref(false)

const toggleChatMenu = () => {
  chatMenuVisible.value = !chatMenuVisible.value
}

const handleMenuItemClick = (key: string) => {
  chatMenuVisible.value = false
  switch (key) {
    case 'create':
      openCreateModal()
      break
    case 'join':
      openJoinModal()
      break
    case 'my-rooms':
      openMyRoomsModal()
      break
    case 'joined-rooms':
      openJoinedRoomsModal()
      break
  }
}

const openCreateModal = () => {
  if (!createModalVisible.value) {
    createdRoom.value = null
    createForm.roomName = ''
    createForm.description = ''
  }
  createModalVisible.value = true
}

const openJoinModal = () => {
  if (!joinModalVisible.value) {
    inviteInfo.value = null
    joinForm.inviteCode = ''
  }
  joinModalVisible.value = true
}

const openMyRoomsModal = () => {
  myCreatedPageNum.value = 1
  myRoomsModalVisible.value = true
}

const openJoinedRoomsModal = () => {
  joinedPageNum.value = 1
  joinedRoomsModalVisible.value = true
}

const openMyRoomDetail = (room: any) => {
  selectedMyRoom.value = room
  myRoomDetailVisible.value = true
}

const openJoinedRoomDetail = (room: any) => {
  selectedJoinedRoom.value = room
  joinedRoomDetailVisible.value = true
}

// 监听弹窗打开，自动加载数据
watch(myRoomsModalVisible, (val) => {
  if (val) {
    fetchMyCreatedRooms()
  }
})

watch(joinedRoomsModalVisible, (val) => {
  if (val) {
    fetchJoinedRooms()
  }
})

// ========== 工具方法 ==========
const formatTime = (timeStr: string | undefined): string => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const M = String(d.getMonth() + 1).padStart(2, '0')
  const D = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${M}-${D} ${h}:${m}:${s}`
}

const extractInviteCode = (value: string) => {
  if (value.includes('invite/')) {
    const parts = value.split('invite/')
    if (parts.length > 1) {
      joinForm.inviteCode = parts[1].split('?')[0].split('#')[0]
    }
  }
}

const getInviteLink = (inviteCode: string) => {
  return `${window.location.origin}/chat/invite/${inviteCode}`
}

const copyInviteLink = async () => {
  const link = createdRoom.value?.inviteCode ? getInviteLink(createdRoom.value.inviteCode) : ''
  if (link) {
    await navigator.clipboard.writeText(link)
    message.success('邀请链接已复制到剪贴板')
  }
}

const copyHeaderInviteLink = async () => {
  if (inviteLink.value) {
    await navigator.clipboard.writeText(inviteLink.value)
    message.success('邀请链接已复制到剪贴板')
  }
}

const enterRoom = (room: any) => {
  const url = window.location.origin + '/chat/room/' + room.id
  window.open(url, '_blank')
}

// ========== 创建群聊 ==========
const handleCreate = async () => {
  try {
    await createFormRef.value?.validate()
    creating.value = true

    const response = await createChatRoom({
      roomName: createForm.roomName,
      description: createForm.description,
    })

    if (response.data.code === 0) {
      const roomData = response.data.data as any
      if (roomData && roomData.id) {
        message.success('群聊创建成功！')
        fetchMyCreatedRooms()
        createModalVisible.value = false
        const url = window.location.origin + '/chat/room/' + roomData.id
        window.open(url, '_blank')
      }
    } else {
      message.error(response.data.message || '创建失败')
    }
  } catch (error) {
    console.error('创建群聊失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// ========== 加入群聊 ==========
const handleJoin = async () => {
  try {
    await joinFormRef.value?.validate()
    joining.value = true

    const response = await getInviteInfo({ inviteCode: joinForm.inviteCode })

    if (response.data.code === 0) {
      inviteInfo.value = response.data.data
    } else {
      message.error(response.data.message || '获取邀请信息失败')
    }
  } catch (error) {
    console.error('获取邀请信息失败：', error)
    message.error('获取信息失败，请检查邀请链接')
  } finally {
    joining.value = false
  }
}

const confirmJoin = async () => {
  try {
    const response = await joinRoomByInviteCode({
      inviteCode: joinForm.inviteCode,
    })

    if (response.data.code === 0) {
      message.success('加入群聊成功！')
      joinModalVisible.value = false
      fetchJoinedRooms()
      const url = window.location.origin + '/chat/room/' + inviteInfo.value.roomId
      window.open(url, '_blank')
    } else {
      message.error(response.data.message || '加入失败')
    }
  } catch (error) {
    console.error('加入群聊失败：', error)
    message.error('加入失败，请重试')
  }
}

// ========== 邀请 ==========
const handleHeaderInvite = async (room: any) => {
  try {
    const res = await generateInviteCodeApi({ roomId: room.id } as any)
    if (res.data?.code === 0 && res.data.data) {
      inviteLink.value = getInviteLink(res.data.data)
      inviteModalVisible.value = true
    } else {
      message.error(res.data?.message || '生成邀请码失败')
    }
  } catch (error) {
    message.error('生成邀请码失败')
  }
}

const closeInviteModal = () => {
  inviteModalVisible.value = false
  inviteLink.value = ''
}

// ========== 修改群聊 ==========
const openEditModal = () => {
  if (!selectedMyRoom.value) return
  editForm.id = selectedMyRoom.value.id
  editForm.roomName = selectedMyRoom.value.roomName || ''
  editForm.description = selectedMyRoom.value.description || ''
  editModalVisible.value = true
}

const handleUpdateRoom = async () => {
  updating.value = true
  try {
    const res = await updateChatRoom({
      id: editForm.id,
      roomName: editForm.roomName,
      description: editForm.description,
    })
    if (res.data?.code === 0) {
      message.success('修改成功')
      editModalVisible.value = false
      myRoomDetailVisible.value = false
      fetchMyCreatedRooms()
    } else {
      message.error(res.data?.message || '修改失败')
    }
  } catch (error) {
    message.error('修改失败')
  } finally {
    updating.value = false
  }
}

// ========== 解散群聊 ==========
const handleDissolve = (room: any) => {
  Modal.confirm({
    title: '确认解散群聊',
    content: `确定要解散群聊「${room.roomName}」吗？解散后所有成员将被移除，此操作不可撤销。`,
    okText: '确认解散',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await dissolveRoom({ id: room.id })
        if (res.data?.code === 0) {
          message.success('群聊已解散')
          myRoomDetailVisible.value = false
          fetchMyCreatedRooms()
        } else {
          message.error(res.data?.message || '解散失败')
        }
      } catch (error) {
        message.error('解散失败')
      }
    },
  })
}

// ========== 退出群聊 ==========
const handleLeaveRoom = (room: any) => {
  Modal.confirm({
    title: '确认退出群聊',
    content: `确定要退出群聊「${room.roomName}」吗？`,
    okText: '确认退出',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await leaveRoom({ roomId: room.id })
        if (res.data?.code === 0) {
          message.success('已退出群聊')
          joinedRoomDetailVisible.value = false
          fetchJoinedRooms()
        } else {
          message.error(res.data?.message || '退出失败')
        }
      } catch (error) {
        message.error('退出失败')
      }
    },
  })
}

onMounted(() => {
  if (loginUserStore.loginUser.id) {
    fetchMyCreatedRooms()
    fetchJoinedRooms()
  }
})
</script>

<style scoped>
.header {
  background: rgba(240, 253, 244, 0.78);
  padding: 0 24px;
  border-bottom: 1px solid rgba(34, 197, 94, 0.2);
  box-shadow: 0 8px 24px rgba(21, 128, 61, 0.1);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  transform: translateZ(0);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 46px;
  width: 46px;
  border-radius: 50%;
  background-image: url('../assets/logo.png');
  background-size: cover;
  background-position: center;
  border: 3px solid rgba(255, 255, 255, 0.8);
  box-shadow:
    0 4px 12px rgba(21, 128, 61, 0.3),
    inset 0 0 8px rgba(34, 197, 94, 0.2);
  transition: transform 0.9s ease-in-out;
}

.logo:hover {
  transform: rotate(360deg) scale(1.02);
}

.site-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-foreground, #0f172a);
  letter-spacing: 1px;
  text-shadow: 0 2px 6px rgba(21, 128, 61, 0.2);
  font-family: 'Lora', serif;
}

.ant-menu-horizontal {
  border-bottom: none !important;
}

.ant-menu {
  background: transparent;
}

.ant-menu-item,
.ant-menu-submenu-title {
  color: var(--color-foreground, #0f172a) !important;
  transition:
    color 0.3s,
    text-shadow 0.3s;
}

.ant-menu-item-selected,
.ant-menu-item-active {
  color: var(--color-primary, #15803d) !important;
  text-shadow: 0 2px 8px rgba(21, 128, 61, 0.3);
}

.ant-menu-horizontal > .ant-menu-item::after,
.ant-menu-horizontal > .ant-menu-submenu::after {
  border-bottom-width: 2px;
  border-bottom-color: transparent;
}

.ant-menu-horizontal > .ant-menu-item-selected::after,
.ant-menu-horizontal > .ant-menu-item-active::after {
  border-bottom-color: var(--color-primary, #15803d) !important;
}

.user-login-status {
  display: flex;
  align-items: center;
  gap: 16px;
}

.chat-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(34, 197, 94, 0.25);
  border-radius: 8px;
  padding: 6px 12px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  color: var(--color-primary, #15803d);
}

.chat-btn:hover {
  transform: scale(1.08) translateY(-2px);
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.25);
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(34, 197, 94, 0.4);
}

.notify-btn {
  width: 40px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8px;
  padding: 6px 12px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  color: var(--color-primary, #15803d);
}

.notify-btn:hover {
  transform: scale(1.08) translateY(-2px);
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.25);
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(34, 197, 94, 0.4);
}

.chat-dropdown-menu {
  min-width: 220px;
  padding: 8px 0;
}

.chat-popover-wrapper {
  position: relative;
}

.chat-popover-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 1001;
  margin-top: 4px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(34, 197, 94, 0.2);
  border-radius: 10px;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  box-shadow:
    0 8px 24px rgba(21, 128, 61, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.06);
  padding: 4px 0;
}

.chat-popover-content {
  min-width: 180px;
  line-height: 14px;
}

.popover-section-title {
  padding: 8px 12px 4px;
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
}

.popover-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 6px;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
  color: #374151;
}

.popover-menu-item:hover {
  background: rgba(34, 197, 94, 0.08);
  color: var(--color-primary, #15803d);
}

.fade-enter-active,
.fade-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.popover-menu-item .anticon {
  font-size: 14px;
}

.chat-dropdown-menu :deep(.ant-menu-item-group-title) {
  padding: 8px 16px;
  font-weight: 600;
  color: #666;
}

.chat-dropdown-menu :deep(.ant-menu-item) {
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.logout-menu-item) {
  font-size: 14px;
}

:deep(.logout-menu-item .ant-dropdown-menu-title-content) {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

:deep(.logout-menu-item .anticon) {
  font-size: 16px;
}

/* 用户下拉菜单样式 - 修复滚动条问题 */
.user-dropdown-menu {
  z-index: 1100 !important;
}

.user-dropdown-menu .ant-dropdown-menu {
  padding: 8px 0;
  min-width: 140px;
}

.user-dropdown-menu .ant-dropdown-menu-item {
  padding: 10px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.invite-info {
  margin-top: 16px;
}

.invite-link-section {
  margin-top: 16px;
  padding: 12px;
  background: rgba(34, 197, 94, 0.06);
  border-radius: 8px;
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.invite-link-section p {
  margin-bottom: 8px;
  color: var(--color-primary, #15803d);
  font-weight: 500;
}

.invite-detail {
  margin-top: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  border: 1px solid rgba(34, 197, 94, 0.1);
}

.invite-detail p {
  margin: 8px 0;
}

.empty-tip {
  padding: 32px 0;
}

.modal-room-list {
  max-height: 360px;
  overflow-y: auto;
}

.modal-room-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 6px;
  cursor: pointer;
  border-bottom: 1px solid rgba(34, 197, 94, 0.1);
  transition: background-color 0.2s;
}

.modal-room-item:hover {
  background: rgba(34, 197, 94, 0.06);
}

.modal-room-item:last-child {
  border-bottom: none;
}

.modal-room-info {
  flex: 1;
  min-width: 0;
}

.modal-room-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.modal-room-desc {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: #6b7280;
  margin-top: 2px;
}

.modal-room-desc > span:first-child {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-time {
  flex-shrink: 0;
  color: #9ca3af;
  font-size: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.room-detail-info {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.room-detail-text h3 {
  margin: 0 0 4px 0;
}

.room-detail-text p {
  margin: 4px 0;
  color: #4b5563;
}

.room-detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.link-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
}

.invite-content {
  text-align: center;
  padding: 8px 0;
}
</style>
