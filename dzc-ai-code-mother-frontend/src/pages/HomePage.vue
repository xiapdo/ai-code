<script setup lang="ts">
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import AppCard from '@/components/AppCard.vue'
import { getDeployUrl } from '@/config/env'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredLoading = ref(false)
const featuredHasMore = ref(true)
const featuredPage = reactive({
  current: 1,
  pageSize: 9,
  total: 0,
})
const sentinelRef = ref<HTMLElement | null>(null)
let sentinelObserver: IntersectionObserver | null = null

// 当前激活的标签页：'my' 我的作品, 'featured' 精选作品
const activeTab = ref<'my' | 'featured'>('my')
const previousTab = ref<'my' | 'featured'>('my')

// ==================== 搜索筛选相关状态 ====================

// 分类选项列表（与后端 AppConstant.CATEGORY_OPTIONS 保持一致）
const CATEGORY_OPTIONS = [
  { value: '', label: '全部分类' },
  { value: 'website', label: '网站' },
  { value: 'tool', label: '工具' },
  { value: 'dataAnalysis', label: '数据分析' },
  { value: 'eventPage', label: '活动页面' },
  { value: 'managementPlatform', label: '管理平台' },
  { value: 'userApp', label: '用户应用' },
  { value: 'personalManagement', label: '个人管理' },
]

// 排序选项列表（与后端 AppConstant.SORT_OPTIONS 保持一致）
const SORT_OPTIONS = [
  { value: 'default', label: '默认排序' },
  { value: 'createTime', label: '最新' },
  { value: 'priority', label: '最热' },
]

// 搜索关键词
const searchKeyword = ref('')

// 选中的分类
const selectedCategory = ref('')

// 选中的排序方式
const selectedSort = ref('default')

// 描述文字打字机效果
const descriptionText = ref('')
const fullDescription = '代码自动生成,使创意应用立现。'
const typewriterCursor = ref(true)
const typewriterTimer = ref<number | null>(null)

const startTypewriter = () => {
  if (typewriterTimer.value) {
    clearInterval(typewriterTimer.value)
    typewriterTimer.value = null
  }

  let charIndex = 0
  descriptionText.value = ''
  typewriterCursor.value = true

  typewriterTimer.value = window.setInterval(() => {
    if (charIndex < fullDescription.length) {
      descriptionText.value += fullDescription[charIndex]
      charIndex++
    } else {
      if (typewriterTimer.value) {
        clearInterval(typewriterTimer.value)
        typewriterTimer.value = null
      }
      setTimeout(() => {
        typewriterCursor.value = false
      }, 1000)
    }
  }, 120)
}

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
  setTimeout(() => {
    startTypewriter()
  }, 2000)
})

onUnmounted(() => {
  destroyFeaturedObserver()
  if (typewriterTimer.value) {
    clearInterval(typewriterTimer.value)
  }
})

// 切换标签页
const switchTab = (tab: 'my' | 'featured') => {
  previousTab.value = activeTab.value
  activeTab.value = tab
  if (tab === 'featured') {
    resetFeaturedState()
    nextTick(() => {
      loadFeaturedApps()
      setupFeaturedObserver()
    })
  } else {
    destroyFeaturedObserver()
  }
}

