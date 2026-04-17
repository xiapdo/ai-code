<template>
  <div class="app-card">
    <div class="app-preview">
      <img v-if="cardData.cover" :src="cardData.cover" :alt="cardData.appName" />
      <div v-else class="app-placeholder">
        <span>🤖</span>
      </div>
      <div class="app-overlay">
        <a-space>
          <a-button type="primary" @click="handleViewChat">查看对话</a-button>
          <a-button type="default" @click="handleViewWork">查看作品</a-button>
        </a-space>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-left">
        <a-avatar :src="cardData.userAvatar" :size="32">
          {{ cardData.userName?.charAt(0) || 'U' }}
        </a-avatar>
      </div>
      <div class="app-info-right">
        <h3 class="app-title">{{ cardData.appName || '未命名应用' }}</h3>
        <p class="app-author">{{ cardData.userName || '未知用户' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getStaticPreviewUrl } from '@/config/env'

interface Props {
  message: API.ChatRoomMessage
}

const props = defineProps<Props>()
const router = useRouter()

const cardData = computed(() => {
  try {
    const data = JSON.parse(props.message.messageContent || '{}')
    return {
      appId: data.appId,
      appName: data.appName || '',
      cover: data.cover || '',
      codeGenType: data.codeGenType || '',
      versionNo: data.versionNo,
      userId: data.userId,
      userName: data.userName || '',
      userAvatar: data.userAvatar || '',
    }
  } catch {
    return {
      appId: '',
      appName: '',
      cover: '',
      codeGenType: '',
      versionNo: 0,
      userId: '',
      userName: '',
      userAvatar: '',
    }
  }
})

const handleViewChat = () => {
  if (cardData.value.appId) {
    router.push(`/app/chat/${cardData.value.appId}?view=1`)
  }
}

const handleViewWork = () => {
  if (cardData.value.codeGenType && cardData.value.appId) {
    const url = getStaticPreviewUrl(cardData.value.codeGenType, String(cardData.value.appId))
    window.open(url, '_blank')
  }
}
</script>

<style scoped>
.app-card {
  max-width: 280px;
  background: rgba(255, 255, 255, 0.72);
  border-radius: 12px;
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

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(21, 128, 61, 0.18);
  border-color: rgba(34, 197, 94, 0.3);
}

.app-preview {
  height: 140px;
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
}

.app-placeholder {
  font-size: 36px;
  color: #86efac;
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
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-info-left {
  flex-shrink: 0;
}

.app-info-right {
  flex: 1;
  min-width: 0;
}

.app-title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 2px;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-author {
  font-size: 12px;
  color: #4b5563;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
