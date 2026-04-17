<template>
  <div id="chatRoomManagePage">
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="群聊名称">
        <a-input v-model:value="searchParams.roomName" placeholder="输入群聊名称" />
      </a-form-item>
      <a-form-item label="房间ID">
        <a-input v-model:value="searchParams.id" placeholder="输入房间ID" />
      </a-form-item>
      <a-form-item label="群主ID">
        <a-input v-model:value="searchParams.ownerUserId" placeholder="输入群主用户ID" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="searchParams.status" placeholder="选择状态" style="width: 150px">
          <a-select-option :value="undefined">全部</a-select-option>
          <a-select-option :value="1">正常</a-select-option>
          <a-select-option :value="2">已解散</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />

    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      @change="doTableChange"
      :scroll="{ x: 1500 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'roomName'">
          <a-space>
            <a-avatar v-if="record.roomAvatar" :src="record.roomAvatar" :size="28" />
            <a-avatar v-else :size="28" style="background-color: #1890ff">
              {{ record.roomName?.charAt(0) }}
            </a-avatar>
            <span>{{ record.roomName }}</span>
          </a-space>
        </template>
        <template v-else-if="column.dataIndex === 'description'">
          <a-tooltip :title="record.description">
            <div class="desc-text">{{ record.description || '暂无描述' }}</div>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag v-if="record.status === 1" color="green">正常</a-tag>
          <a-tag v-else-if="record.status === 2" color="red">已解散</a-tag>
          <a-tag v-else color="default">未知</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'memberCount'">
          <a-tag color="blue">{{ record.memberCount || 0 }}人</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'owner'">
          <UserInfo :user="record.owner" size="small" />
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="primary" size="small" @click="viewDetail(record)">详情</a-button>
            <a-button type="primary" size="small" @click="openEditModal(record)">编辑</a-button>
            <a-button size="small" @click="viewMembers(record)">成员</a-button>
            <a-popconfirm title="确定要解散该群聊吗？" @confirm="dissolveRoom(record.id)">
              <a-button danger size="small">解散</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="detailVisible"
      title="群聊详情"
      :footer="null"
      :width="560"
      :mask="false"
      :get-container="false"
      :z-index="1050"
    >
      <a-descriptions v-if="currentRoom" :column="1" bordered size="small">
        <a-descriptions-item label="群聊ID">{{ currentRoom.id }}</a-descriptions-item>
        <a-descriptions-item label="群聊名称">{{ currentRoom.roomName }}</a-descriptions-item>
        <a-descriptions-item label="群聊头像">
          <a-avatar v-if="currentRoom.roomAvatar" :src="currentRoom.roomAvatar" :size="40" />
          <span v-else>无</span>
        </a-descriptions-item>
        <a-descriptions-item label="群聊描述">{{ currentRoom.description || '暂无' }}</a-descriptions-item>
        <a-descriptions-item label="邀请码">{{ currentRoom.inviteCode || '无' }}</a-descriptions-item>
        <a-descriptions-item label="邀请码过期时间">{{ currentRoom.inviteExpiryTime ? formatTime(currentRoom.inviteExpiryTime) : '无' }}</a-descriptions-item>
        <a-descriptions-item label="成员数">{{ currentRoom.memberCount || 0 }}人</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag v-if="currentRoom.status === 1" color="green">正常</a-tag>
          <a-tag v-else-if="currentRoom.status === 2" color="red">已解散</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="群主ID">{{ currentRoom.ownerUserId || '未知' }}</a-descriptions-item>
        <a-descriptions-item label="群主">
          <UserInfo v-if="currentRoom.owner" :user="currentRoom.owner" size="small" />
          <span v-else>未知</span>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ formatTime(currentRoom.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ formatTime(currentRoom.updateTime) }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <a-modal
      v-model:open="membersVisible"
      title="群聊成员"
      :footer="null"
      :width="600"
      :mask="false"
      :get-container="false"
      :z-index="1050"
    >
      <a-spin :spinning="membersLoading">
        <a-table
          v-if="membersVisible"
          :columns="memberColumns"
          :data-source="memberList"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'userName'">
              <a-space>
                <a-avatar :src="record.userAvatar" :size="24">
                  {{ record.userName?.charAt(0) }}
                </a-avatar>
                <span>{{ record.userName }}</span>
              </a-space>
            </template>
            <template v-else-if="column.dataIndex === 'memberRole'">
              <a-tag v-if="record.memberRole === 'owner'" color="gold">群主</a-tag>
              <a-tag v-else color="default">成员</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'joinTime'">
              {{ formatTime(record.joinTime) }}
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-modal>

    <a-modal
      v-model:open="editVisible"
      title="编辑群聊"
      :width="520"
      @ok="doEdit"
      :confirm-loading="editLoading"
      :mask="false"
      :get-container="false"
      :z-index="1050"
    >
      <a-form
        :model="editForm"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="群聊名称" name="roomName">
          <a-input v-model:value="editForm.roomName" placeholder="输入群聊名称" />
        </a-form-item>
        <a-form-item label="群聊头像URL" name="roomAvatar">
          <a-input v-model:value="editForm.roomAvatar" placeholder="输入头像图片地址" />
        </a-form-item>
        <a-form-item label="群聊描述" name="description">
          <a-textarea v-model:value="editForm.description" placeholder="输入群聊描述" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import {
    dissolveRoomByAdmin,
    getChatRoomVOByAdmin,
    listChatRoomByPageForAdmin,
    listRoomMembers,
    updateChatRoom,
} from '@/api/chatRoomController'
import UserInfo from '@/components/UserInfo.vue'
import { formatTime } from '@/utils/time'
import { message } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'

