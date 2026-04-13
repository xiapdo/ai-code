<template>
  <div class="version-history-drawer">
    <a-drawer
      v-model:open="visible"
      title="历史版本"
      placement="right"
      :width="480"
      :get-container="false"
      :transition-duration="150"
      :mask-transition-duration="150"
      @close="handleClose"
    >
      <div v-if="loading" class="loading-container">
        <a-spin size="large" />
      </div>
      <div v-else-if="versions.length === 0" class="empty-container">
        <a-empty description="暂无历史版本" />
      </div>
      <div v-else class="version-list">
        <div
          v-for="version in versions"
          :key="version.id"
          class="version-item"
          :class="{ 'version-item--current': version.isCurrent }"
        >
          <div class="version-thumbnail">
            <img
              v-if="version.thumbnail"
              :src="version.thumbnail"
              :alt="`版本 ${version.versionNo}`"
            />
            <div v-else class="version-placeholder">
              <span>未部署 {{ version.versionNo }}</span>
            </div>
          </div>
          <div class="version-info">
            <div class="version-header">
              <span class="version-number">V {{ version.versionNo }}</span>
              <a-tag v-if="version.isCurrent" color="green">当前版本</a-tag>
            </div>
            <div class="version-meta">
              <span class="version-time">{{ formatTime(version.createTime) }}</span>
            </div>
            <div class="version-summary">{{ version.changeSummary }}</div>
          </div>
          <div class="version-actions">
            <a-space direction="vertical">
              <a-button size="small" @click="handleViewVersion(version)"> 查看对话 </a-button>
              <a-button
                v-if="!version.isCurrent"
                size="small"
                type="primary"
                ghost
                @click="handleRollback(version)"
                :loading="rollbacking === version.id"
              >
                回退
              </a-button>
            </a-space>
          </div>
        </div>
      </div>
      <div v-if="hasMore" class="load-more">
        <a-button @click="loadMore" :loading="loadingMore">加载更多</a-button>
      </div>
    </a-drawer>

    <!-- 版本预览模态框 -->
    <a-modal v-model:open="previewVisible" title="版本预览" :width="1200" :footer="null">
      <div class="preview-container">
        <iframe v-if="previewUrl" :src="previewUrl" class="preview-iframe"></iframe>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import myAxios from '../request'

const router = useRouter()

interface Props {
  appId: number | undefined
  open: boolean
}

interface Emits {
  (e: 'update:open', open: boolean): void
  (e: 'rollback-success'): void
}

interface VersionVO {
  id: number
  appId: number
  versionNo: number
  thumbnail: string
  changeSummary: string
  chatHistoryId: number
  createTime: string
  isCurrent: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const visible = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const versions = ref<VersionVO[]>([])
const hasMore = ref(false)
const pageNum = ref(1)
const pageSize = 10
const rollbacking = ref<number | null>(null)
const previewVisible = ref(false)
const previewUrl = ref('')

watch(
  () => props.open,
  (newVal) => {
    visible.value = newVal
    if (newVal) {
      loadVersions()
    }
  },
)

watch(visible, (newVal) => {
  emit('update:open', newVal)
})

const loadVersions = async () => {
  if (!props.appId) return
  loading.value = true
  pageNum.value = 1
  try {
    const res = await myAxios.post('/version/list/page', {
      appId: props.appId,
      pageNum: pageNum.value,
      pageSize: pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      versions.value = res.data.data.records || []
      hasMore.value = versions.value.length === pageSize
    } else {
      message.error('加载版本历史失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('加载版本历史失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  if (!props.appId) return
  loadingMore.value = true
  pageNum.value++
  try {
    const res = await myAxios.post('/version/list/page', {
      appId: props.appId,
      pageNum: pageNum.value,
      pageSize: pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      const newVersions = res.data.data.records || []
      versions.value = [...versions.value, ...newVersions]
      hasMore.value = newVersions.length === pageSize
    } else {
      message.error('加载更多失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('加载更多失败：' + error.message)
  } finally {
    loadingMore.value = false
  }
}

const handleViewVersion = (version: VersionVO) => {
  message.info('正在跳转到对应对话...')
  emit('update:open', false)

  // 使用版本在列表中的索引来定位
  const versionIndex = versions.value.findIndex((v) => v.id === version.id)

  router.push({
    path: `/app/chat/${props.appId}`,
    query: {
      versionIndex: versionIndex.toString(),
    },
  })
}

const handleRollback = async (version: VersionVO) => {
  if (!props.appId) return

  rollbacking.value = version.id
  try {
    const res = await myAxios.post('/version/rollback', {
      versionId: version.id,
      appId: props.appId,
    })
    if (res.data.code === 0) {
      message.success('回退成功')
      emit('rollback-success')
      loadVersions()
    } else {
      message.error('回退失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('回退失败：' + error.message)
  } finally {
    rollbacking.value = null
  }
}

const handleClose = () => {
  visible.value = false
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.loading-container,
.empty-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.version-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  transition: all 0.3s;
}

.version-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.version-item--current {
  border-color: #52c41a;
  background: #f6ffed;
}

.version-thumbnail {
  flex-shrink: 0;
  width: 120px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  background: #e8e8e8;
}

.version-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.version-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: bold;
  font-size: 18px;
}

.version-info {
  flex: 1;
  min-width: 0;
}

.version-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.version-number {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.version-meta {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.version-summary {
  font-size: 14px;
  color: #595959;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.version-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.load-more {
  text-align: center;
  padding: 16px 0;
}

.preview-container {
  width: 100%;
  height: 600px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
