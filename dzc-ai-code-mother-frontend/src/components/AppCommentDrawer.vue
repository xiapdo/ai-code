<template>
  <a-drawer
    v-model:open="drawerVisible"
    title="评论"
    placement="right"
    :width="400"
    @close="handleClose"
  >
    <div class="comment-drawer-content">
      <div class="comment-list-section">
        <div class="comment-count">
          共 {{ totalCommentCount }} 条评论
        </div>

        <a-spin :spinning="loading">
          <div v-if="allComments.length === 0 && !loading" class="empty-comments">
            <a-empty description="暂无评论，快来抢沙发~" />
          </div>

          <div v-else class="comment-list">
            <div v-for="comment in displayedComments" :key="comment.id" class="comment-item" :data-comment-id="comment.id">
              <div class="comment-main">
                <a-avatar :src="comment.user?.userAvatar || defaultAvatar" :size="36">
                  {{ comment.user?.userName?.charAt(0) || 'U' }}
                </a-avatar>
                <div class="comment-body">
                  <div class="comment-header">
                    <span class="comment-author">{{ comment.user?.userName || '匿名用户' }}</span>
                    <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  </div>
                  <div class="comment-content">{{ comment.content }}</div>
                  <div class="comment-actions">
                    <a-button type="text" size="small" @click.stop="handleReply(comment)">
                      <template #icon><MessageOutlined /></template>
                      回复
                    </a-button>
                  </div>

                  <div v-if="replyingTo?.reply?.id === comment.id && !replyingTo?.comment" class="reply-input-section">
                    <a-textarea
                      v-model:value="replyContent"
                      :placeholder="`回复 @${comment.user?.userName}...`"
                      :rows="2"
                      :auto-size="{ minRows: 2, maxRows: 4 }"
                      show-count
                      :maxlength="500"
                    />
                    <div class="reply-actions">
                      <a-button size="small" @click="cancelReply">取消</a-button>
                      <a-button type="primary" size="small" :loading="submitting" @click="handleSubmitReply(comment)">
                        回复
                      </a-button>
                    </div>
                  </div>

                  <div v-if="comment.replies && comment.replies.length > 0" class="comment-replies">
                    <div v-for="reply in comment.replies" :key="reply.id" class="reply-item" :data-comment-id="reply.id">
                      <a-avatar :src="reply.user?.userAvatar || defaultAvatar" :size="28">
                        {{ reply.user?.userName?.charAt(0) || 'U' }}
                      </a-avatar>
                      <div class="reply-body">
                        <div class="reply-header">
                          <span class="reply-author">{{ reply.user?.userName || '匿名用户' }}</span>
                          <span v-if="reply.replyToId" class="reply-target">回复 @{{ getReplyTargetUserName(comment, reply.replyToId) }}</span>
                          <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                        </div>
                        <div class="reply-content">{{ reply.content }}</div>
                        <div class="reply-actions">
                          <a-button type="text" size="small" @click.stop="handleReplyReply(comment, reply)">
                            <template #icon><MessageOutlined /></template>
                            回复
                          </a-button>
                        </div>

                        <div v-if="replyingTo?.reply?.id === reply.id && replyingTo?.comment" class="reply-input-section">
                          <a-textarea
                            v-model:value="replyContent"
                            :placeholder="`回复 @${reply.user?.userName}...`"
                            :rows="2"
                            :auto-size="{ minRows: 2, maxRows: 4 }"
                            show-count
                            :maxlength="500"
                          />
                          <div class="reply-actions">
                            <a-button size="small" @click="cancelReply">取消</a-button>
                            <a-button type="primary" size="small" :loading="submitting" @click="handleSubmitReply(comment)">
                              回复
                            </a-button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="allComments.length > displayedComments.length" class="load-more-section">
            <a-button type="link" block @click="loadMore" :loading="loadingMore">
              {{ loadingMore ? '加载中...' : '查看更多评论' }}
            </a-button>
          </div>
        </a-spin>
      </div>
    </div>

    <template #footer>
      <div class="comment-input-section">
        <a-textarea
          v-model:value="commentContent"
          placeholder="发表你的评论..."
          :rows="3"
          :auto-size="{ minRows: 3, maxRows: 6 }"
          show-count
          :maxlength="500"
          @press-enter="handleEnterComment"
        />
        <div class="comment-input-actions">
          <a-button type="primary" :loading="submitting" @click="handleSubmitComment" :disabled="!commentContent.trim()">
            发布评论
          </a-button>
        </div>
      </div>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import { addComment, getCommentList } from '@/api/interactionController'
import { formatTime } from '@/utils/time'
import { MessageOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { computed, ref, watch } from 'vue'

interface CommentWithReplies extends API.CommentVO {
  replies?: API.CommentVO[]
}

interface Props {
  open: boolean
  appId: number
  appName?: string
  highlightCommentId?: number
}

const props = defineProps<Props>()

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'comment-added'): void
}

const emit = defineEmits<Emits>()

const drawerVisible = computed({
  get: () => props.open,
  set: (value) => emit('update:open', value),
})

const commentContent = ref('')
const replyContent = ref('')
const replyingTo = ref<{ comment: API.CommentVO; reply?: API.CommentVO } | null>(null)
const submitting = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const commentList = ref<CommentWithReplies[]>([])
const defaultAvatar = new URL('@/assets/aiAvatar.png', import.meta.url).href
const hasHighlighted = ref(false)
const pageSize = ref(20)
const currentPage = ref(1)
const allComments = ref<CommentWithReplies[]>([])

const displayedComments = computed(() => {
  return allComments.value.slice(0, currentPage.value * pageSize.value)
})

