<template>
  <div class="ai-message-wrapper">
    <div class="ai-message">
      <div class="ai-avatar">
        <a-avatar :src="aiAvatar" :size="36" />
      </div>
      <div :class="['ai-content', { 'ai-content--bounce': shouldBounce }]">
        <div v-if="isThinking" class="ai-thinking-compact">
          <span class="thinking-dot"></span>
          <span class="thinking-dot"></span>
          <span class="thinking-dot"></span>
        </div>
        <template v-else>
          <div class="ai-header">
            <span class="ai-name">AI 助手</span>
            <span class="ai-badge">小鼠</span>
            <span class="ai-time">{{ formatTime(message.createTime) }}</span>
          </div>
          <div class="ai-body">
            <div v-if="displayContent" class="ai-markdown">
              <MarkdownRenderer :content="displayContent" />
              <span v-if="isStreaming" class="cursor-blink">|</span>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { default as aiAvatar } from '@/assets/12.jpg'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { computed, ref, watch } from 'vue'

interface Props {
  message: API.ChatMessageVO
  streamingContent?: string
  isThinking?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  streamingContent: '',
  isThinking: false,
})

const CHAR_DELAY = 65
const PUNCTUATION_DELAY = 180

const rawContent = computed(() => {
  if (props.message.messageType === 'ai_complete') return props.message.messageContent || ''
  return props.streamingContent || ''
})

const isStreaming = computed(() => props.message.messageType !== 'ai_complete' && rawContent.value.length > 0)

const hasBounced = ref(false)
const shouldBounce = computed(() => hasBounced.value === false && rawContent.value.length > 0 && !props.isThinking)

watch(shouldBounce, (val) => { if (val) hasBounced.value = true })

const typedContent = ref('')
let timer: ReturnType<typeof setTimeout> | null = null

const displayContent = computed(() => (isStreaming.value ? typedContent.value : rawContent.value))

watch(
  () => rawContent.value,
  (target) => {
    if (!target || props.isThinking) {
      stop()
      typedContent.value = ''
      return
    }
    if (!isStreaming.value) return
    resumeTyping(target)
  },
  { immediate: true },
)

function resumeTyping(target: string) {
  stop()
  let i = Math.min(typedContent.value.length, target.length)
  function tick() {
    if (i >= target.length) {
      typedContent.value = target
      stop()
      return
    }
    i++
    typedContent.value = target.substring(0, i)
    const ch = target.charAt(i - 1)
    const delay = /[，。！？、：；,.!?;:\n]/.test(ch) ? PUNCTUATION_DELAY : CHAR_DELAY
    timer = setTimeout(tick, delay)
  }
  tick()
}

function stop() {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

const formatTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}
</script>

<style scoped>
.ai-message-wrapper {
  display: flex;
  margin-bottom: 16px;
}

.ai-message {
  display: flex;
  align-items: flex-start;
  max-width: 75%;
}

.ai-avatar {
  margin-right: 8px;
  flex-shrink: 0;
}

.ai-content {
  background: linear-gradient(135deg, rgba(236, 253, 245, 0.96), rgba(220, 252, 231, 0.9));
  border-radius: 16px;
  border-top-left-radius: 4px;
  padding: 8px 14px;
  box-shadow:
    0 4px 16px rgba(21, 128, 61, 0.12),
    0 1px 3px rgba(34, 197, 94, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(167, 243, 208, 0.5);
  border-left: 3px solid #22c55e;
  min-width: 48px;
}

.ai-content--bounce {
  animation: ai-bounce-in 0.45s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

@keyframes ai-bounce-in {
  0% {
    opacity: 0;
    transform: translateY(14px) scale(0.95);
    box-shadow:
      0 0 0 rgba(21, 128, 61, 0),
      0 0 0 rgba(34, 197, 94, 0);
  }
  50% {
    opacity: 1;
    transform: translateY(-3px) scale(1.01);
    box-shadow:
      0 8px 24px rgba(21, 128, 61, 0.18),
      0 2px 6px rgba(34, 197, 94, 0.12),
      inset 0 1px 0 rgba(255, 255, 255, 0.6);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
    box-shadow:
      0 4px 16px rgba(21, 128, 61, 0.12),
      0 1px 3px rgba(34, 197, 94, 0.08),
      inset 0 1px 0 rgba(255, 255, 255, 0.6);
  }
}

.ai-thinking-compact {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 4px;
}

.thinking-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #22c55e;
  opacity: 0.6;
  animation: thinking-pulse 1.4s infinite ease-in-out both;
}

.thinking-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.thinking-dot:nth-child(2) {
  animation-delay: -0.16s;
}

.thinking-dot:nth-child(3) {
  animation-delay: 0s;
}

@keyframes thinking-pulse {
  0%,
  80%,
  100% {
    transform: scale(0.65);
    opacity: 0.35;
  }
  40% {
    transform: scale(1);
    opacity: 0.9;
  }
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 4px;
  font-size: 14px;
  white-space: nowrap;
  flex-wrap: nowrap;
}

.ai-name {
  font-weight: 600;
  color: #166534;
}

.ai-badge {
  display: inline-block;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: white;
  padding: 0px 5px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
  line-height: 1.4;
}

.ai-time {
  opacity: 0.5;
  font-size: 12px;
  color: #15803d;
  margin-left: 2px;
}

.ai-body {
  color: #14532d;
  line-height: 1.65;
  font-size: 14px;
  word-wrap: break-word;
}

.cursor-blink {
  color: #22c55e;
  font-weight: 400;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.ai-markdown {
  max-width: 100%;
  overflow-wrap: break-word;
}

.ai-markdown :deep(p) {
  margin: 0.4em 0;
}

.ai-markdown :deep(pre) {
  background-color: #052e16 !important;
  border: none;
  border-radius: 8px;
  margin: 0.6em 0;
}

.ai-markdown :deep(pre code) {
  color: #bbf7d0;
  font-size: 0.85em;
}

.ai-markdown :deep(code) {
  background-color: rgba(34, 197, 94, 0.1);
  color: #15803d;
  padding: 0.15em 0.4em;
  border-radius: 4px;
  font-size: 0.9em;
}

.ai-markdown :deep(blockquote) {
  border-left-color: #22c55e;
  background-color: rgba(34, 197, 94, 0.04);
}
</style>
