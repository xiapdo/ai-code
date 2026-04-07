<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <p class="copyright">
        <a
          href="https://www.codefather.cn"
          target="_blank"
          rel="noopener noreferrer"
          class="author-link"
        >
          灵码💕秒哒 平台💕💕💕💕💕
        </a>
      </p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.footer {
  background: linear-gradient(
    120deg,
    rgba(248, 250, 252, 0.95),
    rgba(187, 247, 208, 0.9),
    rgba(191, 219, 254, 0.9)
  );
  backdrop-filter: blur(18px);
  text-align: center;
  padding: 20px;
  margin-top: 40px;
  border-top: 1px solid rgba(74, 222, 128, 0.4);
  box-shadow: 0 -8px 24px rgba(14, 165, 233, 0.16);
}

.copyright {
  margin: 0;
  color: #0f172a;
  font-size: 14px;
}

.author-link {
  color: #0284c7;
  text-decoration: none;
  font-weight: 500;
}

.author-link:hover {
  color: #0ea5e9;
}
</style>
