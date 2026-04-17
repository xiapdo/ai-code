<template>
  <a-modal
    :open="open"
    title="分享到群聊"
    :confirm-loading="sending"
    ok-text="确认分享"
    cancel-text="取消"
    @ok="handleConfirm"
    @cancel="handleCancel"
    :width="520"
  >
    <div class="share-app-info">
      <div class="share-app-cover">
        <img v-if="cover" :src="cover" :alt="appName" />
        <span v-else class="share-app-placeholder">🤖</span>
      </div>
      <div class="share-app-detail">
        <div class="share-app-name">{{ appName || '未命名应用' }}</div>
        <div class="share-app-meta">
          <a-tag v-if="codeGenType" color="green">{{ codeGenType }}</a-tag>
          <span v-if="versionNo !== undefined" class="share-app-version">v{{ versionNo }}</span>
        </div>
      </div>
    </div>

    <a-divider style="margin: 16px 0" />

    <div class="share-room-list-header">选择群聊</div>

    <a-spin :spinning="createdLoading || joinedLoading">
      <div v-if="hasAnyRoom" class="share-room-list">
        <a-radio-group v-model:value="selectedRoomId" style="width: 100%">
          <a-collapse
            v-model:activeKey="activeKeys"
            :bordered="false"
            ghost
            expand-icon-position="right"
          >
            <!-- 我的群聊 -->
            <a-collapse-panel
              key="created"
              class="share-collapse-panel"
            >
              <template #header>
                <div class="share-collapse-header">
                  <span>👑 我的群聊</span>
                  <span class="share-section-count">{{ createdTotal }} 个</span>
                </div>
              </template>
              <div v-if="createdList.length === 0" class="share-section-empty">暂无创建的群聊</div>
              <template v-else>
                <div
                  v-for="room in createdList"
                  :key="'created-' + room.id"
                  class="share-room-item"
                  :class="{ 'share-room-item--active': selectedRoomId === room.id }"
                  @click="selectedRoomId = room.id!"
                >
                  <a-radio :value="room.id" />
                  <div class="share-room-info">
                    <div class="share-room-left">
                      <span class="share-room-name">{{ room.roomName }}</span>
                      <span class="share-room-time" :title="room.createTime || ''">{{ formatTime(room.createTime) || room.createTime }}</span>
                    </div>
                    <span class="share-room-count">{{ room.memberCount || 0 }} 人</span>
                  </div>
                </div>
                <div v-if="createdTotalPages > 1" class="share-pagination">
                  <a-button
                    type="link"
                    size="small"
                    :disabled="createdPage <= 1"
                    @click.stop="createdPage = Math.max(1, createdPage - 1)"
                  >上一页</a-button>
                  <span class="share-page-info">{{ createdPage }} / {{ createdTotalPages }}</span>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="createdPage >= createdTotalPages"
                    @click.stop="createdPage = Math.min(createdTotalPages, createdPage + 1)"
                  >下一页</a-button>
                </div>
              </template>
            </a-collapse-panel>

            <!-- 已加入的群聊 -->
            <a-collapse-panel
              key="joined"
              class="share-collapse-panel"
            >
              <template #header>
                <div class="share-collapse-header">
                  <span>📂 已加入群聊</span>
                  <span class="share-section-count">{{ joinedTotal }} 个</span>
                </div>
              </template>
              <div v-if="joinedList.length === 0" class="share-section-empty">暂无加入的群聊</div>
              <template v-else>
                <div
                  v-for="room in joinedList"
                  :key="'joined-' + room.id"
                  class="share-room-item"
                  :class="{ 'share-room-item--active': selectedRoomId === room.id }"
                  @click="selectedRoomId = room.id!"
                >
                <a-radio :value="room.id" />
                <div class="share-room-info">
                  <div class="share-room-left">
                    <span class="share-room-name">{{ room.roomName }}</span>
                    <span class="share-room-time">{{ formatTime(room.createTime) }}</span>
                  </div>
                  <span class="share-room-count">{{ room.memberCount || 0 }} 人</span>
                </div>
              </div>
              <div v-if="joinedTotalPages > 1" class="share-pagination">
                <a-button
                  type="link"
                  size="small"
                  :disabled="joinedPage <= 1"
                  @click.stop="joinedPage = Math.max(1, joinedPage - 1)"
                >上一页</a-button>
                <span class="share-page-info">{{ joinedPage }} / {{ joinedTotalPages }}</span>
                <a-button
                  type="link"
                  size="small"
                  :disabled="joinedPage >= joinedTotalPages"
                  @click.stop="joinedPage = Math.min(joinedTotalPages, joinedPage + 1)"
                >下一页</a-button>
              </div>
              </template>
            </a-collapse-panel>
          </a-collapse>
        </a-radio-group>
      </div>
      <div v-else-if="!createdLoading && !joinedLoading" class="share-room-empty">
        <p>您还没有加入任何群聊</p>
        <router-link to="/chat/create-or-join">去创建或加入群聊</router-link>
      </div>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { listMyCreatedRooms, listMyJoinedRooms, sendChatMessage } from '@/api/chatRoomController'