// 计算过渡方向：切换到精选作品从右往左，切换到我的作品从左往右
const slideDirection = computed(() => {
  if (activeTab.value === 'featured' && previousTab.value === 'my') {
    return 'right-to-left'
  }
  return 'left-to-right'
})

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 优化提示词功能已移除

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      // 跳转到对话页面，确保ID是字符串类型
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async (append = false) => {
  if (featuredLoading.value || (!append && !featuredHasMore.value)) {
    return
  }

  featuredLoading.value = true
  try {
    // 根据排序选项确定排序字段和顺序
    let sortField = 'createTime'
    let sortOrder = 'desc'
    if (selectedSort.value === 'priority') {
      sortField = 'priority'
      sortOrder = 'desc'
    } else if (selectedSort.value === 'createTime') {
      sortField = 'createTime'
      sortOrder = 'desc'
    }

    const res = await listGoodAppVoByPage({
      pageNum: featuredPage.current,
      pageSize: featuredPage.pageSize,
      sortField,
      sortOrder,
      // 搜索关键词（标题搜索）
      appName: searchKeyword.value || undefined,
    })

    if (res.data.code === 0 && res.data.data) {
      const newRecords = res.data.data.records || []
      if (append) {
        featuredApps.value = [...featuredApps.value, ...newRecords]
      } else {
        featuredApps.value = newRecords
      }
      featuredPage.total = Number(res.data.data.totalRow) || 0
      featuredHasMore.value = featuredApps.value.length < featuredPage.total
      if (featuredHasMore.value) {
        featuredPage.current++
      }
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  } finally {
    featuredLoading.value = false
  }
}

// 懒加载
let isLoadingMore = false

const loadMoreFeaturedApps = () => {
  if (isLoadingMore || !featuredHasMore.value || featuredLoading.value) {
    return
  }
  isLoadingMore = true
  loadFeaturedApps(true).finally(() => {
    isLoadingMore = false
  })
}

// 使用 IntersectionObserver 监听 sentinel 元素
const setupFeaturedObserver = () => {
  // 等待 DOM 更新后获取 sentinel 元素
  nextTick(() => {
    if (!sentinelRef.value) {
      console.log('sentinelRef is null, retrying in 100ms')
      setTimeout(() => setupFeaturedObserver(), 100)
      return
    }

    console.log('setting up IntersectionObserver')
    sentinelObserver = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          console.log('sentinel intersecting! loading more...')
          loadMoreFeaturedApps()
        }
      },
      { rootMargin: '200px' },
    )
    sentinelObserver.observe(sentinelRef.value)
    console.log('sentinel observer set up')
  })
}

const destroyFeaturedObserver = () => {
  if (sentinelObserver) {
    sentinelObserver.disconnect()
    sentinelObserver = null
  }
}

// 重置精选状态（同时重置筛选条件）
const resetFeaturedState = () => {
  featuredApps.value = []
  featuredPage.current = 1
  featuredHasMore.value = true
  // 重置筛选条件
  searchKeyword.value = ''
  selectedCategory.value = ''
  selectedSort.value = 'default'
  destroyFeaturedObserver()
}

// ==================== 搜索筛选处理函数 ====================

// 搜索处理函数
const onSearch = () => {
  // 重置页码并重新加载
  featuredPage.current = 1
  featuredApps.value = []
  featuredHasMore.value = true
  loadFeaturedApps()
}

// 分类选择处理函数
const onCategoryChange = (value: string) => {
  selectedCategory.value = value
  // 重置页码并重新加载
  featuredPage.current = 1
  featuredApps.value = []
  featuredHasMore.value = true
  loadFeaturedApps()
}

// 排序选择处理函数
const onSortChange = (value: string) => {
  selectedSort.value = value
  // 重置页码并重新加载
  featuredPage.current = 1
  featuredApps.value = []
  featuredHasMore.value = true
  loadFeaturedApps()
}

// 查看对话
const viewChat = (appId: string | number | undefined, showInteraction?: boolean) => {
  if (appId) {
    if (showInteraction) {
      router.push(`/app/chat/${appId}?view=1&showInteraction=true`)
    } else {
      router.push(`/app/chat/${appId}?view=1`)
    }
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// ==================== 页面加载与生命周期 ====================

// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
  setTimeout(() => {
    startTypewriter()
  }, 2000)
})

onUnmounted(() => {
  destroyFeaturedObserver()
  if (typewriterTimer.value) {
    clearInterval(typewriterTimer.value)
  }
})

watch(activeTab, (newTab) => {
  if (newTab === 'featured') {
    setupFeaturedObserver()
  } else {
    destroyFeaturedObserver()
  }
})
</script>