// 计算总评论数（包括主评论和子回复）
const totalCommentCount = computed(() => {
  let count = allComments.value.length
  allComments.value.forEach(comment => {
    count += comment.replies?.length || 0
  })
  return count
})

watch(() => props.open, (newVal) => {
  if (newVal) {
    hasHighlighted.value = false
    loadComments()
  }
})

const loadComments = async () => {
  if (!props.appId) return
  loading.value = true
  currentPage.value = 1
  try {
    const res = await getCommentList({
      appId: props.appId,
      pageNum: 1,
      pageSize: 100,
    })
    console.log('评论加载响应:', res)
    if (res.data.code === 0 && res.data.data) {
      const allFetchedComments = res.data.data as CommentWithReplies[]
      console.log('所有评论:', allFetchedComments)
      commentList.value = allFetchedComments.filter(c => !c.parentId).map(comment => ({
        ...comment,
        replies: allFetchedComments
          .filter(r => r.parentId === comment.id)
          .sort((a, b) => new Date(a.createTime!).getTime() - new Date(b.createTime!).getTime())
      }))
      allComments.value = commentList.value
      console.log('处理后评论:', commentList.value)

      // 如果有高亮评论ID，加载完成后触发高亮闪烁（只执行一次）
      if (props.highlightCommentId && !hasHighlighted.value) {
        hasHighlighted.value = true
        const commentIdToHighlight = props.highlightCommentId
        setTimeout(() => {
          triggerHighlight(commentIdToHighlight)
        }, 300)
      }
    } else {
      console.error('评论加载失败:', res.data.message)
    }
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  currentPage.value++
}

const handleEnterComment = (e: KeyboardEvent) => {
  if (!e.shiftKey) {
    e.preventDefault()
    handleSubmitComment()
  }
}

const handleSubmitComment = async () => {
  if (!commentContent.value.trim()) return
  submitting.value = true
  try {
    const res = await addComment({
      appId: props.appId,
      content: commentContent.value.trim(),
    })
    if (res.data.code === 0) {
      message.success('评论发布成功')
      commentContent.value = ''
      await loadComments()
      emit('comment-added')
    } else {
      message.error('评论发布失败: ' + res.data.message)
    }
  } catch (error) {
    message.error('评论发布失败')
  } finally {
    submitting.value = false
  }
}

const handleReply = (comment: API.CommentVO) => {
  console.log('handleReply 被调用:', comment)
  replyingTo.value = { reply: comment }
  replyContent.value = ''
}

const handleReplyReply = (parentComment: API.CommentVO, reply: API.CommentVO) => {
  console.log('handleReplyReply 被调用:', parentComment, reply)
  replyingTo.value = { comment: parentComment, reply }
  replyContent.value = ''
}

const cancelReply = () => {
  replyingTo.value = null
  replyContent.value = ''
}

const handleSubmitReply = async (parentComment: API.CommentVO) => {
  if (!replyContent.value.trim()) return
  submitting.value = true
  try {
    const res = await addComment({
      appId: props.appId,
      content: replyContent.value.trim(),
      parentId: parentComment.id,
      replyToId: replyingTo.value?.reply?.id,
    })
    if (res.data.code === 0) {
      message.success('回复成功')
      replyContent.value = ''
      replyingTo.value = null
      await loadComments()
      emit('comment-added')
    } else {
      message.error('回复失败: ' + res.data.message)
    }
  } catch (error) {
    message.error('回复失败')
  } finally {
    submitting.value = false
  }
}

const getReplyTargetUserName = (parentComment: CommentWithReplies, replyToId: number): string => {
  if (parentComment.id === replyToId) {
    return parentComment.user?.userName || ''
  }
  const reply = parentComment.replies?.find(r => r.id === replyToId)
  return reply?.user?.userName || ''
}

const triggerHighlight = (commentId: number) => {
  const element = document.querySelector(`[data-comment-id="${commentId}"]`)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'center' })
    element.classList.add('highlight-flash')
    setTimeout(() => {
      element.classList.remove('highlight-flash')
    }, 2000)
  }
}

const handleClose = () => {
  commentContent.value = ''
  replyContent.value = ''
  replyingTo.value = null
}
</script>

<style scoped>
.comment-drawer-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.comment-input-section {
  padding-bottom: 16px;
}

.comment-input-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.comment-list-section {
  flex: 1;
  overflow-y: auto;
}

.comment-count {
  color: #666;
  font-size: 14px;
  margin-bottom: 16px;
}

.empty-comments {
  padding: 40px 0;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-main {
  display: flex;
  gap: 12px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-author {
  font-weight: 500;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  color: #444;
  line-height: 1.6;
  margin-bottom: 8px;
}

.comment-actions {
  display: flex;
  gap: 8px;
}

.reply-input-section {
  margin-top: 12px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.reply-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.comment-replies {
  margin-top: 12px;
  padding-left: 16px;
  border-left: 2px solid #e8e8e8;
}

.reply-item {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 2px;
}

.reply-author {
  font-weight: 500;
  font-size: 13px;
  color: #333;
}

.reply-target {
  font-size: 12px;
  color: #999;
}

.reply-time {
  font-size: 11px;
  color: #bbb;
}

.reply-content {
  font-size: 13px;
  color: #555;
  line-height: 1.5;
}

@keyframes highlight-flash-animation {
  0%, 100% {
    background-color: transparent;
  }
  10%, 30%, 50%, 70% {
    background-color: rgba(34, 197, 94, 0.3);
  }
  20%, 40%, 60% {
    background-color: rgba(34, 197, 94, 0.15);
  }
}

.load-more-section {
  margin-top: 16px;
  text-align: center;
}

.highlight-flash {
  animation: highlight-flash-animation 2s ease-in-out;
  border-radius: 8px;
  transition: background-color 0.3s ease;
}
</style>
