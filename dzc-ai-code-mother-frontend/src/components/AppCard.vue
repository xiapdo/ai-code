<template>
  <div class="app-card" :class="{ 'app-card--featured': featured }" @click="handleCardClick">
    <div class="app-preview">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="app-placeholder">
        <img src="../assets/1.webp" width="1440px" height="1440px" alt="小艾" />
      </div>
      <!-- 浏览量图标（右下角，仅部署后的作品显示） -->
      <div v-if="app.deployKey" class="view-count-badge">
        <EyeOutlined class="view-count-icon" />
        <span class="view-count-text">{{ formatViewCount(app.viewCount || 0) }}</span>
      </div>
      <div class="app-overlay">
        <a-space direction="vertical" :size="12">
          <a-space>
            <a-button type="primary" @click.stop="handleViewChat">查看对话</a-button>
            <a-button v-if="app.deployKey" type="default" @click.stop="handleViewWork"
              >查看作品</a-button
            >
            <a-button v-if="featured" @click.stop="handleOpenComment">
              <template #icon><MessageOutlined /></template>
              评论 {{ app.commentCount ? app.commentCount : '' }}
            </a-button>
          </a-space>
        </a-space>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-left">
        <a-avatar :src="app.user?.userAvatar" :size="40">
          {{ app.user?.userName?.charAt(0) || 'U' }}
        </a-avatar>
      </div>
      <div class="app-info-right">
        <h3 class="app-title">{{ app.appName || '未命名应用' }}</h3>
        <div class="app-meta">
          <span class="app-author">
            {{ app.user?.userName || (featured ? '官方' : '未知用户') }}
          </span>
        </div>
      </div>
    </div>

    <AppCommentDrawer
      v-model:open="commentDrawerVisible"
      :app-id="app.id as number"
      :app-name="app.appName"
      @comment-added="handleCommentAdded"
    />
  </div>
</template>

<script setup lang="ts">
import { EyeOutlined, MessageOutlined } from '@ant-design/icons-vue'
import { ref } from 'vue'
import AppCommentDrawer from './AppCommentDrawer.vue'

interface Props {
  app: API.AppVO
  featured?: boolean
  showInteraction?: boolean
  isOwner?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined, showInteraction?: boolean): void
  (e: 'view-work', app: API.AppVO): void
  (e: 'view-stat', appId: number): void
  (e: 'click', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
  showInteraction: false,
})

const emit = defineEmits<Emits>()

const commentDrawerVisible = ref(false)

/**
 * 格式化浏览量显示
 * @param count 浏览量
 * @returns 格式化后的字符串
 */
const formatViewCount = (count: number): string => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + 'w'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'k'
  }
  return String(count)
}

const handleViewChat = () => {
  emit('view-chat', props.app.id, props.showInteraction)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}

const handleOpenComment = () => {
  commentDrawerVisible.value = true
}

const handleCommentAdded = () => {}

const handleCardClick = () => {
  emit('click', props.app)
}
</script>

<style scoped>
.app-card {
  background: rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(21, 128, 61, 0.1);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  transition:
    transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    box-shadow 0.3s,
    border-color 0.3s;
  cursor: pointer;
}

.card-entrance {
  opacity: 0;
  transform: translateY(30px) scale(0.96);
}

.card-visible {
  animation: cardEntrance 0.5s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}

@keyframes cardEntrance {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.app-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 40px rgba(21, 128, 61, 0.18);
  border-color: rgba(34, 197, 94, 0.3);
}

.app-preview {
  height: 180px;
  background:
    radial-gradient(circle at 50% 0%, rgba(240, 253, 244, 0.4) 0, transparent 70%),
    linear-gradient(135deg, rgba(187, 247, 208, 0.5), rgba(134, 239, 172, 0.6));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-card:hover .app-preview img {
  transform: scale(1.08);
}

.app-placeholder {
  width: 140px;
  height: auto;
  font-size: 48px;
  color: #86efac;
}

/* 浏览量标签（右下角） */
.view-count-badge {
  position: absolute;
  bottom: 10px;
  right: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgb(0 0 0 / 7%);
  border-radius: 20px;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 2;
  transition: all 0.3s;
}

.view-count-badge:hover {
  background: rgba(0, 0, 0, 0.7);
}

.view-count-icon {
  font-size: 13px;
  color: #fff;
}

.view-count-text {
  font-size: 12px;
  color: #fff;
  font-weight: 600;
}

.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(21, 128, 61, 0.55), rgba(5, 150, 105, 0.5));
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.app-info {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-info-left {
  flex-shrink: 0;
}

.app-info-right {
  flex: 1;
  min-width: 0;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 4px;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-author {
  font-size: 14px;
  color: #4b5563;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-views {
  font-size: 12px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 4px;
}

.view-icon {
  font-size: 12px;
}
</style>
