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
  background: rgba(240, 253, 244, 0.82);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  text-align: center;
  padding: 20px;
  border-top: 1px solid rgba(34, 197, 94, 0.2);
  box-shadow: 0 -6px 20px rgba(21, 128, 61, 0.08);
}

.copyright {
  margin: 0;
  color: var(--color-foreground, #0f172a);
  font-size: 14px;
}

.author-link {
  color: var(--color-primary, #15803d);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.author-link:hover {
  color: var(--color-primary-light, #22c55e);
}
</style>