<template>
  <div id="homePage">
    <div class="container">
      <!-- 网站标题和描述 -->
      <div class="hero-section">
        <h1 class="hero-title entrance-animate" style="animation-delay: 0ms">想你所梦,构建无穷</h1>
        <p class="hero-description">
          <span class="typewriter-text">{{ descriptionText }}</span>
          <span v-if="typewriterCursor" class="typewriter-cursor">|</span>
        </p>
      </div>

      <!-- 用户提示词输入框 -->
      <div class="input-section entrance-animate" style="animation-delay: 200ms">
        <a-textarea
          v-model:value="userPrompt"
          placeholder="帮我创建个人博客网站"
          :rows="4"
          :maxlength="1000"
          class="prompt-input"
        />
        <div class="input-actions">
          <a-button type="primary" size="large" @click="createApp" :loading="creating">
            <template #icon>
              <span>↑</span>
            </template>
          </a-button>
        </div>
      </div>

      <!-- 快捷按钮 -->
      <div class="quick-actions entrance-animate" style="animation-delay: 300ms">
        <a-button
          type="default"
          @click="
            setPrompt(
              '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁的设计风格，支持响应式布局，文章支持Markdown格式，首页展示最新文章和热门推荐。',
            )
          "
          >个人博客网站</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面。采用商务风格的设计，包含轮播图、产品展示卡片、团队介绍、客户案例展示，支持多语言切换和在线客服功能。',
            )
          "
          >企业官网</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '构建一个功能完整的在线商城，包含商品展示、购物车、用户注册登录、订单管理、支付结算等功能。设计现代化的商品卡片布局，支持商品搜索筛选、用户评价、优惠券系统和会员积分功能。',
            )
          "
          >在线商城</a-button
        >
        <a-button
          type="default"
          @click="
            setPrompt(
              '制作一个精美的作品展示网站，适合设计师、摄影师、艺术家等创作者。包含作品画廊、项目详情页、个人简历、联系方式等模块。采用瀑布流或网格布局展示作品，支持图片放大预览和作品分类筛选。',
            )
          "
          >作品展示网站</a-button
        >
      </div>

      <!-- 作品切换标签 -->
      <div class="tab-switch entrance-animate" style="animation-delay: 400ms">
        <div class="tab-buttons">
          <button class="tab-btn" :class="{ active: activeTab === 'my' }" @click="switchTab('my')">
            📁 我的作品
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'featured' }"
            @click="switchTab('featured')"
          >
            ❤️ 精选作品
          </button>
        </div>
      </div>

      <!-- 搜索筛选栏（仅在精选作品标签页显示） -->
      <div
        v-if="activeTab === 'featured'"
        class="filter-bar sticky-bar"
      >
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索作品标题"
          class="filter-search"
          @search="onSearch"
          @pressEnter="onSearch"
        />
        <a-select v-model:value="selectedCategory" class="filter-select" @change="onCategoryChange">
          <a-select-option
            v-for="option in CATEGORY_OPTIONS"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-select-option>
        </a-select>
        <a-select v-model:value="selectedSort" class="filter-select" @change="onSortChange">
          <a-select-option v-for="option in SORT_OPTIONS" :key="option.value" :value="option.value">
            {{ option.label }}
          </a-select-option>
        </a-select>
      </div>

      <!-- 作品展示区域 -->
      <div class="section entrance-animate" style="animation-delay: 500ms">
        <transition :name="slideDirection" mode="out-in">
          <div v-if="activeTab === 'my'" key="my">
            <div class="app-grid" :class="{ 'grid-mode': myApps.length > 0 }">
              <template v-if="myApps.length === 0">
                <div class="empty-placeholder">
                  <div class="empty-message">快去创建案例吧！！！</div>
                </div>
              </template>
              <template v-else>
                <AppCard
                  v-for="app in myApps"
                  :key="app.id"
                  :app="app"
                  @view-chat="viewChat"
                  @view-work="viewWork"
                />
              </template>
            </div>
            <div class="pagination-wrapper">
              <a-pagination
                v-model:current="myAppsPage.current"
                v-model:page-size="myAppsPage.pageSize"
                :total="myAppsPage.total"
                :show-size-changer="false"
                :show-total="(total: number) => `共 ${total} 个应用`"
                @change="loadMyApps"
              />
            </div>
          </div>
          <div v-else key="featured">
            <div class="masonry-css">
              <div v-for="(app, index) in featuredApps" :key="app.id" class="masonry-css-item">
                <AppCard
                  :app="app"
                  :featured="true"
                  :show-interaction="true"
                  :class="['card-entrance', { 'card-visible': true }]"
                  :style="{ animationDelay: `${(index % 12) * 0.05}s` }"
                  @view-chat="viewChat"
                  @view-work="viewWork"
                />
              </div>
            </div>
            <div ref="sentinelRef" class="sentinel-item"></div>
            <div v-if="featuredLoading" class="loading-indicator">
              <a-spin size="large" />
            </div>
            <div v-if="!featuredHasMore && featuredApps.length > 0" class="end-indicator">
              <span>— 已加载全部内容 —</span>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  position: relative;
}

