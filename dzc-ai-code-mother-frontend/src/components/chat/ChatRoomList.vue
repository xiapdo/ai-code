<template>
  <div class="chat-room-list">
    <a-empty v-if="!loading && data.length === 0" description="暂无群聊" />
    <a-list
      v-else
      :loading="loading"
      :data-source="data"
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4 }"
    >
      <template #renderItem="{ item }">
        <a-list-item>
          <a-card hoverable class="chat-room-card" @click="handleEnter(item)">
            <template #cover>
              <div class="room-cover">
                <a-avatar :size="80" :src="item.roomAvatar || defaultAvatar" class="room-avatar" />
              </div>
            </template>
            <a-card-meta :title="item.roomName">
              <template #description>
                <div class="room-description">
                  <p class="desc-text">{{ item.description || '暂无描述' }}</p>
                  <p class="member-count">{{ item.memberCount || 0 }} 人</p>
                </div>
              </template>
            </a-card-meta>
            <template #actions v-if="showActions">
              <a-button type="link" size="small" @click.stop="handleEdit(item)">
                <EditOutlined />
                编辑
              </a-button>
              <a-button type="link" size="small" @click.stop="handleInvite(item)">
                <UserAddOutlined />
                邀请
              </a-button>
              <a-popconfirm
                title="确定解散该群聊吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm.stop="handleDissolve(item)"
              >
                <a-button type="link" size="small" danger @click.stop>
                  <DeleteOutlined />
                  解散
                </a-button>
              </a-popconfirm>
            </template>
            <template #actions v-else>
              <a-button type="primary" @click.stop="handleEnter(item)">
                <EnterOutlined />
                进入群聊
              </a-button>
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import defaultAvatar from '@/assets/logo.png'
import { DeleteOutlined, EditOutlined, EnterOutlined, UserAddOutlined } from '@ant-design/icons-vue'

interface Props {
  loading?: boolean
  data: API.ChatRoomVO[]
  showActions?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  showActions: true,
})

const emit = defineEmits<{
  refresh: []
  edit: [record: API.ChatRoomVO]
  dissolve: [record: API.ChatRoomVO]
  invite: [record: API.ChatRoomVO]
  enter: [record: API.ChatRoomVO]
}>()

const handleEnter = (record: API.ChatRoomVO) => {
  emit('enter', record)
}

const handleEdit = (record: API.ChatRoomVO) => {
  emit('edit', record)
}

const handleDissolve = (record: API.ChatRoomVO) => {
  emit('dissolve', record)
}

const handleInvite = (record: API.ChatRoomVO) => {
  emit('invite', record)
}
</script>

<style scoped>
.chat-room-list {
  padding: 16px 0;
}

.chat-room-card {
  cursor: pointer;
  transition: all 0.3s;
}

.chat-room-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.room-cover {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 120px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.room-avatar {
  border: 4px solid #fff;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.room-description {
  text-align: center;
}

.desc-text {
  color: #666;
  margin: 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.member-count {
  color: #1890ff;
  font-size: 12px;
  margin: 0;
}

:deep(.ant-card-actions) {
  border-top: 1px solid #f0f0f0;
  padding: 8px 0;
}
</style>
