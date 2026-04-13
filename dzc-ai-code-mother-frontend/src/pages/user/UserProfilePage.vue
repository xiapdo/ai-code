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
            <a-tag :color="loginUserStore.loginUser.userRole === 'admin' ? 'gold' : 'blue'">
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
                @click="switchToInfo"
              >
                我的信息
              </div>
              <div
                class="tab-button"
                :class="{ active: activeTab === 'edit' }"
                @click="switchToEdit"
              >
                修改信息
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
            <div v-else key="edit" class="edit-form">
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
                    <a-button @click="cancelEdit">取消</a-button>
                  </a-space>
                </a-form-item>
              </a-form>
            </div>
          </transition>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { editUser, uploadAvatar } from '@/api/userController'
import defaultAvatar from '@/assets/aiAvatar.png'
import { useLoginUserStore } from '@/stores/loginUser'
import { CopyOutlined, PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance, Rule } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { computed, reactive, ref } from 'vue'

const loginUserStore = useLoginUserStore()

const activeTab = ref('info')
const prevTab = ref('info')
const saving = ref(false)
const formRef = ref<FormInstance>()

const editForm = reactive({
  userAvatar: '',
  userName: '',
  userProfile: '',
  phone: '',
  email: '',
})

const transitionName = computed(() => {
  return prevTab.value === 'info' ? 'slide-left' : 'slide-right'
})

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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  height: fit-content;
}

.avatar-section {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

.user-name-section {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.role-tag {
  margin-bottom: 24px;
}

.info-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #333;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  color: #666;
  font-weight: 500;
}

.info-item .value {
  color: #333;
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
  color: #1890ff;
  font-size: 14px;
  transition: color 0.3s;
}

.copy-icon:hover {
  color: #40a9ff;
}

.detail-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  min-height: 500px;
}

.tab-header {
  margin-bottom: 24px;
}

.tab-buttons {
  display: flex;
  gap: 8px;
  border-bottom: 2px solid #f0f0f0;
  padding-bottom: 0;
}

.tab-button {
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: all 0.3s ease;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
}

.tab-button:hover {
  color: #1890ff;
}

.tab-button.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.info-list {
  padding: 16px 0;
}

.info-row {
  display: flex;
  align-items: flex-start;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 15px;
}

.info-row:last-child {
  border-bottom: none;
}

.field-label {
  color: #666;
  font-weight: 500;
  min-width: 100px;
  flex-shrink: 0;
}

.field-value {
  color: #333;
  flex: 1;
}

.edit-form {
  padding: 16px 0;
  max-width: 600px;
}

:deep(.ant-upload-select-picture-card) {
  width: 100px;
  height: 100px;
}

.fade-slide-enter-active,
.fade-slide-leave-active,
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from,
.slide-left-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.fade-slide-leave-to,
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
</style>
