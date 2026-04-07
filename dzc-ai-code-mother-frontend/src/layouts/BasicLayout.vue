<template>
  <a-layout class="basic-layout">
    <!-- 顶部导航栏 -->
    <GlobalHeader />
    <!-- 主要内容区域 -->
    <a-layout-content class="main-content">
      <router-view />
    </a-layout-content>
    <!-- 底部版权信息 -->
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalHeader from '@/components/GlobalHeader.vue'
</script>

<style scoped>
.basic-layout {
  background: none;
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

.basic-layout::before,
.basic-layout::after {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: -1;
}

.basic-layout::before {
  background:
    radial-gradient(circle at 10% 20%, rgba(134, 239, 172, 0.35) 0, transparent 45%),
    radial-gradient(circle at 90% 30%, rgba(96, 165, 250, 0.3) 0, transparent 50%),
    radial-gradient(circle at 50% 80%, rgba(248, 250, 252, 0.4) 0, transparent 60%);
  filter: blur(30px);
  animation: blossomDrift 26s linear infinite;
}

.basic-layout::after {
  background-image:
    radial-gradient(
      circle 120px at var(--petal-x, 30%) 20%,
      rgba(248, 113, 113, 0.2),
      transparent 60%
    ),
    radial-gradient(
      circle 160px at 70% var(--petal-y, 70%),
      rgba(250, 204, 21, 0.15),
      transparent 70%
    );
  animation: twinkleSoft 18s ease-in-out infinite alternate;
}

.main-content {
  width: 100%;
  padding: 24px 24px 48px;
  background: transparent;
  margin: 0;
}

@keyframes blossomDrift {
  0% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(-25px, -20px, 0);
  }
  100% {
    transform: translate3d(0, 0, 0);
  }
}

@keyframes twinkleSoft {
  0% {
    opacity: 0.3;
  }
  100% {
    opacity: 0.7;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px 12px 32px;
  }
}
</style>