const columns = [
  {
    title: '房间ID',
    dataIndex: 'id',
    width: 100,
    fixed: 'left',
  },
  {
    title: '群主ID',
    dataIndex: 'ownerUserId',
    width: 100,
  },
  {
    title: '群聊名称',
    dataIndex: 'roomName',
    width: 180,
  },
  {
    title: '描述',
    dataIndex: 'description',
    width: 200,
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 80,
  },
  {
    title: '成员数',
    dataIndex: 'memberCount',
    width: 80,
  },
  {
    title: '群主',
    dataIndex: 'owner',
    width: 140,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 160,
  },
  {
    title: '操作',
    key: 'action',
    width: 240,
    fixed: 'right',
  },
]

const memberColumns = [
  {
    title: '用户ID',
    dataIndex: 'userId',
    width: 80,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    width: 150,
  },
  {
    title: '角色',
    dataIndex: 'memberRole',
    width: 80,
  },
  {
    title: '加入时间',
    dataIndex: 'joinTime',
    width: 160,
  },
]

const data = ref<API.ChatRoomVO[]>([])
const total = ref(0)

const searchParams = reactive<API.ChatRoomQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const fetchData = async () => {
  try {
    const res = await listChatRoomByPageForAdmin({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    console.error('获取数据失败：', error)
    message.error('获取数据失败')
  }
}

onMounted(() => {
  fetchData()
})

const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

const doTableChange = (page: { current: number; pageSize: number }) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const dissolveRoom = async (id: number | undefined) => {
  if (!id) return
  try {
    const res = await dissolveRoomByAdmin({ id })
    if (res.data.code === 0) {
      message.success('解散成功')
      fetchData()
    } else {
      message.error('解散失败：' + res.data.message)
    }
  } catch (error) {
    console.error('解散失败：', error)
    message.error('解散失败')
  }
}

const detailVisible = ref(false)
const currentRoom = ref<API.ChatRoomVO | null>(null)

const viewDetail = async (room: API.ChatRoomVO) => {
  if (!room.id) return
  try {
    const res = await getChatRoomVOByAdmin({ id: room.id })
    if (res.data.code === 0 && res.data.data) {
      currentRoom.value = res.data.data
      detailVisible.value = true
    } else {
      message.error('获取详情失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取详情失败：', error)
    message.error('获取详情失败')
  }
}

const membersVisible = ref(false)
const membersLoading = ref(false)
const memberList = ref<API.ChatRoomMember[]>([])

const viewMembers = async (room: API.ChatRoomVO) => {
  if (!room.id) return
  membersVisible.value = true
  membersLoading.value = true
  try {
    const res = await listRoomMembers({ roomId: room.id })
    if (res.data.code === 0 && res.data.data) {
      memberList.value = res.data.data
    } else {
      message.error('获取成员列表失败：' + res.data.message)
    }
  } catch (error) {
    console.error('获取成员列表失败：', error)
    message.error('获取成员列表失败')
  } finally {
    membersLoading.value = false
  }
}

const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive<API.ChatRoomUpdateRequest>({
  id: undefined,
  roomName: '',
  roomAvatar: '',
  description: '',
})

const openEditModal = (room: API.ChatRoomVO | undefined) => {
  if (!room || !room.id) return
  editForm.id = room.id
  editForm.roomName = room.roomName ?? ''
  editForm.roomAvatar = room.roomAvatar ?? ''
  editForm.description = room.description ?? ''
  editVisible.value = true
}

const doEdit = async () => {
  if (!editForm.id || !editForm.roomName?.trim()) {
    message.warning('群聊名称不能为空')
    return
  }
  editLoading.value = true
  try {
    const res = await updateChatRoom({ ...editForm })
    if (res.data.code === 0) {
      message.success('编辑成功')
      editVisible.value = false
      fetchData()
    } else {
      message.error('编辑失败：' + res.data.message)
    }
  } catch (error) {
    console.error('编辑失败：', error)
    message.error('编辑失败')
  } finally {
    editLoading.value = false
  }
}
</script>

<style scoped>
#chatRoomManagePage {
  padding: 24px;
  background: rgba(255, 255, 255, 0.72);
  margin-top: 16px;
  border-radius: 12px;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.08);
}

.desc-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}
</style>
