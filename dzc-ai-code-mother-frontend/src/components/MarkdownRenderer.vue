<template>
  <div class="markdown-content" v-html="renderedMarkdown" @click="handleClick"></div>
</template>

<script setup lang="ts">
import hljs from 'highlight.js'
import MarkdownIt from 'markdown-it'
import { computed } from 'vue'

import 'highlight.js/styles/github.css'

interface Props {
  content: string
  isGenerating?: boolean
  streamingLanguage?: string | null
  latestToolFileName?: string
}

const props = withDefaults(defineProps<Props>(), {
  isGenerating: false,
  streamingLanguage: null,
  latestToolFileName: '',
})

const emit = defineEmits<{
  (e: 'viewCode', fileName: string): void
}>()

const LANG_TO_EXT: Record<string, string[]> = {
  html: ['html'], css: ['css'], js: ['js'], javascript: ['js'],
  ts: ['ts'], typescript: ['ts'], vue: ['vue'], json: ['json'],
}

function extractAllToolFiles(content: string): string[] {
  const results: string[] = []
  const patterns = [
    /\[工具调用[^\]]*\]\s*写入文件\s+([^\s\n`]+)/gi,
    /\]\s*写入文件\s+([^\s\n`]+)/gi,
    /写入文件[：:\s]+([^\s\n`]+)/gi,
    /正在创建文件[:：\s]+([^\s\n`]+)/gi,
  ]
  for (const pattern of patterns) {
    const matches = content.matchAll(pattern)
    for (const match of matches) {
      let fileName = match[1].trim()
      fileName = fileName.replace(/[`'"*]/g, '')
      if (fileName && !results.includes(fileName)) {
        results.push(fileName)
      }
    }
  }
  return results
}

function guessFileNameByLang(lang: string, index: number): string {
  const langLower = lang.toLowerCase()
  
  if (langLower === 'html') return index === 0 ? 'index.html' : `page${index + 1}.html`
  if (langLower === 'css') return index === 0 ? 'style.css' : `style${index + 1}.css`
  if (langLower === 'js' || langLower === 'javascript') return index === 0 ? 'app.js' : `script${index + 1}.js`
  if (langLower === 'ts' || langLower === 'typescript') return index === 0 ? 'app.ts' : `script${index + 1}.ts`
  if (langLower === 'vue') return index === 0 ? 'App.vue' : `Component${index + 1}.vue`
  if (langLower === 'json') return 'package.json'
  
  return `file${index + 1}.${lang || 'txt'}`
}

function matchFileNameForFence(
  lang: string,
  content: string,
  fileNameMatch: RegExpMatchArray | null,
  allToolFiles: string[],
  usedToolFiles: Set<string>,
  fenceIndex: number
): string {
  if (fileNameMatch) return fileNameMatch[1].trim()

  const langLower = lang.toLowerCase()
  for (const toolFile of allToolFiles) {
    if (usedToolFiles.has(toolFile)) continue
    const toolExt = toolFile.split('.').pop()?.toLowerCase()
    const validExts = LANG_TO_EXT[langLower] || []
    if (validExts.includes(toolExt || '')) {
      usedToolFiles.add(toolFile)
      return toolFile
    }
  }

  return guessFileNameByLang(lang, fenceIndex)
}

const md: MarkdownIt = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return (
          '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
        )
      } catch {
        // 忽略错误
      }
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  },
})

md.renderer.rules.fence = function (tokens, idx) {
  const token = tokens[idx]
  const lang = token.info.trim()
  const content = token.content

  const fileNameMatch = content.match(/^\/\/\s*文件名[:：]\s*(.+)$/m)
  const langLabel = lang || 'code'

  const allToolFiles = extractAllToolFiles(props.content)
  const usedToolFiles = new Set<string>()

  // 计算当前语言类型的索引（用于文件名推断）
  const langCounts: Record<string, number> = {}

  for (let i = 0; i < idx; i++) {
    const prevToken = tokens[i]
    if (prevToken.type === 'fence') {
      const prevLang = prevToken.info.trim() || 'code'
      const prevContent = prevToken.content
      const prevMatch = prevContent.match(/^\/\/\s*文件名[:：]\s*(.+)$/m)

      // 更新语言计数
      const prevLangLower = prevLang.toLowerCase()
      langCounts[prevLangLower] = (langCounts[prevLangLower] || 0) + 1

      matchFileNameForFence(prevLang, prevContent, prevMatch, allToolFiles, usedToolFiles, langCounts[prevLangLower] - 1)
    }
  }

  // 更新当前语言计数
  const langLower = langLabel.toLowerCase()
  langCounts[langLower] = (langCounts[langLower] || 0) + 1
  const currentLangIndex = langCounts[langLower] - 1

  const displayFileName = matchFileNameForFence(langLabel, content, fileNameMatch, allToolFiles, usedToolFiles, currentLangIndex)

  const isCurrentlyStreaming = props.isGenerating && props.latestToolFileName && (
    displayFileName === props.latestToolFileName ||
    displayFileName.endsWith('/' + props.latestToolFileName) ||
    props.latestToolFileName.endsWith('/' + displayFileName)
  )

  return `<div class="code-block-card" data-file-name="${displayFileName}" data-language="${langLabel}" style="cursor:pointer;display:flex;align-items:center;justify-content:space-between;gap:12px;padding:10px 14px;border-radius:10px;background:#f8f9fa;border:1px solid #e5e7eb;">
  <div class="file-info-badge" style="flex:1;display:flex;align-items:center;gap:8px;">
    <svg style="width:18px;height:18px;color:#6b7280;flex-shrink:0;" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path><polyline points="13 2 13 9 20 9"></polyline></svg>
    <span style="font-family:monospace;font-size:16px;color:#1f2937;font-weight:500;">${displayFileName}</span>
  </div> 
  <svg class="code-spinner" data-spinner="true" style="width:20px;height:20px;color:#52c41a;flex-shrink:0;${isCurrentlyStreaming ? 'display:inline-block;animation:code-block-spin 1s linear infinite;' : 'display:none;'}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10" stroke-opacity="0.25"></circle><path d="M12 2a10 10 0 0 1 10 10" stroke-linecap="round"></path></svg>
</div>`
}

const renderedMarkdown = computed(() => {
  // 依赖 props.content, props.latestToolFileName, props.isGenerating
  // 当这些 props 变化时，重新渲染
  void props.latestToolFileName
  void props.isGenerating
  return md.render(props.content)
})

const handleClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  const card = target.closest('.code-block-card')
  if (card) {
    const fileName = card.getAttribute('data-file-name') || ''
    if (fileName) {
      emit('viewCode', fileName)
    }
  }
}
</script>

<style scoped>
.markdown-content {
  line-height: 1.6;
  color: #333;
  word-wrap: break-word;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 1.5em 0 0.5em 0;
  font-weight: 600;
  line-height: 1.25;
}

.markdown-content :deep(h1) {
  font-size: 1.5em;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h2) {
  font-size: 1.3em;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-content :deep(h3) {
  font-size: 1.1em;
}

.markdown-content :deep(p) {
  margin: 0.8em 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0.8em 0;
  padding-left: 1.5em;
}

.markdown-content :deep(li) {
  margin: 0.3em 0;
}

.markdown-content :deep(blockquote) {
  margin: 1em 0;
  padding: 0.5em 1em;
  border-left: 4px solid #ddd;
  background-color: #f9f9f9;
  color: #666;
}

.markdown-content :deep(code) {
  background-color: #f1f1f1;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
}

.markdown-content :deep(pre) {
  background-color: #f8f8f8;
  border: 1px solid #e1e1e1;
  border-radius: 6px;
  padding: 1em;
  overflow-x: auto;
  margin: 1em 0;
}

.markdown-content :deep(pre code) {
  background-color: transparent;
  padding: 0;
  border-radius: 0;
  font-size: 0.9em;
  line-height: 1.4;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
}

.markdown-content :deep(table th),
.markdown-content :deep(table td) {
  border: 1px solid #ddd;
  padding: 0.5em 0.8em;
  text-align: left;
}

.markdown-content :deep(table th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

.markdown-content :deep(table tr:nth-child(even)) {
  background-color: #f9f9f9;
}

.markdown-content :deep(a) {
  color: var(--color-primary, #15803d);
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 0.5em 0;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid #eee;
  margin: 1.5em 0;
}

.markdown-content :deep(.hljs) {
  background-color: #f8f8f8 !important;
  border-radius: 6px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
  line-height: 1.4;
}

.markdown-content :deep(.hljs-keyword) {
  color: #d73a49;
  font-weight: 600;
}

.markdown-content :deep(.hljs-string) {
  color: #032f62;
}

.markdown-content :deep(.hljs-comment) {
  color: #6a737d;
  font-style: italic;
}

.markdown-content :deep(.hljs-number) {
  color: #005cc5;
}

.markdown-content :deep(.hljs-function) {
  color: #6f42c1;
}

.markdown-content :deep(.hljs-tag) {
  color: #22863a;
}

.markdown-content :deep(.hljs-attr) {
  color: #6f42c1;
}

.markdown-content :deep(.hljs-title) {
  color: #6f42c1;
  font-weight: 600;
}

.code-block-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: #f8f9fa;
  border: 1px solid #e5e7eb;
}

.markdown-content :deep(.code-block-card) {
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.markdown-content :deep(.code-block-card:hover) {
  background: #f0fdf4;
  border-color: #bbf7d0;
  box-shadow:
    0 4px 12px rgba(34, 197, 94, 0.15),
    0 2px 4px rgba(34, 197, 94, 0.08);
  transform: translateY(-2px);
}

.markdown-content :deep(.file-info-badge) {
  min-width: 0;
}
</style>

<style>
@keyframes code-block-spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
