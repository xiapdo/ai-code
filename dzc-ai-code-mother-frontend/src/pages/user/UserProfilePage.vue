<template>
  <div class="user-profile-container">
    <a-row :gutter="24">
      <!-- 左侧用户基本信息卡片 -->
      <a-col :span="8">
        <a-card class="user-info-card" :bordered="false">
          <div class="avatar-section">
            <a-avatar :src="loginUserStore.loginUser.userAvatar || defaultAvatar" :size="120" />
          </div>
          <div class="user-name-section">
            {{ loginUserStore.loginUser.userName || '未填写' }}
          </div>
          <div class="role-tag">
            <a-tag :color="loginUserStore.loginUser.userRole === 'admin' ? 'gold' : 'green'">
              {{ loginUserStore.loginUser.userRole === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </div>
          <div class="info-item">
            <span class="label">注册日期：</span>
            <span class="value">{{ formatDate(loginUserStore.loginUser.createTime) }}</span>
          </div>
          <div class="info-item id-item">
            <span class="label">我的 id：</span>
            <span class="value">{{ loginUserStore.loginUser.id }}</span>
            <CopyOutlined class="copy-icon" @click="copyUserId" />
          </div>
          <div class="coins-info">
            <div class="coins-item">
              <span class="coins-icon">🪙</span>
              <span class="coins-value">{{ loginUserStore.loginUser.coins || 0 }}</span>
              <span class="coins-label">金币</span>
            </div>
            <div class="coins-divider"></div>
            <div class="quota-item">
              <span class="quota-icon">🎯</span>
              <span class="quota-value">{{ loginUserStore.loginUser.chatRoomQuota || 0 }}</span>
              <span class="quota-label">群聊名额</span>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 右侧详情区域 -->
      <a-col :span="16">
        <a-card class="detail-card" :bordered="false">
          <!-- 标签切换 -->
          <div class="tab-header">
            <div class="tab-buttons">
              <div
                class="tab-button"
                :class="{ active: activeTab === 'info' }"
                @click="switchTab('info')"
              >
                我的信息
              </div>
              <div
                class="tab-button"
                :class="{ active: activeTab === 'edit' }"
                @click="switchTab('edit')"
              >
                修改信息
              </div>
              <div
                class="tab-button"
                :class="{ active: activeTab === 'collect' }"
                @click="switchTab('collect')"
              >
                我的收藏
              </div>
              <div
                class="tab-button"
                :class="{ active: activeTab === 'like' }"
                @click="switchTab('like')"
              >
                我的喜欢
              </div>
              <div
                class="tab-button"
                :class="{ active: activeTab === 'checkin' }"
                @click="switchTab('checkin')"
              >
                每日签到
              </div>
              <div
                class="tab-button"
                :class="{ active: activeTab === 'works' }"
                @click="switchTab('works')"
              >
                我的作品
              </div>
            </div>
          </div>

          <!-- 内容区域 -->
          <transition :name="transitionName" mode="out-in">
            <!-- 查看模式 -->
            <div v-if="activeTab === 'info'" key="info" class="info-list">
              <div class="info-row">
                <span class="field-label">用户昵称：</span>
                <span class="field-value">{{ loginUserStore.loginUser.userName || '未填写' }}</span>
              </div>
              <div class="info-row">
                <span class="field-label">用户简介：</span>
                <span class="field-value">{{ loginUserStore.loginUser.userProfile || '未填写' }}</span>
              </div>
              <div class="info-row">
                <span class="field-label">手机号：</span>
                <span class="field-value">{{ loginUserStore.loginUser.phone || '未填写' }}</span>
              </div>
              <div class="info-row">
                <span class="field-label">邮箱：</span>
                <span class="field-value">{{ loginUserStore.loginUser.email || '未填写' }}</span>
              </div>
            </div>

            <!-- 修改模式 -->
            <div v-else-if="activeTab === 'edit'" key="edit" class="edit-form">
              <a-form
                ref="formRef"
                :model="editForm"
                :rules="rules"
                layout="vertical"
              >
                <a-form-item label="用户头像" name="userAvatar">
                  <a-upload
                    :custom-request="handleUpload"
                    :show-upload-list="false"
                    :before-upload="beforeUpload"
                    accept="image/*"
                    list-type="picture-card"
                  >
                    <div v-if="editForm.userAvatar">
                      <img :src="editForm.userAvatar" alt="头像" style="width: 100px; height: 100px; object-fit: cover; border-radius: 8px;" />
                    </div>
                    <div v-else>
                      <PlusOutlined />
                      <div class="ant-upload-text">上传头像</div>
                    </div>
                  </a-upload>
                </a-form-item>
                <a-form-item label="用户昵称" name="userName">
                  <a-input v-model:value="editForm.userName" placeholder="请输入用户昵称" />
                </a-form-item>
                <a-form-item label="用户简介" name="userProfile">
                  <a-textarea v-model:value="editForm.userProfile" placeholder="请输入用户简介" :rows="3" :maxlength="200" show-count />
                </a-form-item>
                <a-form-item label="手机号" name="phone">
                  <a-input v-model:value="editForm.phone" placeholder="请输入手机号" />
                </a-form-item>
                <a-form-item label="邮箱" name="email">
                  <a-input v-model:value="editForm.email" placeholder="请输入邮箱" />
                </a-form-item>
                <a-form-item>
                  <a-space>
                    <a-button type="primary" @click="handleSave" :loading="saving">保存</a-button>
                    <a-button @click="switchTab('info')">取消</a-button>
                  </a-space>
                </a-form-item>
              </a-form>
            </div>

            <!-- 我的收藏 -->
            <div v-else-if="activeTab === 'collect'" key="collect" class="app-list-tab">
              <a-spin :spinning="loadingCollect">
                <a-empty v-if="collectedApps.length === 0 && !loadingCollect" description="还没有收藏任何作品" />
                <div v-else class="app-grid">
                  <AppCard
                    v-for="app in collectedApps"
                    :key="app.id"
                    :app="app"
                    @click="handleAppClick"
                    @view-chat="viewChat"
                    @view-work="viewWork"
                  />
                </div>
              </a-spin>
            </div>

            <!-- 我的喜欢 -->
            <div v-else-if="activeTab === 'like'" key="like" class="app-list-tab">
              <a-spin :spinning="loadingLike">
                <a-empty v-if="likedApps.length === 0 && !loadingLike" description="还没有喜欢任何作品" />
                <div v-else class="app-grid">
                  <AppCard
                    v-for="app in likedApps"
                    :key="app.id"
                    :app="app"
                    @click="handleAppClick"
                    @view-chat="viewChat"
                    @view-work="viewWork"
                  />
                </div>
              </a-spin>
            </div>

            <!-- 每日签到 -->
            <div v-else-if="activeTab === 'checkin'" key="checkin" class="checkin-tab">
              <a-spin :spinning="checkinLoading">
                <div class="checkin-container">
                  <!-- 签到按钮 -->
                  <div class="checkin-button-wrapper">
                    <a-button
                      type="primary"
                      size="large"
                      :disabled="checkinStatus?.checkedToday"
                      @click="handleCheckin"
                      class="checkin-button"
                      :class="{ 'checked-today': checkinStatus?.checkedToday }"
                    >
                      <span v-if="checkinStatus?.checkedToday">
                        <CheckCircleOutlined /> 今日已签到
                      </span>
                      <span v-else>
                        <GiftOutlined /> 立即签到
                      </span>
                    </a-button>
                  </div>

                  <!-- 签到统计 -->
                  <div class="checkin-stats">
                    <div class="stat-item">
                      <div class="stat-value">{{ checkinStatus?.continuousDays || 0 }}</div>
                      <div class="stat-label">连续签到天数</div>
                    </div>
                    <div class="stat-divider"></div>
                    <div class="stat-item">
                      <div class="stat-value">{{ checkinStatus?.totalDays || 0 }}</div>
                      <div class="stat-label">累计签到天数</div>
                    </div>
                  </div>

                  <!-- 金币进度条 -->
                  <div class="coin-progress-section">
                    <div class="progress-header">
                      <span class="progress-title">💰 金币兑换群聊名额</span>
                      <span class="progress-ratio">{{ checkinStatus?.coinReward?.totalCoins || loginUserStore.loginUser.coins || 0 }}/7</span>
                    </div>
                    <div class="progress-bar-wrapper">
                      <div class="progress-bar-bg">
                        <div
                          class="progress-bar-fill"
                          :style="{ width: `${Math.min(((checkinStatus?.coinReward?.totalCoins || loginUserStore.loginUser.coins || 0) / 7) * 100, 100)}%` }"
                        ></div>
                      </div>
                      <div class="progress-tip">
                        {{ (checkinStatus?.coinReward?.totalCoins || loginUserStore.loginUser.coins || 0) >= 7 ? '已可兑换！' : `还差 ${7 - (checkinStatus?.coinReward?.totalCoins || loginUserStore.loginUser.coins || 0)} 枚金币即可兑换` }}
                      </div>
                    </div>
                  </div>

                  <!-- 最近 7 天签到记录 -->
                  <div class="checkin-records">
                    <div class="records-title">最近 7 天签到记录</div>
                    <div class="records-grid">
                      <div
                        v-for="record in checkinStatus?.recentRecords"
                        :key="record.date"
                        class="record-item"
                        :class="{ 'checked': record.checked }"
                      >
                        <div class="record-day">{{ record.dayOfWeek }}</div>
                        <div class="record-date">{{ formatShortDate(record.date) }}</div>
                        <div class="record-status">
                          <CheckCircleOutlined v-if="record.checked" class="checked-icon" />
                          <CloseCircleOutlined v-else class="unchecked-icon" />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </a-spin>
            </div>

            <!-- 我的作品 -->
            <div v-else-if="activeTab === 'works'" key="works" class="app-list-tab">
              <a-spin :spinning="loadingWorks">
                <a-empty v-if="myApps.length === 0 && !loadingWorks" description="还没有创建任何作品" />
                <div v-else class="app-grid">
                  <AppCard
                    v-for="app in myApps"
                    :key="app.id"
                    :app="app"
                    :isOwner="true"
                    @click="handleAppClick"
                    @view-chat="viewChat"
                    @view-work="viewWork"
                    @view-stat="handleViewStat"
                  />
                </div>
                <div v-if="myApps.length > 0" class="pagination-wrapper">
                  <a-pagination
                    v-model:current="worksPageNum"
                    :total="worksTotal"
                    :page-size="8"
                    show-size-changer
                    @change="loadMyApps"
                    @show-size-change="loadMyApps"
                  />
                </div>
              </a-spin>
            </div>
          </transition>
        </a-card>
      </a-col>
    </a-row>

    <!-- 金币弹跳动画 -->
    <Teleport to="body">
      <Transition name="coin-popup">
        <div v-if="showCoinAnimation" class="coin-animation-overlay" @click="showCoinAnimation = false">
          <div class="coin-main-wrapper">
            <!-- 中央大金币（闪闪发光 + 旋转） -->
            <div class="coin-main">
              <div class="coin-glow"></div>
              <div class="coin-spin">🪙</div>
            </div>
            <!-- 周围小金币 -->
            <div v-for="i in 6" :key="i" class="coin-small" :style="{ animationDelay: `${i * 0.08}s` }">
              🪙
            </div>
          </div>
          <div class="coin-text" v-if="coinRewardText">
            {{ coinRewardText }}
          </div>
          <div class="coin-hint">点击任意处关闭</div>
        </div>
      </Transition>
    </Teleport>

    <!-- 金币音效 -->
    <audio ref="coinSoundRef" :src="coinSoundUrl" preload="auto"></audio>
  </div>
</template>

<script setup lang="ts">
import { listMyAppVoByPage } from '@/api/appController'
import { getUserCollectedApps, getUserLikedApps } from '@/api/interactionController'
import { editUser, getCheckinStatus, uploadAvatar, userCheckin } from '@/api/userController'
import defaultAvatar from '@/assets/aiAvatar.png'
import AppCard from '@/components/AppCard.vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { CheckCircleOutlined, CloseCircleOutlined, CopyOutlined, GiftOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { message, type FormInstance } from 'ant-design-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 标签页状态
const activeTab = ref('info')
const prevTab = ref('info')
const saving = ref(false)
const formRef = ref<FormInstance>()

// 收藏和喜欢数据
const collectedApps = ref<API.AppVO[]>([])
const likedApps = ref<API.AppVO[]>([])
const loadingCollect = ref(false)
const loadingLike = ref(false)

// 签到相关数据
const checkinStatus = ref<API.CheckinStatusVO>()
const checkinLoading = ref(false)

// 作品相关数据
const myApps = ref<API.AppVO[]>([])
const worksPageNum = ref(1)
const worksTotal = ref(0)
const loadingWorks = ref(false)

// 金币动画相关数据
const showCoinAnimation = ref(false)
const coinRewardText = ref('')

const editForm = reactive({
  userAvatar: '',
  userName: '',
  userProfile: '',
  phone: '',
  email: '',
})

const transitionName = computed(() => {
  return prevTab.value === 'info' || prevTab.value === 'edit'
    ? (activeTab.value === 'collect' || activeTab.value === 'like' ? 'fade' : 'slide-left')
    : (activeTab.value === 'info' || activeTab.value === 'edit' ? 'slide-right' : 'fade')
})

// 切换标签页
const switchTab = (tab: string) => {
  prevTab.value = activeTab.value
  activeTab.value = tab

  // 切换到收藏时加载数据
  if (tab === 'collect') {
    loadCollectedApps()
  }
  // 切换到喜欢时加载数据
  if (tab === 'like') {
    loadLikedApps()
  }
  // 切换到作品时加载数据
  if (tab === 'works') {
    loadMyApps()
  }
  // 切换到签到时加载数据
  if (tab === 'checkin') {
    loadCheckinStatus()
  }
}

// 加载收藏列表
const loadCollectedApps = async () => {
  loadingCollect.value = true
  try {
    const res = await getUserCollectedApps()
    if (res.data.code === 0 && res.data.data) {
      collectedApps.value = res.data.data
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    message.error('加载收藏列表失败')
  } finally {
    loadingCollect.value = false
  }
}

// 加载喜欢列表
const loadLikedApps = async () => {
  loadingLike.value = true
  try {
    const res = await getUserLikedApps()
    if (res.data.code === 0 && res.data.data) {
      likedApps.value = res.data.data
    }
  } catch (error) {
    console.error('加载喜欢列表失败:', error)
    message.error('加载喜欢列表失败')
  } finally {
    loadingLike.value = false
  }
}

// 加载我的作品列表
const loadMyApps = async () => {
  loadingWorks.value = true
  try {
    const res = await listMyAppVoByPage({
      current: worksPageNum.value,
      pageSize: 8,
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      worksTotal.value = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载作品列表失败:', error)
    message.error('加载作品列表失败')
  } finally {
    loadingWorks.value = false
  }
}

/**
 * 跳转到浏览统计页面
 */
const handleViewStat = (appId: number) => {
  router.push(`/app/viewstat/${appId}`)
}

// 加载签到状态
const loadCheckinStatus = async () => {
  try {
    const res = await getCheckinStatus()
    if (res.data.code === 0 && res.data.data) {
      checkinStatus.value = res.data.data
      // 同时更新用户信息中的金币和名额（保持数据同步）
      if (loginUserStore.loginUser && res.data.data.coinReward) {
        loginUserStore.loginUser.coins = res.data.data.coinReward.totalCoins
      }
    }
  } catch (error) {
    console.error('加载签到状态失败:', error)
  }
}

// 金币音效（使用 Web Audio API 生成金币旋转的叮当声）
const playCoinSound = () => {
  try {
    const AudioContext = window.AudioContext || (window as any).webkitAudioContext
    if (!AudioContext) return
    
    const audioContext = new AudioContext()
    
    // 创建多个振荡器模拟金币叮当声
    const frequencies = [2200, 2600, 3000, 2400, 2800]
    const delays = [0, 80, 160, 240, 320]
    
    frequencies.forEach((freq, index) => {
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()
      
      // 连接音频节点
      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)
      
      // 设置音调
      oscillator.type = 'sine'
      oscillator.frequency.setValueAtTime(freq, audioContext.currentTime)
      
      // 快速频率衰减模拟旋转声
      oscillator.frequency.exponentialRampToValueAtTime(freq * 0.7, audioContext.currentTime + 0.2)
      
      // 设置音量包络（增大音量）
      gainNode.gain.setValueAtTime(0.4, audioContext.currentTime + delays[index] / 1000)
      gainNode.gain.exponentialRampToValueAtTime(0.001, audioContext.currentTime + delays[index] / 1000 + 0.4)
      
      // 开始播放
      oscillator.start(audioContext.currentTime + delays[index] / 1000)
      oscillator.stop(audioContext.currentTime + delays[index] / 1000 + 0.4)
    })
  } catch (error) {
    console.error('播放金币音效失败:', error)
  }
}

// 执行签到
const handleCheckin = async () => {
  checkinLoading.value = true
  try {
    const res = await userCheckin()
    if (res.data.code === 0 && res.data.data) {
      checkinStatus.value = res.data.data
      message.success('签到成功！')
      
      // 显示金币动画
      const coinReward = res.data.data.coinReward
      if (coinReward) {
        coinRewardText.value = `+${coinReward.earnedCoins || 1} 金币`
        showCoinAnimation.value = true
        // 播放金币音效
        playCoinSound()
        // 4 秒后自动隐藏动画
        setTimeout(() => {
          showCoinAnimation.value = false
          coinRewardText.value = ''
        }, 4000)
      }
      
      // 更新用户信息中的金币数
      if (loginUserStore.loginUser) {
        loginUserStore.loginUser.coins = coinReward?.totalCoins || loginUserStore.loginUser.coins
        if (coinReward?.needExchange) {
          loginUserStore.loginUser.chatRoomQuota = (loginUserStore.loginUser.chatRoomQuota || 0) + (coinReward.exchangedQuota || 0)
          loginUserStore.loginUser.coins = coinReward.remainingCoins
          setTimeout(() => {
            message.success('金币已满，自动兑换为群聊名额！')
          }, 1000)
        }
      }
    } else {
      message.error(res.data.message || '签到失败')
    }
  } catch (error: any) {
    if (error?.response?.data?.message) {
      message.error(error.response.data.message)
    } else {
      message.error('签到失败，请重试')
    }
  } finally {
    checkinLoading.value = false
  }
}

// 点击应用卡片跳转到 AI 对话页面
const viewChat = (appId: string | number | undefined, showInteraction?: boolean) => {
  if (appId) {
    if (showInteraction) {
      router.push(`/app/chat/${appId}?view=1&showInteraction=true`)
    } else {
      router.push(`/app/chat/${appId}?view=1`)
    }
  }
}

// 点击应用卡片
const handleAppClick = (app: API.AppVO) => {
  viewChat(app.id)
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  router.push(`/app/work/${app.id}`)
}

const switchToEdit = () => {
  prevTab.value = activeTab.value
  editForm.userAvatar = loginUserStore.loginUser.userAvatar || ''
  editForm.userName = loginUserStore.loginUser.userName || ''
  editForm.userProfile = loginUserStore.loginUser.userProfile || ''
  editForm.phone = loginUserStore.loginUser.phone || ''
  editForm.email = loginUserStore.loginUser.email || ''
  activeTab.value = 'edit'
}

const validatePhone = (_rule: Rule, value: string) => {
  if (value && value.trim() !== '') {
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(value)) {
      return Promise.reject('请输入正确的手机号（11位，以1开头）')
    }
  }
  return Promise.resolve()
}

const validateEmail = (_rule: Rule, value: string) => {
  if (value && value.trim() !== '') {
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
    if (!emailRegex.test(value)) {
      return Promise.reject('请输入正确的邮箱格式')
    }
  }
  return Promise.resolve()
}

const rules = reactive({
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }],
})

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '未知'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 格式化短日期（MM/dd）
const formatShortDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}/${day}`
}

const copyUserId = async () => {
  if (loginUserStore.loginUser.id) {
    try {
      await navigator.clipboard.writeText(String(loginUserStore.loginUser.id))
      message.success('ID已复制到剪贴板')
    } catch (error) {
      message.error('复制失败')
    }
  }
}

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    message.error('只能上传图片文件！')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB！')
    return false
  }
  return true
}

const handleUpload = async ({ file, onSuccess, onError }: any) => {
  try {
    const res = await uploadAvatar(file)
    if (res.data.code === 0 && res.data.data) {
      editForm.userAvatar = res.data.data
      message.success('头像上传成功')
      onSuccess?.(res.data.data)
    } else {
      message.error(res.data.message || '上传失败')
      onError?.(new Error(res.data.message))
    }
  } catch (error) {
    message.error('上传失败，请重试')
    onError?.(error as Error)
  }
}

const handleSave = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const res = await editUser({
      userName: editForm.userName || undefined,
      userAvatar: editForm.userAvatar || undefined,
      userProfile: editForm.userProfile || undefined,
      phone: editForm.phone || undefined,
      email: editForm.email || undefined,
    })

    if (res.data.code === 0) {
      message.success('信息更新成功')
      activeTab.value = 'info'
      await loginUserStore.fetchLoginUser()
    } else {
      message.error(res.data.message || '更新失败')
    }
  } catch (error) {
    message.error('更新失败，请重试')
  } finally {
    saving.value = false
  }
}

const switchToInfo = () => {
  prevTab.value = activeTab.value
  activeTab.value = 'info'
}

const cancelEdit = () => {
  prevTab.value = activeTab.value
  activeTab.value = 'info'
}

// 页面加载时获取用户信息
onMounted(() => {
  loginUserStore.fetchLoginUser()
})
</script>

<style scoped>
.user-profile-container {
  max-width: 1200px;
  margin: 24px auto;
  padding: 0 16px;
  min-height: calc(100vh - 150px);
}

.user-info-card {
  text-align: center;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.08);
  height: fit-content;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.avatar-section {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

.user-name-section {
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 12px;
  font-family: 'Lora', serif;
}

.role-tag {
  margin-bottom: 24px;
}

.info-item {
  padding: 12px 0;
  border-bottom: 1px solid rgba(34, 197, 94, 0.1);
  font-size: 14px;
  color: #1f2937;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #4b5563;
  font-weight: 500;
}

.info-item .value {
  color: #1f2937;
  margin-left: 4px;
}

.id-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.copy-icon {
  cursor: pointer;
  color: var(--color-primary, #15803d);
  font-size: 14px;
  transition: color 0.3s;
}

.copy-icon:hover {
  color: var(--color-primary-light, #22c55e);
}

.detail-card {
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.08);
  min-height: 500px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.35);
}

.tab-header {
  margin-bottom: 24px;
}

.tab-buttons {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid rgba(34, 197, 94, 0.15);
  padding-bottom: 0;
}

.tab-button {
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  color: #4b5563;
  cursor: pointer;
  position: relative;
  transition: all 0.3s ease;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
}

.tab-button:hover {
  color: var(--color-primary, #15803d);
}

.tab-button.active {
  color: var(--color-primary, #15803d);
  border-bottom-color: var(--color-primary, #15803d);
}

.info-list {
  padding: 16px 0;
}

.info-row {
  display: flex;
  align-items: flex-start;
  padding: 16px 0;
  border-bottom: 1px solid rgba(34, 197, 94, 0.08);
  font-size: 15px;
}

.info-row:last-child {
  border-bottom: none;
}

.field-label {
  color: #4b5563;
  font-weight: 500;
  min-width: 100px;
  flex-shrink: 0;
}

.field-value {
  color: #1f2937;
  flex: 1;
}

.edit-form {
  padding: 16px 0;
  max-width: 600px;
}

/* 应用列表样式 */
.app-list-tab {
  padding: 16px 0;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

:deep(.ant-upload-select-picture-card) {
  width: 100px;
  height: 100px;
}

.fade-enter-active,
.fade-leave-active,
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 签到页样式 */
.checkin-tab {
  padding: 16px 0;
}

.checkin-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
}

.checkin-button-wrapper {
  text-align: center;
  animation: slideInDown 0.6s ease;
}

@keyframes slideInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.checkin-button {
  min-width: 200px;
  height: 56px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 28px;
  background: linear-gradient(135deg, #15803d 0%, #22c55e 100%);
  border: none;
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.checkin-button::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.checkin-button:active::before {
  width: 300px;
  height: 300px;
}

.checkin-button:hover:not(:disabled) {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 8px 28px rgba(21, 128, 61, 0.45);
}

.checkin-button.checked-today {
  background: linear-gradient(135deg, #6b7280 0%, #9ca3af 100%);
  cursor: not-allowed;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 4px 16px rgba(107, 114, 128, 0.3);
  }
  50% {
    box-shadow: 0 4px 24px rgba(107, 114, 128, 0.5);
  }
}

.checkin-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  padding: 24px 40px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.08);
  width: 100%;
  max-width: 500px;
  animation: slideInUp 0.6s ease 0.1s both;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #15803d;
  font-family: 'Lora', serif;
  animation: countUp 0.8s ease;
}

@keyframes countUp {
  from {
    opacity: 0;
    transform: scale(0.5);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 60px;
  background: rgba(34, 197, 94, 0.2);
}

.checkin-records {
  width: 100%;
  max-width: 500px;
  animation: fadeIn 0.6s ease 0.2s both;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.records-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 16px;
  text-align: center;
}

.records-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
}

.record-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 4px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.5s ease forwards;
  opacity: 0;
  transform: translateY(10px);
}

/* 依次延迟动画 */
.record-item:nth-child(1) { animation-delay: 0.05s; }
.record-item:nth-child(2) { animation-delay: 0.1s; }
.record-item:nth-child(3) { animation-delay: 0.15s; }
.record-item:nth-child(4) { animation-delay: 0.2s; }
.record-item:nth-child(5) { animation-delay: 0.25s; }
.record-item:nth-child(6) { animation-delay: 0.3s; }
.record-item:nth-child(7) { animation-delay: 0.35s; }

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.record-item:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow: 0 8px 24px rgba(21, 128, 61, 0.15);
}

.record-item.checked {
  background: rgba(34, 197, 94, 0.15);
  border: 2px solid rgba(34, 197, 94, 0.5);
  box-shadow: 0 4px 16px rgba(34, 197, 94, 0.2);
}

.record-item.checked:hover {
  box-shadow: 0 8px 24px rgba(34, 197, 94, 0.3);
}

.record-item:not(.checked) {
  background: rgba(107, 114, 128, 0.05);
  border: 1px solid rgba(107, 114, 128, 0.2);
}

.record-day {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
  margin-bottom: 2px;
}

.record-date {
  font-size: 11px;
  color: #9ca3af;
  margin-bottom: 6px;
}

.record-status {
  font-size: 24px;
  transition: transform 0.3s ease;
}

.record-item:hover .record-status {
  transform: scale(1.1);
}

.checked-icon {
  color: #22c55e;
  animation: checkPop 0.4s ease;
}

@keyframes checkPop {
  0% {
    transform: scale(0);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.unchecked-icon {
  color: #d1d5db;
}

/* 金币和名额展示样式 */
.coins-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 16px;
  margin-top: 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(21, 128, 61, 0.08);
}

.coins-item, .quota-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.coins-icon, .quota-icon {
  font-size: 24px;
}

.coins-value, .quota-value {
  font-size: 20px;
  font-weight: 700;
  color: #15803d;
  font-family: 'Lora', serif;
}

.coins-label, .quota-label {
  font-size: 12px;
  color: #6b7280;
}

.coins-divider {
  width: 1px;
  height: 30px;
  background: rgba(34, 197, 94, 0.2);
}

/* 金币进度条样式 */
.coin-progress-section {
  width: 100%;
  max-width: 500px;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.08);
  animation: slideInUp 0.6s ease 0.15s both;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.progress-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.progress-ratio {
  font-size: 14px;
  font-weight: 700;
  color: #15803d;
}

.progress-bar-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-bar-bg {
  width: 100%;
  height: 12px;
  background: rgba(21, 128, 61, 0.1);
  border-radius: 6px;
  overflow: hidden;
  position: relative;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #15803d 0%, #22c55e 100%);
  border-radius: 6px;
  transition: width 0.5s ease;
  position: relative;
}

.progress-bar-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.progress-tip {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
}

/* 金币弹跳动画样式 */
.coin-animation-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 9999;
}

.coin-popup-enter-active {
  animation: coinFadeIn 0.3s ease;
}

.coin-popup-leave-active {
  animation: coinFadeOut 0.3s ease;
}

@keyframes coinFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes coinFadeOut {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

.coin-main-wrapper {
  position: relative;
  width: 280px;
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 中央大金币容器 */
.coin-main {
  position: relative;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

/* 金币发光背景 */
.coin-glow {
  position: absolute;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(251, 191, 36, 0.6) 0%, rgba(251, 191, 36, 0.2) 40%, transparent 70%);
  animation: glowPulse 1.5s ease-in-out infinite;
}

@keyframes glowPulse {
  0%, 100% {
    transform: scale(0.8);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.2);
    opacity: 1;
  }
}

/* 旋转金币 */
.coin-spin {
  font-size: 90px;
  animation: coinSpin 1.5s ease-in-out infinite;
  filter: drop-shadow(0 0 20px rgba(251, 191, 36, 0.8))
          drop-shadow(0 0 40px rgba(251, 191, 36, 0.4));
  position: relative;
  z-index: 1;
}

@keyframes coinSpin {
  0% {
    transform: scale(1) rotateY(0deg);
  }
  25% {
    transform: scale(0.6) rotateY(180deg);
  }
  50% {
    transform: scale(1) rotateY(360deg);
  }
  75% {
    transform: scale(0.6) rotateY(540deg);
  }
  100% {
    transform: scale(1) rotateY(720deg);
  }
}

/* 周围小金币 */
.coin-small {
  position: absolute;
  font-size: 32px;
  animation: coinOrbit 2s ease-in-out infinite;
  filter: drop-shadow(0 0 8px rgba(251, 191, 36, 0.6));
}

/* 6 个小金币分布在周围 */
.coin-small:nth-child(2) { top: 10px; left: 50%; transform: translateX(-50%); animation-delay: 0s; }
.coin-small:nth-child(3) { top: 30%; right: 10px; animation-delay: 0.2s; }
.coin-small:nth-child(4) { bottom: 30%; right: 10px; animation-delay: 0.4s; }
.coin-small:nth-child(5) { bottom: 10px; left: 50%; transform: translateX(-50%); animation-delay: 0.6s; }
.coin-small:nth-child(6) { bottom: 30%; left: 10px; animation-delay: 0.8s; }
.coin-small:nth-child(7) { top: 30%; left: 10px; animation-delay: 1s; }

@keyframes coinOrbit {
  0%, 100% {
    transform: translateY(0) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translateY(-15px) scale(1.3);
    opacity: 1;
  }
}

.coin-text {
  font-size: 36px;
  font-weight: 700;
  color: #fbbf24;
  text-shadow: 0 2px 12px rgba(251, 191, 36, 0.8),
               0 0 30px rgba(251, 191, 36, 0.4);
  margin-top: 32px;
  animation: coinTextPop 0.6s ease, textGlow 2s ease-in-out infinite 0.6s;
}

@keyframes textGlow {
  0%, 100% {
    text-shadow: 0 2px 12px rgba(251, 191, 36, 0.8),
                 0 0 30px rgba(251, 191, 36, 0.4);
  }
  50% {
    text-shadow: 0 2px 20px rgba(251, 191, 36, 1),
                 0 0 50px rgba(251, 191, 36, 0.7),
                 0 0 80px rgba(251, 191, 36, 0.3);
  }
}

.coin-hint {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 16px;
  animation: fadeIn 0.5s ease 1s both;
}

@keyframes coinTextPop {
  0% {
    transform: scale(0);
  }
  50% {
    transform: scale(1.3);
  }
  100% {
    transform: scale(1);
  }
}

@media (max-width: 768px) {
  .records-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  
  .checkin-stats {
    flex-direction: column;
    gap: 20px;
    padding: 20px;
  }
  
  .stat-divider {
    width: 60px;
    height: 1px;
  }
}
</style>
