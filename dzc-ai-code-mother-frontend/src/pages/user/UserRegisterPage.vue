<template>
  <div id="userRegisterPage">
    <h2 class="title">灵码 💕 秒哒 - 用户注册</h2>
    <div class="desc">不写一行代码，生成完整应用</div>
    <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item name="userName" :rules="[{ required: true, message: '请输入名称' }]">
        <a-input v-model:value="formState.userName" placeholder="请输入名称" />
      </a-form-item>
      <a-form-item
        name="phone"
        :rules="[{ validator: validatePhone, trigger: 'blur' }]"
      >
        <a-input v-model:value="formState.phone" placeholder="请输入手机号（选填）" />
      </a-form-item>
      <a-form-item
        name="email"
        :rules="[{ validator: validateEmail, trigger: 'blur' }]"
      >
        <a-input v-model:value="formState.email" placeholder="请输入邮箱（选填）" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请确认密码' },
          { min: 8, message: '密码不能小于 8 位' },
          { validator: validateCheckPassword },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" />
      </a-form-item>
      <div class="tips">
        已有账号？
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { userRegister } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue'
import { reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  userName: '',
  checkPassword: '',
  phone: '',
  email: '',
})

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

const validateCheckPassword = (rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value && value !== formState.userPassword) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  background: white;
  max-width: 720px;
  padding: 24px;
  margin: 24px auto;
}

.title {
  text-align: center;
  margin-bottom: 16px;
}

.desc {
  text-align: center;
  color: #bbb;
  margin-bottom: 16px;
}

.tips {
  margin-bottom: 16px;
  color: #bbb;
  font-size: 13px;
  text-align: right;
}
</style>
