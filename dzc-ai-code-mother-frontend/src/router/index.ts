import AppManagePage from '@/pages/admin/AppManagePage.vue'
import ChatManagePage from '@/pages/admin/ChatManagePage.vue'
import ChatRoomManagePage from '@/pages/admin/ChatRoomManagePage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import AppChatPage from '@/pages/app/AppChatPage.vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'
import ChatCreateOrJoinPage from '@/pages/chat/ChatCreateOrJoinPage.vue'
import ChatInvitePage from '@/pages/chat/ChatInvitePage.vue'
import ChatRoomSimplePage from '@/pages/chat/ChatRoomSimplePage.vue'
import ChatTestPage from '@/pages/chat/ChatTestPage.vue'
import HomePage from '@/pages/HomePage.vue'
import NotificationPage from '@/pages/user/NotificationPage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserProfilePage from '@/pages/user/UserProfilePage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/user/profile',
      name: '个人中心',
      component: UserProfilePage,
    },
    {
      path: '/notification',
      name: '我的通知',
      component: NotificationPage,
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
    },
    {
      path: '/admin/appManage',
      name: '应用管理',
      component: AppManagePage,
    },
    {
      path: '/admin/chatManage',
      name: '对话管理',
      component: ChatManagePage,
    },
    {
      path: '/admin/chatRoomManage',
      name: '群聊管理',
      component: ChatRoomManagePage,
    },
    {
      path: '/app/chat/:id',
      name: '应用对话',
      component: AppChatPage,
    },
    {
      path: '/app/edit/:id',
      name: '编辑应用',
      component: AppEditPage,
    },
    {
      path: '/chat/create-or-join',
      name: '创建或加入群聊',
      component: ChatCreateOrJoinPage,
    },
    {
      path: '/chat/room',
      name: '我的群聊',
      component: ChatCreateOrJoinPage,
    },
    {
      path: '/chat/room/:id',
      name: '群聊聊天',
      component: ChatRoomSimplePage,
    },
    {
      path: '/chat/invite/:code',
      name: '加入群聊',
      component: ChatInvitePage,
    },
    {
      path: '/chat/test',
      name: 'WebSocket测试',
      component: ChatTestPage,
    },
  ],
})

export default router
