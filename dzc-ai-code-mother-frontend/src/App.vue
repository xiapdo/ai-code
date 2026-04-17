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

  --color-primary: #15803d;
  --color-primary-light: #22c55e;
  --color-primary-lighter: #86efac;
  --color-primary-dark: #166534;
  --color-secondary: #059669;
  --color-accent: #d97706;
  --color-accent-light: #fbbf24;
  --color-background: #f0fdf4;
  --color-foreground: #0f172a;
  --color-muted: #f0f7f3;
  --color-border: #bbf7d0;
  --color-surface: rgba(255, 255, 255, 0.72);
  --color-surface-hover: rgba(255, 255, 255, 0.88);
  --glass-blur: 16px;
  --glass-border: 1px solid rgba(255, 255, 255, 0.35);
  --glass-shadow: 0 8px 32px rgba(21, 128, 61, 0.12);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 24px;
  --transition-fast: 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-normal: 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

* {
  box-sizing: border-box;
}

html {
  overflow-x: hidden;
  scrollbar-gutter: stable;
}

body {
  margin: 0;
  padding: 0;
  font-family:
    'Raleway',
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    'Roboto',
    'Oxygen',
    'Ubuntu',
    'Cantarell',
    'Fira Sans',
    'Droid Sans',
    'Helvetica Neue',
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
  position: relative;
  background: #f8faf9;
}

body::before,
body::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -1;
}

body::before {
  background:
    radial-gradient(ellipse 500px 400px at 10% 20%, rgba(34, 197, 94, 0.12) 0%, transparent 70%),
    radial-gradient(ellipse 400px 350px at 90% 80%, rgba(5, 150, 105, 0.1) 0%, transparent 70%);
  animation: softFloat 18s ease-in-out infinite;
}

body::after {
  background:
    radial-gradient(ellipse 350px 300px at 85% 15%, rgba(217, 119, 6, 0.08) 0%, transparent 70%),
    radial-gradient(ellipse 300px 250px at 15% 85%, rgba(34, 197, 94, 0.1) 0%, transparent 70%);
  animation: softFloat 22s ease-in-out infinite reverse;
}

@keyframes softFloat {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.8;
  }
  25% {
    transform: translate(15px, -20px) scale(1.02);
    opacity: 1;
  }
  50% {
    transform: translate(-10px, 10px) scale(0.98);
    opacity: 0.9;
  }
  75% {
    transform: translate(20px, 15px) scale(1.01);
    opacity: 0.85;
  }
}

@media (prefers-reduced-motion: reduce) {
  body::before,
  body::after {
    animation: none;
  }
}

.ant-btn-primary {
  background: linear-gradient(135deg, var(--color-primary), var(--color-secondary)) !important;
  border: none !important;
  border-radius: var(--radius-sm) !important;
  backdrop-filter: blur(var(--glass-blur)) !important;
  box-shadow:
    0 4px 16px rgba(21, 128, 61, 0.25),
    inset 0 1px 0 rgba(255, 255, 255, 0.2) !important;
  transition: all var(--transition-normal) !important;
  color: #ffffff !important;
}

.ant-btn-primary:hover {
  background: linear-gradient(135deg, var(--color-primary-light), var(--color-primary)) !important;
  transform: translateY(-2px) !important;
  box-shadow:
    0 8px 24px rgba(21, 128, 61, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.3) !important;
}

.ant-btn-primary:active {
  transform: translateY(0) !important;
}

.ant-btn-default {
  background: var(--color-surface) !important;
  border: var(--glass-border) !important;
  border-radius: var(--radius-sm) !important;
  backdrop-filter: blur(var(--glass-blur)) !important;
  box-shadow: var(--glass-shadow) !important;
  color: var(--color-primary) !important;
  transition: all var(--transition-normal) !important;
}

.ant-btn-default:hover {
  background: var(--color-surface-hover) !important;
  border-color: var(--color-primary-lighter) !important;
  color: var(--color-primary-dark) !important;
  transform: translateY(-2px) !important;
  box-shadow:
    0 8px 24px rgba(21, 128, 61, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.4) !important;
}

.ant-card {
  background: var(--color-surface) !important;
  border: var(--glass-border) !important;
  border-radius: var(--radius-lg) !important;
  backdrop-filter: blur(var(--glass-blur)) !important;
  box-shadow: var(--glass-shadow) !important;
}

.ant-modal-content {
  background: var(--color-surface-hover) !important;
  border: var(--glass-border) !important;
  border-radius: var(--radius-lg) !important;
  backdrop-filter: blur(20px) !important;
  box-shadow: 0 16px 48px rgba(21, 128, 61, 0.15) !important;
}

.ant-input,
.ant-input-password,
.ant-input-affix-wrapper,
.ant-select-selector,
.ant-picker {
  border-radius: var(--radius-sm) !important;
  border-color: var(--color-border) !important;
  transition: all var(--transition-fast) !important;
  background: rgba(255, 255, 255, 0.6) !important;
}

.ant-input:focus,
.ant-input-focused,
.ant-input-affix-wrapper:focus,
.ant-input-affix-wrapper-focused,
.ant-select-focused .ant-select-selector {
  border-color: var(--color-primary-light) !important;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.15) !important;
  background: rgba(255, 255, 255, 0.85) !important;
}

.ant-modal .ant-modal-content {
  background: var(--color-surface-hover) !important;
}

.ant-modal .ant-modal-header {
  background: transparent !important;
}

.ant-modal .ant-modal-body {
  background: transparent !important;
}

.ant-modal .ant-modal-footer {
  background: transparent !important;
  border-top-color: rgba(34, 197, 94, 0.15) !important;
}

.ant-tag {
  background: rgba(34, 197, 94, 0.1) !important;
  border-color: rgba(34, 197, 94, 0.25) !important;
  color: var(--color-primary) !important;
}

/* 全局替换 Ant Design 默认蓝色为森林绿 */
.ant-menu-item-selected,
.ant-menu-item-active,
.ant-menu-submenu-selected .ant-menu-submenu-title {
  color: var(--color-primary, #15803d) !important;
}

.ant-menu-horizontal > .ant-menu-item-selected::after,
.ant-menu-horizontal > .ant-menu-item-active::after,
.ant-menu-horizontal > .ant-menu-submenu-selected::after {
  border-bottom-color: var(--color-primary, #15803d) !important;
}

.ant-btn-link {
  color: var(--color-secondary, #059669) !important;
}

.ant-btn-link:hover {
  color: var(--color-primary, #15803d) !important;
}

.ant-link,
a {
  color: var(--color-secondary, #059669);
}

.ant-link:hover,
a:hover {
  color: var(--color-primary, #15803d);
}

.ant-pagination .ant-pagination-item-active {
  border-color: var(--color-primary) !important;
}

.ant-pagination .ant-pagination-item-active a {
  color: var(--color-primary) !important;
}

.ant-tag-green {
  background: rgba(34, 197, 94, 0.12) !important;
  border-color: rgba(34, 197, 94, 0.3) !important;
  color: var(--color-primary) !important;
}
</style>
