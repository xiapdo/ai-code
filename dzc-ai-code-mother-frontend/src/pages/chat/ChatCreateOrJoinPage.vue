<template>
  <div class="chat-create-or-join-page">
    <a-card class="main-card" title="群聊" :bordered="false">
      <a-tabs v-model:activeKey="activeTab" centered @change="handleTabChange">
        <a-tab-pane key="create" tab="创建群聊">
          <a-form
            ref="formRef"
            :model="createForm"
            :rules="createRules"
            layout="vertical"
          >
            <a-form-item label="群聊名称" name="roomName">
              <a-input
                v-model:value="createForm.roomName"
                placeholder="请输入群聊名称"
                size="large"
              />
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
            <a-form-item>
              <a-button type="primary" size="large" :loading="creating" @click="handleCreate">
                创建群聊
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="join" tab="加入群聊">
          <a-form
            ref="joinFormRef"
            :model="joinForm"
            :rules="joinRules"
            layout="vertical"
          >
            <a-form-item label="邀请链接" name="inviteCode">
              <a-input
                v-model:value="joinForm.inviteCode"
                placeholder="请输入邀请链接或邀请码"
                size="large"
                @input="extractInviteCode"
              >
                <template #prefix>
                  <LinkOutlined />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" size="large" :loading="joining" @click="handleJoin">
                加入群聊
              </a-button>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane key="my-created" tab="我的群聊">
          <a-spin :spinning="createdLoading">
            <div v-if="myCreatedList.length === 0 && !createdLoading" class="empty-tip">
              <a-empty description="暂无创建的群聊" />
            </div>
            <div v-else class="room-list">
              <div
                v-for="room in myCreatedList"
                :key="room.id"
                class="room-list-item"
                @click="openMyRoomDetail(room)"
              >
                <a-avatar :src="room.roomAvatar" style="background-color: #1890ff">
                  {{ room.roomName?.charAt(0) }}
                </a-avatar>
                <div class="room-list-item-content">
                  <div class="room-list-item-title">{{ room.roomName }}</div>
                  <div class="room-list-item-desc">{{ room.description || '暂无描述' }} · {{ room.memberCount }}人</div>
                </div>
              </div>
            </div>
            <div class="pagination-wrapper" v-if="createdTotal > 5">
              <a-pagination
                v-model:current="createdPageNum"
                :total="createdTotal"
                :page-size="5"
                size="small"
                @change="onCreatedPageChange"
              />
            </div>
          </a-spin>
        </a-tab-pane>

        <a-tab-pane key="joined" tab="已加入群聊">
          <a-spin :spinning="joinedLoading">
            <div v-if="joinedList.length === 0 && !joinedLoading" class="empty-tip">
              <a-empty description="暂无已加入的群聊" />
            </div>
            <div v-else class="room-list">
              <div
                v-for="room in joinedList"
                :key="room.id"
                class="room-list-item"
                @click="openJoinedRoomDetail(room)"
              >
                <a-avatar :src="room.roomAvatar" style="background-color: #52c41a">
                  {{ room.roomName?.charAt(0) }}
                </a-avatar>
                <div class="room-list-item-content">
                  <div class="room-list-item-title">{{ room.roomName }}</div>
                  <div class="room-list-item-desc">{{ room.description || '暂无描述' }} · {{ room.memberCount }}人</div>
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
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 我的群聊 - 详情弹窗 -->
    <a-modal
      v-model:open="myRoomModalVisible"
      :title="selectedRoom?.roomName"
      :footer="null"
      :width="480"
    >
      <div v-if="selectedRoom" class="room-detail">
        <div class="room-detail-info">
          <a-avatar :src="selectedRoom.roomAvatar" :size="48" style="background-color: #1890ff">
            {{ selectedRoom.roomName?.charAt(0) }}
          </a-avatar>
          <div class="room-detail-text">
            <h3>{{ selectedRoom.roomName }}</h3>
            <p>{{ selectedRoom.description || '暂无描述' }}</p>
            <p>成员：{{ selectedRoom.memberCount }}人</p>
          </div>
        </div>

        <div class="room-detail-actions">
          <a-button type="primary" @click="enterRoomNewTab(selectedRoom)">
            进入群聊
          </a-button>
          <a-button @click="handleInvite(selectedRoom)">
            邀请
          </a-button>
          <a-button @click="openEditModal">
            修改
          </a-button>
          <a-button danger @click="handleDissolve(selectedRoom)">
            解散
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- 已加入群聊 - 详情弹窗 -->
    <a-modal
      v-model:open="joinedRoomModalVisible"
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
          <a-button type="primary" @click="enterRoomNewTab(selectedJoinedRoom)">
            进入群聊
          </a-button>
          <a-button danger @click="handleLeaveRoom(selectedJoinedRoom)">
            退出群聊
          </a-button>
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
            <a-button type="primary" @click="copyInviteLink">
              复制
            </a-button>
          </a-input-group>
          <p class="link-tip">链接有效期为5分钟</p>
        </div>
      </div>
    </a-modal>

    <!-- 创建成功弹窗 -->
    <a-modal
      v-model:open="createSuccessModalVisible"
      title="创建成功"
      :footer="null"
    >
      <div v-if="createdRoom" class="invite-confirm-content">
        <a-alert type="success" show-icon :message="`群聊「${createdRoom.roomName}」创建成功！`" />
        <div class="invite-link-section" style="margin-top: 16px">
          <h4>您的邀请链接：</h4>
          <a-input-group compact>
            <a-input
              :value="getInviteLink(createdRoom.inviteCode)"
              readonly
              style="width: calc(100% - 100px)"
            />
            <a-button type="primary" @click="copyCreatedInviteLink">复制</a-button>
          </a-input-group>
          <p class="link-tip">复制此链接分享给其他人，邀请他们加入群聊</p>
        </div>
        <div style="text-align: right; margin-top: 16px">
          <a-button @click="createSuccessModalVisible = false">关闭</a-button>
          <a-button type="primary" style="margin-left: 8px" @click="enterCreatedRoomNewTab">
            进入群聊
          </a-button>
        </div>
      </div>
    </a-modal>

    <!-- 加入群聊确认弹窗 -->
    <a-modal
      v-model:open="joinConfirmModalVisible"
      title="确认加入群聊"
      @ok="handleConfirmJoin"
      :confirm-loading="joining"
    >
      <div v-if="inviteInfo">
        <a-alert type="info" show-icon :message="`您将被邀请加入：${inviteInfo.roomName}`" />
        <div class="invite-info" style="margin-top: 12px">
          <p><strong>群聊名称：</strong>{{ inviteInfo.roomName }}</p>
          <p><strong>创建者：</strong>{{ inviteInfo.creatorName }}</p>
          <p><strong>当前人数：</strong>{{ inviteInfo.memberCount }}人</p>
          <p><strong>描述：</strong>{{ inviteInfo.description || '暂无描述' }}</p>
        </div>
      </div>
    </a-modal>
  </div>
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
import { LinkOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { Modal, message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const formRef = ref<FormInstance>()
const joinFormRef = ref<FormInstance>()

const activeTab = ref('create')
const creating = ref(false)
const joining = ref(false)
const updating = ref(false)
const createdLoading = ref(false)
const joinedLoading = ref(false)

const createSuccessModalVisible = ref(false)
const joinConfirmModalVisible = ref(false)
const myRoomModalVisible = ref(false)
const joinedRoomModalVisible = ref(false)
const editModalVisible = ref(false)
const inviteModalVisible = ref(false)
const inviteLink = ref('')

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

const inviteInfo = ref<any>(null)
const createdRoom = ref<any>(null)
const selectedRoom = ref<any>(null)
const selectedJoinedRoom = ref<any>(null)

const editForm = reactive({
  id: '' as any,
  roomName: '',
  description: '',
})

const myCreatedList = ref<any[]>([])
const joinedList = ref<any[]>([])
const createdPageNum = ref(1)
const joinedPageNum = ref(1)
const createdTotal = ref(0)
const joinedTotal = ref(0)

const handleTabChange = (key: string) => {
  if (key === 'my-created') {
    fetchMyCreatedRooms()
  } else if (key === 'joined') {
    fetchJoinedRooms()
  }
}

const fetchMyCreatedRooms = async () => {
  createdLoading.value = true
  try {
    const res = await listMyCreatedRooms({
      chatRoomQueryRequest: { pageNum: createdPageNum.value, pageSize: 5 }
    } as any)
    if (res.data?.code === 0 && res.data.data) {
      myCreatedList.value = res.data.data.records || []
      createdTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('获取创建的群聊失败', error)
  } finally {
    createdLoading.value = false
  }
}

const fetchJoinedRooms = async () => {
  joinedLoading.value = true
  try {
    const res = await listMyJoinedRooms({
      chatRoomQueryRequest: { pageNum: joinedPageNum.value, pageSize: 5 }
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

const onCreatedPageChange = (page: number) => {
  createdPageNum.value = page
  fetchMyCreatedRooms()
}

const onJoinedPageChange = (page: number) => {
  joinedPageNum.value = page
  fetchJoinedRooms()
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
  if (inviteLink.value) {
    await navigator.clipboard.writeText(inviteLink.value)
    message.success('邀请链接已复制到剪贴板')
  }
}

const copyCreatedInviteLink = async () => {
  if (createdRoom.value?.inviteCode) {
    await navigator.clipboard.writeText(getInviteLink(createdRoom.value.inviteCode))
    message.success('邀请链接已复制到剪贴板')
  }
}

const enterRoomNewTab = (room: any) => {
  const url = window.location.origin + '/chat/room/' + room.id
  window.open(url, '_blank')
}

const enterCreatedRoomNewTab = () => {
  if (!createdRoom.value) return
  const url = window.location.origin + '/chat/room/' + createdRoom.value.id
  window.open(url, '_blank')
  createSuccessModalVisible.value = false
}

const openMyRoomDetail = (room: any) => {
  selectedRoom.value = room
  myRoomModalVisible.value = true
}

const openJoinedRoomDetail = (room: any) => {
  selectedJoinedRoom.value = room
  joinedRoomModalVisible.value = true
}

const handleInvite = async (room: any) => {
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

const openEditModal = () => {
  if (!selectedRoom.value) return
  editForm.id = selectedRoom.value.id
  editForm.roomName = selectedRoom.value.roomName || ''
  editForm.description = selectedRoom.value.description || ''
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
      myRoomModalVisible.value = false
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
          myRoomModalVisible.value = false
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
          joinedRoomModalVisible.value = false
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

const handleCreate = async () => {
  try {
    await formRef.value?.validate()
    creating.value = true

    const response = await createChatRoom({
      roomName: createForm.roomName,
      description: createForm.description,
    })

    if (response.data.code === 0) {
      const roomData = response.data.data as any
      createdRoom.value = {
        id: roomData?.id,
        inviteCode: roomData?.inviteCode || '',
        roomName: roomData?.roomName,
        description: roomData?.description,
      }
      createSuccessModalVisible.value = true
      message.success('群聊创建成功！')
      createForm.roomName = ''
      createForm.description = ''
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

const handleJoin = async () => {
  try {
    await joinFormRef.value?.validate()
    joining.value = true

    const response = await getInviteInfo({ inviteCode: joinForm.inviteCode } as any)

    if (response.data.code === 0) {
      inviteInfo.value = response.data.data
      joinConfirmModalVisible.value = true
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

const handleConfirmJoin = async () => {
  if (!inviteInfo.value) return
  joining.value = true
  try {
    const response = await joinRoomByInviteCode({
      inviteCode: joinForm.inviteCode,
    })

    if (response.data.code === 0) {
      message.success('加入群聊成功！')
      joinConfirmModalVisible.value = false
      const url = window.location.origin + '/chat/room/' + inviteInfo.value.roomId
      window.open(url, '_blank')
    } else {
      message.error(response.data.message || '加入失败')
    }
  } catch (error) {
    console.error('加入群聊失败：', error)
    message.error('加入失败，请重试')
  } finally {
    joining.value = false
  }
}

onMounted(() => {
  fetchMyCreatedRooms()
})
</script>

<style scoped>
.chat-create-or-join-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.main-card {
  width: 100%;
  max-width: 800px;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.empty-tip {
  padding: 40px 0;
}

.room-list {
  display: flex;
  flex-direction: column;
}

.room-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.room-list-item:hover {
  background-color: #f5f5f5;
}

.room-list-item:last-child {
  border-bottom: none;
}

.room-list-item-content {
  flex: 1;
  min-width: 0;
}

.room-list-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-list-item-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
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
  color: #666;
}

.room-detail-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.invite-confirm-content {
  padding: 10px 0;
}

.invite-info {
  margin: 20px 0;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.invite-info p {
  margin: 8px 0;
}

.invite-link-section {
  margin-top: 20px;
  padding: 15px;
  background: #f0f7ff;
  border-radius: 8px;
  border: 1px solid #91caff;
}

.invite-link-section h4 {
  margin-bottom: 10px;
  color: #1890ff;
}

.link-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

.invite-content {
  text-align: center;
  padding: 16px 0;
}

.invite-link {
  margin: 16px 0;
}

@media (max-width: 768px) {
  .chat-create-or-join-page {
    padding: 20px 10px;
  }

  .main-card {
    margin: 0 10px;
  }
}
</style>