#homePage::before {
  content: '';
  position: fixed;
  inset: 0;
  background:
    radial-gradient(ellipse 600px 500px at 10% 20%, rgba(34, 197, 94, 0.25) 0%, transparent 60%),
    radial-gradient(ellipse 500px 400px at 90% 80%, rgba(5, 150, 105, 0.2) 0%, transparent 60%);
  animation: softFloat 18s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}

#homePage::after {
  content: '';
  position: fixed;
  inset: 0;
  background:
    radial-gradient(ellipse 450px 380px at 85% 15%, rgba(217, 119, 6, 0.18) 0%, transparent 60%),
    radial-gradient(ellipse 380px 320px at 15% 85%, rgba(34, 197, 94, 0.2) 0%, transparent 60%);
  animation: softFloat 22s ease-in-out infinite reverse;
  pointer-events: none;
  z-index: 0;
}

@keyframes softFloat {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
    opacity: 1;
  }
  25% {
    transform: translate(15px, -20px) scale(1.05);
    opacity: 1;
  }
  50% {
    transform: translate(-10px, 10px) scale(0.98);
    opacity: 0.9;
  }
  75% {
    transform: translate(20px, 15px) scale(1.03);
    opacity: 0.95;
  }
}

@media (prefers-reduced-motion: reduce) {
  #homePage::before,
  #homePage::after {
    animation: none;
  }

  .entrance-animate {
    animation: none !important;
    opacity: 1 !important;
    transform: none !important;
  }
}

