<template>
  <div id="chatRoomListPage">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>我的群聊</h1>
      <a-button type="primary" @click="showCreateModal">
        <PlusOutlined />
        创建群聊
      </a-button>
    </div>

    <!-- 标签页切换 -->
    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <a-tab-pane key="created" tab="我创建的">
        <ChatRoomList
          :loading="loading"
          :data="createdRoomList"
          @refresh="fetchCreatedRooms"
          @edit="handleEdit"
          @dissolve="handleDissolve"
          @invite="showInviteModal"
        />
      </a-tab-pane>
      <a-tab-pane key="joined" tab="我加入的">
        <ChatRoomList
          :loading="loading"
          :data="joinedRoomList"
          @refresh="fetchJoinedRooms"
          @enter="enterRoom"
        />
      </a-tab-pane>
    </a-tabs>

    <!-- 创建群聊弹窗 -->
    <a-modal
      v-model:open="createModalVisible"
      title="创建群聊"
      @ok="handleCreate"
      @cancel="closeCreateModal"
      :confirmLoading="createLoading"
    >
      <a-form :model="createForm" layout="vertical">
        <a-form-item label="群聊名称" required>
          <a-input
            v-model:value="createForm.roomName"
            placeholder="请输入群聊名称"
            :maxLength="256"
          />
        </a-form-item>
        <a-form-item label="群聊描述">
          <a-textarea
            v-model:value="createForm.description"
            placeholder="请输入群聊描述（选填）"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑群聊弹窗 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑群聊"
      @ok="handleUpdate"
      @cancel="closeEditModal"
      :confirmLoading="updateLoading"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="群聊名称">
          <a-input
            v-model:value="editForm.roomName"
            placeholder="请输入群聊名称"
            :maxLength="256"
          />
        </a-form-item>
        <a-form-item label="群聊描述">
          <a-textarea v-model:value="editForm.description" placeholder="请输入群聊描述" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 邀请码弹窗 -->
    <a-modal
      v-model:open="inviteModalVisible"
      title="邀请成员"
      @cancel="closeInviteModal"
      :footer="null"
    >
      <div class="invite-content">
        <p>分享此链接邀请成员加入群聊（5分钟内有效）：</p>
        <div class="invite-link">
          <a-input
            v-model:value="inviteLink"
            readonly
            addon-after="复制"
            @click-addon="copyInviteLink"
          />
        </div>
        <p class="invite-tip">每个邀请码只能使用一次，过期后请重新生成</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {
  createChatRoom,
  dissolveRoom,
  generateInviteCode,
  listMyCreatedRooms,
  listMyJoinedRooms,
  updateChatRoom,
} from '@/api/chatRoomController'
import ChatRoomList from '@/components/chat/ChatRoomList.vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 状态
const activeTab = ref('created')
const loading = ref(false)
const createdRoomList = ref<API.ChatRoomVO[]>([])
const joinedRoomList = ref<API.ChatRoomVO[]>([])

// 创建弹窗
const createModalVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  roomName: '',
  description: '',
})

// 编辑弹窗
const editModalVisible = ref(false)
const updateLoading = ref(false)
const editForm = reactive<API.ChatRoomUpdateRequest>({
  id: undefined,
  roomName: '',
  description: '',
})

// 邀请弹窗
const inviteModalVisible = ref(false)
const inviteLink = ref('')

// 获取创建的群聊
const fetchCreatedRooms = async () => {
  loading.value = true
  try {
    const res = await listMyCreatedRooms({
      chatRoomQueryRequest: { pageNum: 1, pageSize: 20 },
    } as any)
    if (res.data?.code === 0 && res.data.data) {
      createdRoomList.value = res.data.data.records || []
    }
  } catch (error) {
    message.error('获取群聊列表失败')
  } finally {
    loading.value = false
  }
}

// 获取加入的群聊
const fetchJoinedRooms = async () => {
  loading.value = true
  try {
    const res = await listMyJoinedRooms({
      chatRoomQueryRequest: { pageNum: 1, pageSize: 20 },
    } as any)
    if (res.data?.code === 0 && res.data.data) {
      joinedRoomList.value = res.data.data.records || []
    }
  } catch (error) {
    message.error('获取群聊列表失败')
  } finally {
    loading.value = false
  }
}

// 标签切换
const handleTabChange = (key: string) => {
  if (key === 'created') {
    fetchCreatedRooms()
  } else {
    fetchJoinedRooms()
  }
}

// 显示创建弹窗
const showCreateModal = () => {
  createModalVisible.value = true
  createForm.roomName = ''
  createForm.description = ''
}

// 关闭创建弹窗
const closeCreateModal = () => {
  createModalVisible.value = false
}

// 创建群聊
const handleCreate = async () => {
  if (!createForm.roomName.trim()) {
    message.error('请输入群聊名称')
    return
  }
  createLoading.value = true
  try {
    const res = await createChatRoom(createForm)
    if (res.data?.code === 0) {
      message.success('创建成功')
      closeCreateModal()
      fetchCreatedRooms()
    } else {
      message.error(res.data?.message || '创建失败')
    }
  } catch (error) {
    message.error('创建失败')
  } finally {
    createLoading.value = false
  }
}

// 显示编辑弹窗
const handleEdit = (record: API.ChatRoomVO) => {
  editForm.id = record.id
  editForm.roomName = record.roomName || ''
  editForm.description = record.description || ''
  editModalVisible.value = true
}

// 关闭编辑弹窗
const closeEditModal = () => {
  editModalVisible.value = false
}

// 更新群聊
const handleUpdate = async () => {
  updateLoading.value = true
  try {
    const res = await updateChatRoom(editForm)
    if (res.data?.code === 0) {
      message.success('更新成功')
      closeEditModal()
      fetchCreatedRooms()
    } else {
      message.error(res.data?.message || '更新失败')
    }
  } catch (error) {
    message.error('更新失败')
  } finally {
    updateLoading.value = false
  }
}

// 解散群聊
const handleDissolve = async (record: API.ChatRoomVO) => {
  try {
    const res = await dissolveRoom({ id: record.id })
    if (res.data?.code === 0) {
      message.success('解散成功')
      fetchCreatedRooms()
    } else {
      message.error(res.data?.message || '解散失败')
    }
  } catch (error) {
    message.error('解散失败')
  }
}

// 显示邀请弹窗
const showInviteModal = async (record: API.ChatRoomVO) => {
  try {
    const res = await generateInviteCode(record.id!)
    if (res.data?.code === 0 && res.data.data) {
      inviteLink.value = `${window.location.origin}/chat/invite/${res.data.data}`
      inviteModalVisible.value = true
    } else {
      message.error(res.data?.message || '生成邀请码失败')
    }
  } catch (error) {
    message.error('生成邀请码失败')
  }
}

// 关闭邀请弹窗
const closeInviteModal = () => {
  inviteModalVisible.value = false
  inviteLink.value = ''
}

// 复制邀请链接
const copyInviteLink = () => {
  navigator.clipboard.writeText(inviteLink.value).then(() => {
    message.success('已复制到剪贴板')
  })
}

// 进入群聊
const enterRoom = (record: API.ChatRoomVO) => {
  router.push(`/chat/room/${record.id}`)
}

onMounted(() => {
  fetchCreatedRooms()
})
</script>

<style scoped>
#chatRoomListPage {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
}

.invite-content {
  text-align: center;
  padding: 16px 0;
}

.invite-link {
  margin: 16px 0;
}

.invite-tip {
  color: #999;
  font-size: 12px;
}
</style>