import { useLoginUserStore } from '@/stores/loginUser'
import { message, Modal } from 'ant-design-vue'
import { computed, ref, watch } from 'vue'

interface Props {
  appId: number | string | undefined
  appName: string | undefined
  cover: string | undefined
  codeGenType: string | undefined
  versionNo: number | undefined
  open: boolean
}

interface Emits {
  (e: 'update:open', open: boolean): void
  (e: 'share-success', roomId: string | number, roomName: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const loginUserStore = useLoginUserStore()

const CREATED_PAGE_SIZE = 5
const JOINED_PAGE_SIZE = 10

const loading = ref(false)
const createdLoading = ref(false)
const joinedLoading = ref(false)
const sending = ref(false)
const selectedRoomId = ref<number | undefined>(undefined)
const activeKeys = ref<string[]>([])

const createdList = ref<API.ChatRoom[]>([])
const joinedList = ref<API.ChatRoom[]>([])
const createdPage = ref(1)
const joinedPage = ref(1)
const createdTotal = ref(0)
const joinedTotal = ref(0)

const hasAnyRoom = computed(() => createdTotal.value > 0 || joinedTotal.value > 0)

const createdTotalPages = computed(() => Math.max(1, Math.ceil(createdTotal.value / CREATED_PAGE_SIZE)))
const joinedTotalPages = computed(() => Math.max(1, Math.ceil(joinedTotal.value / JOINED_PAGE_SIZE)))

const formatTime = (time?: any): string => {
  if (time === null || time === undefined) return ''
  try {
    let d: Date
    if (typeof time === 'number') {
      d = new Date(time > 9999999999 ? time : time * 1000)
    } else if (Array.isArray(time)) {
      d = new Date(
        time[0],
        (time[1] || 1) - 1,
        time[2] || 1,
        time[3] || 0,
        time[4] || 0,
        time[5] || 0,
      )
    } else {
      d = new Date(String(time))
    }
    if (isNaN(d.getTime())) return ''
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hour = String(d.getHours()).padStart(2, '0')
    const min = String(d.getMinutes()).padStart(2, '0')
    return `${month}-${day} ${hour}:${min}`
  } catch {
    return ''
  }
}

const fetchCreatedRooms = async () => {
  createdLoading.value = true
  try {
    const res = await listMyCreatedRooms({
      chatRoomQueryRequest: { pageNum: createdPage.value, pageSize: CREATED_PAGE_SIZE },
    } as any)
    if (res.data.code === 0 && res.data.data) {
      createdList.value = res.data.data.records || []
      createdTotal.value = res.data.data.totalRow || 0
    }
  } catch {
    message.error('获取我的群聊失败')
  } finally {
    createdLoading.value = false
  }
}

const fetchJoinedRooms = async () => {
  joinedLoading.value = true
  try {
    const res = await listMyJoinedRooms({
      chatRoomQueryRequest: { pageNum: joinedPage.value, pageSize: JOINED_PAGE_SIZE },
    } as any)
    if (res.data.code === 0 && res.data.data) {
      joinedList.value = res.data.data.records || []
      joinedTotal.value = res.data.data.totalRow || 0
    }
  } catch {
    message.error('获取已加入群聊失败')
  } finally {
    joinedLoading.value = false
  }
}

watch(
  () => props.open,
  (val) => {
    if (val) {
      selectedRoomId.value = undefined
      activeKeys.value = []
      createdPage.value = 1
      joinedPage.value = 1
      createdTotal.value = 0
      joinedTotal.value = 0
      fetchCreatedRooms()
      fetchJoinedRooms()
    }
  },
)

watch(createdPage, () => { if (props.open) fetchCreatedRooms() })
watch(joinedPage, () => { if (props.open) fetchJoinedRooms() })

const handleConfirm = () => {
  if (!selectedRoomId.value) {
    message.warning('请选择一个群聊')
    return
  }

  const selectedRoom = [...createdList.value, ...joinedList.value].find((r) => r.id === selectedRoomId.value)
  const roomName = selectedRoom?.roomName || ''

  Modal.confirm({
    title: '确认分享',
    content: `确认将【${props.appName || '未命名应用'}】分享到【${roomName}】？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      sending.value = true
      try {
        const shareData = JSON.stringify({
          appId: props.appId,
          appName: props.appName,
          cover: props.cover,
          codeGenType: props.codeGenType,
          versionNo: props.versionNo,
          userId: loginUserStore.loginUser.id,
          userName: loginUserStore.loginUser.userName,
          userAvatar: loginUserStore.loginUser.userAvatar,
        })
        const res = await sendChatMessage({
          roomId: selectedRoomId.value,
          messageContent: shareData,
          messageType: 'app_share',
        })
        if (res.data.code === 0) {
          message.success(`已分享到 ${roomName}`)
          emit('share-success', selectedRoomId.value!, roomName)
          handleCancel()
        } else {
          message.error(res.data.message || '分享失败')
        }
      } catch {
        message.error('分享失败，请稍后重试')
      } finally {
        sending.value = false
      }
    },
  })
}

const handleCancel = () => {
  emit('update:open', false)
}
</script>

<style scoped>
.share-app-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: rgba(240, 253, 244, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(34, 197, 94, 0.15);
}

.share-app-cover {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(187, 247, 208, 0.5), rgba(134, 239, 172, 0.6));
  display: flex;
  align-items: center;
  justify-content: center;
}

.share-app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.share-app-placeholder {
  font-size: 28px;
  color: #86efac;
}

.share-app-detail {
  flex: 1;
  min-width: 0;
}

.share-app-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.share-app-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.share-app-version {
  font-size: 13px;
  color: #6b7280;
}

.share-room-list-header {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 12px;
}

.share-room-list {
  max-height: 360px;
  overflow-y: auto;
}

.share-collapse-panel {
  margin-bottom: 8px;
}

.share-section-empty {
  text-align: center;
  padding: 20px 0;
  color: #b0b8c4;
  font-size: 13px;
}

.share-collapse-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 12px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.share-section-count {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 400;
}

.share-room-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
  border: 1px solid transparent;
  margin-bottom: 4px;
}

.share-room-item:hover {
  background: rgba(240, 253, 244, 0.7);
  border-color: rgba(34, 197, 94, 0.2);
}

.share-room-item--active {
  background: rgba(240, 253, 244, 0.8);
  border-color: rgba(34, 197, 94, 0.35);
}

.share-room-info {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.share-room-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
  max-width: calc(100% - 80px);
}

.share-room-name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.share-room-time {
  font-size: 12px;
  color: #9ca3af;
  flex-shrink: 0;
  white-space: nowrap;
}

.share-room-count {
  font-size: 13px;
  color: #9ca3af;
  flex-shrink: 0;
  margin-left: 12px;
}

.share-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 4px;
}

.share-page-info {
  font-size: 12px;
  color: #9ca3af;
  min-width: 60px;
  text-align: center;
}

.share-empty {
  text-align: center;
  padding: 40px 0;
  color: #9ca3af;
}

.share-empty p {
  margin-bottom: 12px;
  font-size: 14px;
}

.share-empty a {
  color: #16a34a;
  font-size: 14px;
}

.share-empty a:hover {
  color: #15803d;
}
</style>
