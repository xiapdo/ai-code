<template>
  <div style="padding: 20px; font-family: monospace">
    <h1>WebSocket 测试页面</h1>

    <div style="margin-bottom: 20px">
      <h3>
        状态:
        <span :style="{ color: connected ? 'green' : 'red' }">{{
          connected ? '已连接' : '未连接'
        }}</span>
      </h3>
      <h3>群聊ID: <input v-model="testRoomId" type="text" style="width: 200px" /></h3>
    </div>

    <div style="margin-bottom: 20px">
      <button @click="connect" :disabled="connected">连接</button>
      <button @click="disconnect" :disabled="!connected" style="margin-left: 10px">断开</button>
      <button @click="clearLogs" style="margin-left: 10px">清空日志</button>
    </div>

    <div style="margin-bottom: 20px">
      <input v-model="testMessage" type="text" placeholder="输入测试消息" style="width: 400px" />
      <button @click="sendTestMessage" :disabled="!connected" style="margin-left: 10px">
        发送
      </button>
    </div>

    <div style="background: #f5f5f5; padding: 10px; height: 500px; overflow-y: auto">
      <div v-for="(log, index) in logs" :key="index" :style="{ color: log.color }">
        [{{ log.time }}] {{ log.message }}
      </div>
    </div>

    <div style="margin-top: 20px">
      <h3>收到的消息:</h3>
      <div
        v-for="(msg, index) in receivedMessages"
        :key="index"
        style="background: #e6f7ff; margin: 5px 0; padding: 10px"
      >
        <div><strong>发送者:</strong> {{ msg.userName }} (ID: {{ msg.userId }})</div>
        <div><strong>内容:</strong> {{ msg.messageContent }}</div>
        <div><strong>时间:</strong> {{ msg.createTime }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { ref } from 'vue'

const testRoomId = ref('1')
const testMessage = ref('测试消息')
const connected = ref(false)
const logs = ref<Array<{ time: string; message: string; color: string }>>([])
const receivedMessages = ref<any[]>([])

let stompClient: any = null

const addLog = (message: string, color: string = 'black') => {
  const now = new Date()
  const time = now.toLocaleTimeString()
  logs.value.push({ time, message, color })
}

const connect = () => {
  try {
    addLog('开始连接 WebSocket...', 'blue')

    const socket = new SockJS('/api/ws')
    stompClient = Stomp.over(socket)

    stompClient.debug = (str: string) => {
      addLog('[STOMP] ' + str, 'gray')
    }

    stompClient.connect(
      {},
      (frame: any) => {
        connected.value = true
        addLog('✅ 连接成功!', 'green')
        addLog('Frame: ' + JSON.stringify(frame), 'gray')

        // 订阅群聊消息
        const subscribePath = `/topic/chat/${testRoomId.value}`
        addLog('订阅路径: ' + subscribePath, 'blue')

        stompClient.subscribe(subscribePath, (message: any) => {
          addLog('📩 收到消息: ' + JSON.stringify(message.body), 'green')

          try {
            const msg = JSON.parse(message.body)
            receivedMessages.value.push(msg)
            addLog('解析后: ' + JSON.stringify(msg), 'blue')
          } catch (e: any) {
            addLog('解析失败: ' + e.message, 'red')
          }
        })

        addLog('✅ 订阅成功!', 'green')
      },
      (error: any) => {
        connected.value = false
        addLog('❌ 连接失败: ' + JSON.stringify(error), 'red')
      },
    )
  } catch (error: any) {
    addLog('❌ 异常: ' + error.message, 'red')
  }
}

const disconnect = () => {
  if (stompClient) {
    stompClient.disconnect(() => {
      connected.value = false
      addLog('已断开连接', 'orange')
    })
  }
}

const sendTestMessage = () => {
  if (!stompClient || !connected.value) {
    addLog('❌ 未连接，无法发送', 'red')
    return
  }

  const messageData = {
    content: testMessage.value,
    messageType: 'text',
  }

  const sendPath = `/app/chat/${testRoomId.value}/send`
  addLog('发送路径: ' + sendPath, 'blue')
  addLog('发送数据: ' + JSON.stringify(messageData), 'blue')

  try {
    stompClient.send(sendPath, {}, JSON.stringify(messageData))
    addLog('✅ 发送成功!', 'green')
  } catch (error: any) {
    addLog('❌ 发送失败: ' + error.message, 'red')
  }
}

const clearLogs = () => {
  logs.value = []
  receivedMessages.value = []
}
</script>
