<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <div class="logo" aria-label="灵码 logo" role="img"></div>
            <h1 class="site-title">灵码💕秒哒</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <a-avatar :src="loginUserStore.loginUser.userAvatar || defaultAvatar" />
                {{ loginUserStore.loginUser.userName ?? '十二' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item class="logout-menu-item" @click="doLogout">
                    <LogoutOutlined />
                    <span>退出登录</span>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { userLogout } from '@/api/userController.ts'
import defaultAvatar from '@/assets/aiAvatar.png'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { HomeOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { type MenuProps, message } from 'ant-design-vue'
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'

const loginUserStore = useLoginUserStore()
const router = useRouter()
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to, from, next) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: 'others',
    label: '其他',
    title: '其他',
  },
]

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    if (menuKey?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}

// 退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  background: linear-gradient(
    120deg,
    rgba(248, 250, 252, 0.9),
    rgba(187, 247, 208, 0.85),
    rgba(191, 219, 254, 0.85)
  );
  padding: 0 24px;
  border-bottom: 1px solid rgba(74, 222, 128, 0.35);
  box-shadow: 0 10px 28px rgba(14, 165, 233, 0.18);
  position: sticky;
  top: 0;
  z-index: 50;
  backdrop-filter: blur(18px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 46px;
  width: 46px;
  border-radius: 50%;
  background-image: url('../assets/logo.png');
  background-size: cover;
  background-position: center;
  border: 3px solid rgba(255, 255, 255, 0.8);
  box-shadow:
    0 6px 16px rgba(236, 72, 153, 0.45),
    inset 0 0 8px rgba(59, 130, 246, 0.35);
  transition: transform 0.9s ease-in-out;
}

.logo:hover {
  transform: rotate(360deg) scale(1.02);
}

.site-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: 1px;
  text-shadow: 0 2px 6px rgba(16, 185, 129, 0.25);
}

.ant-menu-horizontal {
  border-bottom: none !important;
}

.ant-menu {
  background: transparent;
}

.ant-menu-item,
.ant-menu-submenu-title {
  color: #0f172a !important;
  transition:
    color 0.3s,
    text-shadow 0.3s;
}

.ant-menu-item-selected,
.ant-menu-item-active {
  color: #0284c7 !important;
  text-shadow: 0 2px 8px rgba(14, 165, 233, 0.4);
}

.ant-menu-horizontal > .ant-menu-item::after,
.ant-menu-horizontal > .ant-menu-submenu::after {
  border-bottom-width: 2px;
  border-bottom-color: rgba(34, 197, 94, 0.8);
}

.user-login-status {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

:deep(.logout-menu-item) {
  font-size: 14px;
}

:deep(.logout-menu-item .ant-dropdown-menu-title-content) {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

:deep(.logout-menu-item .anticon) {
  font-size: 16px;
}
</style>