@keyframes entranceFromBottom {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes entranceFromLeft {
  from {
    opacity: 0;
    transform: translateX(-40px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.entrance-animate {
  opacity: 0;
  animation: entranceFromBottom 800ms cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}

.hero-title.entrance-animate {
  animation: entranceFromLeft 1500ms cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
}

.typewriter-text {
  display: inline;
}

.typewriter-cursor {
  display: inline-block;
  color: #15803d;
  font-weight: 300;
  margin-left: 2px;
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

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0px 20px;
  position: relative;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
}

.hero-section {
  text-align: center;
  padding: 80px 0 60px;
  color: #1e293b;
  position: relative;
  overflow: hidden;
}

.hero-title {
  font-size: 56px;
  font-weight: 700;
  margin: 0 0 20px;
  line-height: 1.2;
  background: linear-gradient(135deg, #15803d 0%, #059669 40%, #d97706 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -1px;
  position: relative;
  z-index: 2;
  font-family: 'Lora', serif;
}

.hero-description {
  font-size: 20px;
  margin: 0;
  opacity: 0.8;
  color: #4b5563;
  position: relative;
  z-index: 2;
}

.input-section {
  position: relative;
  margin: 0 auto 24px;
  max-width: 800px;
}

.prompt-input {
  border-radius: 16px;
  border: 1px solid rgba(34, 197, 94, 0.2);
  font-size: 16px;
  padding: 20px 60px 20px 20px;
  background: rgba(255, 255, 255, 0.72);
}

.input-actions {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.quick-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 60px;
  flex-wrap: wrap;
}

.quick-actions .ant-btn {
  border-radius: 25px;
  padding: 8px 20px;
  height: auto;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(34, 197, 94, 0.2);
  color: #374151;
}

.quick-actions .ant-btn:hover {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(34, 197, 94, 0.4);
  color: var(--color-primary, #15803d);
}

.section-title {
  font-size: 32px;
  font-weight: 600;
  color: #1e293b;
  font-family: 'Lora', serif;
}

.app-grid {
  margin-bottom: 12px;
}

.app-grid.grid-mode {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.empty-placeholder {
  width: 100%;
  height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  border-radius: 8px;
  background: transparent;
}

.empty-message {
  font-size: 36px;
  font-weight: 800;
  text-align: center;
  color: #15803d;
  letter-spacing: 0.22em;
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

/* CSS Columns 瀑布流布局 */
.masonry-css {
  column-count: 3;
  column-gap: 24px;
}

.masonry-css-item {
  break-inside: avoid;
  margin-bottom: 24px;
}

@media (max-width: 1024px) {
  .masonry-css {
    column-count: 2;
  }
}

@media (max-width: 640px) {
  .masonry-css {
    column-count: 1;
  }
}

.sentinel-item {
  width: 100%;
  min-height: 80px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 24px;
}

.loading-indicator {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
}

.loading-indicator .ant-spin {
  color: #15803d;
}

.end-indicator {
  text-align: center;
  color: #6b7280;
  font-size: 14px;
  letter-spacing: 0.1em;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .hero-description {
    font-size: 16px;
  }

  .app-grid.grid-mode,
  .featured-grid {
    grid-template-columns: 1fr;
  }

  .quick-actions {
    justify-content: center;
  }
}

.tab-switch {
  display: flex;
  justify-content: center;
  margin-bottom: 40px;
}

.tab-buttons {
  display: flex;
  gap: 4px;
  padding: 6px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50px;
  border: 1px solid rgba(34, 197, 94, 0.15);
}

.tab-btn {
  padding: 12px 32px;
  font-size: 15px;
  font-weight: 500;
  border: none;
  border-radius: 50px;
  cursor: pointer;
  background: transparent;
  color: #6b7280;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.tab-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #15803d 0%, #059669 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: -1;
}

.tab-btn:hover {
  color: #1f2937;
}

.tab-btn:hover::before {
  opacity: 0.1;
}

.tab-btn.active {
  background: linear-gradient(135deg, #15803d 0%, #059669 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(21, 128, 61, 0.3);
  transform: translateY(-1px);
}

/* 搜索筛选栏样式 */
.filter-bar {
  display: flex;
  gap: 42px;
  justify-content: center;
  align-items: center;
  margin-bottom: 32px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  border: 1px solid rgba(34, 197, 94, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  flex-wrap: wrap;
  transition: box-shadow 0.3s ease;
}

/* 吸顶效果 */
.sticky-bar {
  position: sticky;
  top: 88px;
  z-index: 999;
}

.sticky-bar.is-sticky {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 4px 20px rgba(21, 128, 61, 0.12);
  border-color: rgba(34, 197, 94, 0.2);
  border-radius: 0 0 12px 12px;
}

.filter-search {
  flex: 1;
  min-width: 240px;
  max-width: 400px;
}

.filter-search :deep(.ant-input) {
  border-radius: 8px;
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.filter-search :deep(.ant-input-group) {
  display: flex;
  gap: 4px;
  height: 40px;
}

.filter-search :deep(.ant-input) {
  border-radius: 8px;
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.filter-search :deep(.ant-input-search-button) {
  border-radius: 8px;
  background: linear-gradient(135deg, #15803d 0%, #059669 100%);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-search :deep(.ant-input-group-addon) {
  border: none;
  background: transparent;
  margin-top: 4px;
}

.filter-select {
  min-width: 140px;
}

.filter-select :deep(.ant-select-selector) {
  border: 1px solid rgba(34, 197, 94, 0.2);
}

.filter-select :deep(.ant-select-focused .ant-select-selector) {
  border-color: #15803d;
  box-shadow: 0 0 0 2px rgba(21, 128, 61, 0.1);
}

@media (max-width: 768px) {
  .filter-bar {
    flex-direction: column;
    gap: 12px;
    padding: 16px;
  }

  .filter-search,
  .filter-select {
    width: 100%;
    max-width: none;
    min-width: auto;
  }
}

.right-to-left-enter-active,
.right-to-left-leave-active {
  transition: all 0.5s ease;
}

.right-to-left-enter-from {
  opacity: 0;
  transform: translateX(60px);
}

.right-to-left-leave-to {
  opacity: 0;
  transform: translateX(-60px);
}

.left-to-right-enter-active,
.left-to-right-leave-active {
  transition: all 0.5s ease;
}

.left-to-right-enter-from {
  opacity: 0;
  transform: translateX(-60px);
}

.left-to-right-leave-to {
  opacity: 0;
  transform: translateX(60px);
}
</style>
