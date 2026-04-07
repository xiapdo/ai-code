<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { onBeforeUnmount, onMounted } from 'vue'

// 鼠标水波：根据指针位置更新 CSS 变量
const updateRipplePosition = (event: PointerEvent) => {
  const xPercent = (event.clientX / window.innerWidth) * 100
  const yPercent = (event.clientY / window.innerHeight) * 100
  document.documentElement.style.setProperty('--ripple-x', `${xPercent}%`)
  document.documentElement.style.setProperty('--ripple-y', `${yPercent}%`)
}

// 雨滴数量，可以自己调 5、6、8 等
const RAINDROP_COUNT = 8
let rainTimer: number | undefined

// 随机生成雨滴位置，写入 CSS 变量
const spawnRaindrops = () => {
  for (let i = 1; i <= RAINDROP_COUNT; i++) {
    const x = 10 + Math.random() * 80
    const y = 10 + Math.random() * 80
    document.documentElement.style.setProperty(`--rain-${i}-x`, `${x}%`)
    document.documentElement.style.setProperty(`--rain-${i}-y`, `${y}%`)
  }
}

onMounted(() => {
  window.addEventListener('pointermove', updateRipplePosition)
  spawnRaindrops()
  // 每 1.6 秒刷新一批雨滴位置，形成持续下雨效果
  rainTimer = window.setInterval(spawnRaindrops, 1600)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', updateRipplePosition)
  if (rainTimer) {
    window.clearInterval(rainTimer)
  }
})
</script>

<template>
  <BasicLayout />
</template>

<style>
/* 全局样式优化 + 水波变量 */
:root {
  --ripple-x: 50%;
  --ripple-y: 50%;
  --rain-1-x: 20%;
  --rain-1-y: 20%;
  --rain-2-x: 80%;
  --rain-2-y: 25%;
  --rain-3-x: 60%;
  --rain-3-y: 70%;
  --rain-4-x: 30%;
  --rain-4-y: 80%;
  --rain-5-x: 45%;
  --rain-5-y: 40%;
  --rain-6-x: 15%;
  --rain-6-y: 60%;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell',
    'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  position: relative;
  background:
    radial-gradient(circle at 15% 30%, rgba(74, 222, 128, 0.35), transparent 55%),
    radial-gradient(circle at 85% 20%, rgba(59, 130, 246, 0.3), transparent 55%),
    radial-gradient(circle at 60% 80%, rgba(249, 168, 212, 0.35), transparent 60%), #f0fdf4;
  background-size: 140% 140%;
  animation: breezeGlow 18s ease-in-out infinite;
}

html {
  overflow-x: hidden;
}

body::before,
body::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -1;
}

/* 柔光背景 */
body::before {
  background:
    radial-gradient(circle at 20% 70%, rgba(34, 197, 94, 0.2) 0, transparent 45%),
    radial-gradient(circle at 80% 40%, rgba(99, 102, 241, 0.25) 0, transparent 50%);
  filter: blur(40px);
  animation: floatBloom 24s linear infinite;
}

/* 雨滴水波 + 鼠标水波 */
body::after {
  background:
    radial-gradient(
      circle at var(--rain-1-x) var(--rain-1-y),
      rgba(59, 130, 246, 0.28) 0%,
      rgba(59, 130, 246, 0.12) 26%,
      transparent 40%
    ),
    radial-gradient(
      circle at var(--rain-2-x) var(--rain-2-y),
      rgba(16, 185, 129, 0.26) 0%,
      rgba(16, 185, 129, 0.1) 24%,
      transparent 38%
    ),
    radial-gradient(
      circle at var(--rain-3-x) var(--rain-3-y),
      rgba(56, 189, 248, 0.26) 0%,
      rgba(56, 189, 248, 0.12) 24%,
      transparent 38%
    ),
    radial-gradient(
      circle at var(--rain-4-x) var(--rain-4-y),
      rgba(59, 130, 246, 0.24) 0%,
      rgba(59, 130, 246, 0.1) 22%,
      transparent 36%
    ),
    radial-gradient(
      circle at var(--rain-5-x) var(--rain-5-y),
      rgba(34, 197, 94, 0.24) 0%,
      rgba(34, 197, 94, 0.1) 22%,
      transparent 36%
    ),
    radial-gradient(
      circle at var(--rain-6-x) var(--rain-6-y),
      rgba(56, 189, 248, 0.22) 0%,
      rgba(56, 189, 248, 0.1) 20%,
      transparent 34%
    ),
    radial-gradient(
      circle at var(--ripple-x) var(--ripple-y),
      rgba(56, 189, 248, 0.35) 0%,
      rgba(56, 189, 248, 0.18) 30%,
      transparent 44%
    ),
    linear-gradient(120deg, rgba(255, 255, 255, 0.35), rgba(255, 255, 255, 0));
  background-size:
    230% 230%,
    230% 230%,
    230% 230%,
    230% 230%,
    230% 230%,
    230% 230%,
    230% 230%,
    100% 100%;
  mix-blend-mode: soft-light;
  opacity: 0.95;
  animation: rippleExpand 34s linear infinite;
  animation-delay: 2s;
  will-change: background-size;
}

/* 背景整体缓慢流动 */
@keyframes breezeGlow {
  0% {
    background-position:
      0% 30%,
      100% 20%,
      50% 90%,
      50% 50%;
  }
  50% {
    background-position:
      20% 40%,
      90% 10%,
      60% 80%,
      50% 50%;
  }
  100% {
    background-position:
      0% 30%,
      100% 20%,
      50% 90%,
      50% 50%;
  }
}

/* 柔光区域缓慢位移 */
@keyframes floatBloom {
  0% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(-30px, -20px, 0) scale(1.05);
  }
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
}

/* 所有水波统一做缩放动画 */
@keyframes rippleExpand {
  0% {
    background-size:
      90% 90%,
      90% 90%,
      90% 90%,
      90% 90%,
      90% 90%,
      90% 90%,
      90% 90%,
      100% 100%;
  }
  50% {
    background-size:
      200% 200%,
      200% 200%,
      200% 200%,
      200% 200%,
      200% 200%,
      200% 200%,
      200% 200%,
      110% 110%;
  }
  100% {
    background-size:
      90% 90%,
      90% 90%,
      90% 90%,
      90% 90%,
      90% 90%,
      90% 90%,
      90% 90%,
      100% 100%;
  }
}
</style>
