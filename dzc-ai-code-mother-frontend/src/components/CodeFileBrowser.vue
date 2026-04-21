<template>
  <div class="code-file-browser">
    <div class="browser-container">
      <div class="file-tree-panel">
        <div class="file-tree-header">
          <span class="file-tree-title">文件</span>
        </div>
        <div class="file-tree-content">
          <div v-if="treeNodes.length === 0 && isGenerating" class="file-tree-empty">
            <div class="empty-animation">
              <a-spin size="large" />
              <div class="empty-loading-text">
                <span>AI 正在生成代码文件...</span>
              </div>
            </div>
          </div>
          <div v-else-if="treeNodes.length === 0" class="file-tree-empty">
            <FileOutlined class="empty-icon" />
            <p>暂无文件</p>
          </div>
          <template v-else>
            <tree-node
              v-for="node in treeNodes"
              :key="node.key"
              :node="node"
              :selected-path="selectedFile?.path || ''"
              :generating-path="generatingFilePath"
              @select="handleNodeSelect"
              @toggle="toggleFolder"
            />
          </template>
        </div>
      </div>

      <div class="code-panel">
        <div v-if="selectedFile" class="code-panel-content">
          <div class="code-panel-header">
            <component :is="getFileIcon(selectedFile)" class="file-icon" />
            <span class="file-path">{{ selectedFile.path }}</span>
            <div class="code-panel-actions">
              <a-button
                v-if="!isEditMode"
                type="link"
                size="small"
                @click="startEdit"
              >
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <template v-else>
                <a-button type="link" size="small" @click="cancelEdit">取消</a-button>
                <a-button type="primary" size="small" @click="saveEdit">保存</a-button>
              </template>
            </div>
          </div>
          <div ref="codeViewerRef" class="code-viewer">
            <pre v-if="selectedFile.content && !isEditMode"><code v-html="highlightedCode"></code></pre>
            <div v-else-if="isEditMode && selectedFile.content" class="editor-wrapper">
              <VueMonacoEditor
                v-model:value="editContent"
                :language="editorLanguage"
                theme="vs"
                :options="editorOptions"
                path="monaco-editor"
              />
            </div>
            <div v-else class="code-empty">
              <p>等待文件生成...</p>
            </div>
          </div>
        </div>
        <div v-else class="code-panel-empty">
          <FileOutlined class="empty-icon" />
          <p>选择一个文件查看代码</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { VueMonacoEditor, loader } from '@guolao/vue-monaco-editor'
import hljs from 'highlight.js'
import { type Component, computed, nextTick, ref, watch } from 'vue'

loader.config({
  paths: {
    vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.44.0/min/vs',
  },
})

import {
  CodeOutlined,
  EditOutlined,
  FileImageOutlined,
  FileOutlined,
  FileTextOutlined,
  Html5Outlined,
} from '@ant-design/icons-vue'

import 'highlight.js/styles/github.css'

import TreeNode from './TreeNode.vue'

interface CodeFile {
  name: string
  path: string
  language: string
  content: string
}

interface TreeNodeType {
  key: string
  name: string
  isFolder: boolean
  children: TreeNodeType[]
  file?: CodeFile
  expanded: boolean
}

interface Props {
  files: CodeFile[]
  activeFilePath?: string
  isGenerating?: boolean
  streamingLanguage?: string | null
  latestToolFileName?: string
}

const props = withDefaults(defineProps<Props>(), {
  files: () => [],
  activeFilePath: '',
  isGenerating: false,
  streamingLanguage: null,
  latestToolFileName: '',
})

const selectedFile = ref<CodeFile | null>(null)
const codeViewerRef = ref<HTMLElement | null>(null)
const isEditMode = ref(false)
const editContent = ref('')
const originalContent = ref('')

const treeNodes = ref<TreeNodeType[]>([])
const expandedFolders = ref<Set<string>>(new Set(['root']))

const generatingFilePath = ref<string>('')

const emit = defineEmits<{
  (e: 'select', file: CodeFile): void
  (e: 'save', file: CodeFile, newContent: string): void
}>()

const editorLanguage = computed(() => {
  if (!selectedFile.value) return 'plaintext'
  const lang = selectedFile.value.language.toLowerCase()
  const langMap: Record<string, string> = {
    vue: 'vue',
    html: 'html',
    htm: 'html',
    css: 'css',
    scss: 'scss',
    less: 'less',
    js: 'javascript',
    ts: 'typescript',
    json: 'json',
    md: 'markdown',
    xml: 'xml',
    yaml: 'yaml',
    yml: 'yaml',
    sql: 'sql',
    java: 'java',
    python: 'python',
    py: 'python',
    go: 'go',
    rust: 'rust',
    c: 'c',
    cpp: 'cpp',
    csharp: 'csharp',
    cs: 'csharp',
    php: 'php',
    ruby: 'ruby',
    rb: 'ruby',
    shell: 'shell',
    sh: 'shell',
    bash: 'shell',
  }
  return langMap[lang] || lang
})

const editorOptions = {
  minimap: { enabled: false },
  fontSize: 13,
  lineNumbers: 'on' as const,
  roundedSelection: true,
  scrollBeyondLastLine: false,
  automaticLayout: true,
  tabSize: 2,
  wordWrap: 'on' as const,
  folding: true,
  foldingStrategy: 'indentation' as const,
  renderWhitespace: 'selection' as const,
  cursorBlinking: 'smooth' as const,
  cursorSmoothCaretAnimation: 'on' as const,
  smoothScrolling: true,
  padding: { top: 16, bottom: 16 },
  suggest: {
    showKeywords: true,
    showSnippets: true,
    showClasses: true,
    showFunctions: true,
    showVariables: true,
    showConstants: true,
    showProperties: true,
    showMethods: true,
    showModules: true,
    showInterfaces: true,
    showColors: true,
    showFiles: true,
    showFolders: true,
  },
  quickSuggestions: {
    other: true,
    comments: false,
    strings: true,
  },
  parameterHints: { enabled: true },
  formatOnPaste: true,
  formatOnType: true,
}

const buildTree = (files: CodeFile[]): TreeNodeType[] => {
  const root: TreeNodeType[] = []
  const nodeMap = new Map<string, TreeNodeType>()
  const rootOrder: string[] = []

  for (const file of files) {
    const parts = file.path.split('/')
    let currentPath = ''
    let parentNodes = root

    for (let i = 0; i < parts.length; i++) {
      const part = parts[i]
      const isFile = i === parts.length - 1
      const nodeKey = currentPath ? `${currentPath}/${part}` : part

      if (!nodeMap.has(nodeKey)) {
        const node: TreeNodeType = {
          key: nodeKey,
          name: part,
          isFolder: !isFile,
          children: [],
          expanded: expandedFolders.value.has(nodeKey),
        }
        if (isFile) {
          node.file = file
        }
        nodeMap.set(nodeKey, node)
        parentNodes.push(node)
        if (i === 0) {
          rootOrder.push(nodeKey)
        }
      }

      const existingNode = nodeMap.get(nodeKey)!
      parentNodes = existingNode.children
      currentPath = nodeKey
    }
  }

  root.sort((a, b) => rootOrder.indexOf(a.key) - rootOrder.indexOf(b.key))

  return root
}

const findFileByToolName = (files: CodeFile[], toolFileName: string): CodeFile | undefined => {
  // 清理文件名（去除前后空格和特殊字符）
  const cleanToolFileName = toolFileName.trim().replace(/[`'"*]/g, '')
  
  // 1. 精确匹配 path
  let target = files.find((f) => f.path === cleanToolFileName)
  if (target) return target
  
  // 2. 精确匹配 name
  target = files.find((f) => f.name === cleanToolFileName)
  if (target) return target
  
  // 3. 路径结尾匹配（如 toolFileName 是 "src/pages/Contact.vue"，匹配 path 以它结尾的文件）
  target = files.find((f) => f.path.endsWith(cleanToolFileName))
  if (target) return target
  
  // 4. 文件名结尾匹配（如 toolFileName 是 "Contact.vue"，匹配 name 以它结尾的文件）
  target = files.find((f) => cleanToolFileName.endsWith(f.name))
  if (target) return target
  
  // 5. 路径包含匹配（如 toolFileName 是 "pages/Contact.vue"，匹配 path 包含它的文件）
  target = files.find((f) => f.path.includes(cleanToolFileName))
  if (target) return target
  
  return undefined
}

const expandFoldersForPath = (filePath: string) => {
  const parts = filePath.split('/')
  const foldersToExpand: string[] = []
  let currentPath = ''

  for (let i = 0; i < parts.length - 1; i++) {
    currentPath = currentPath ? `${currentPath}/${parts[i]}` : parts[i]
    foldersToExpand.push(currentPath)
  }

  let hasChanges = false
  for (const folderPath of foldersToExpand) {
    if (!expandedFolders.value.has(folderPath)) {
      expandedFolders.value.add(folderPath)
      hasChanges = true
    }
  }

  if (hasChanges) {
    const updateExpanded = (nodes: TreeNodeType[]) => {
      for (const n of nodes) {
        n.expanded = expandedFolders.value.has(n.key)
        if (n.children.length > 0) {
          updateExpanded(n.children)
        }
      }
    }
    updateExpanded(treeNodes.value)
    treeNodes.value = [...treeNodes.value]
  }
}

watch(
  () => props.files,
  (newFiles) => {
    treeNodes.value = buildTree(newFiles)

    if (props.isGenerating && newFiles.length > 0) {
      if (props.latestToolFileName) {
        const target = findFileByToolName(newFiles, props.latestToolFileName)
        if (target) {
          expandFoldersForPath(target.path)
          selectedFile.value = target
          generatingFilePath.value = target.path
        }
      } else if (props.streamingLanguage) {
        const target = newFiles.find((f) => f.language === props.streamingLanguage)
        if (target) {
          expandFoldersForPath(target.path)
          selectedFile.value = target
          generatingFilePath.value = target.path
        }
      }
    }

    if (newFiles.length > 0 && !selectedFile.value) {
      const firstFile = newFiles[0]
      expandFoldersForPath(firstFile.path)
      selectedFile.value = firstFile
    }

    if (selectedFile.value && newFiles.length > 0) {
      const current = newFiles.find((f) => f.path === selectedFile.value?.path)
      if (current) {
        selectedFile.value = current
      }
    }
  },
  { deep: true, immediate: true },
)

watch(
  () => props.latestToolFileName,
  (newFileName) => {
    if (!newFileName) {
      generatingFilePath.value = ''
      return
    }

    // 文件名变化时尝试跳转
    const trySelectFile = () => {
      const target = findFileByToolName(props.files, newFileName)
      if (target) {
        expandFoldersForPath(target.path)
        selectedFile.value = target
        generatingFilePath.value = target.path
        return true
      }
      return false
    }

    // 先尝试直接跳转
    if (trySelectFile()) return

    // 如果找不到文件，等待文件列表更新后再跳转
    const unwatch = watch(
      () => props.files,
      () => {
        if (trySelectFile()) {
          unwatch()
        }
      },
      { deep: true },
    )
  },
  { immediate: true },
)

watch(
  () => props.streamingLanguage,
  (newLang, oldLang) => {
    if (!newLang || newLang === oldLang) return

    const target = props.files.find((f) => f.language === newLang)
    if (target) {
      expandFoldersForPath(target.path)
      selectedFile.value = target
      generatingFilePath.value = target.path
    }
  },
)

watch(
  () => props.activeFilePath,
  (newPath) => {
    if (newPath) {
      const file = props.files.find((f) => f.path === newPath)
      if (file) {
        selectedFile.value = file
      }
    }
  },
  { immediate: true },
)

const selectFile = (file: CodeFile) => {
  selectedFile.value = file
  emit('select', file)
  if (isEditMode.value) {
    cancelEdit()
  }
}

const handleNodeSelect = (node: TreeNodeType) => {
  if (node.isFolder) {
    toggleFolder(node)
  } else if (node.file) {
    selectFile(node.file)
  }
}

const toggleFolder = (node: TreeNodeType) => {
  if (node.isFolder) {
    if (expandedFolders.value.has(node.key)) {
      expandedFolders.value.delete(node.key)
    } else {
      expandedFolders.value.add(node.key)
    }
    const updateExpanded = (nodes: TreeNodeType[]) => {
      for (const n of nodes) {
        if (n.key === node.key) {
          n.expanded = expandedFolders.value.has(node.key)
        }
        if (n.children.length > 0) {
          updateExpanded(n.children)
        }
      }
    }
    updateExpanded(treeNodes.value)
    treeNodes.value = [...treeNodes.value]
  }
}

const startEdit = () => {
  if (!selectedFile.value) return
  originalContent.value = selectedFile.value.content
  editContent.value = selectedFile.value.content
  isEditMode.value = true
}

const cancelEdit = () => {
  isEditMode.value = false
  editContent.value = ''
  originalContent.value = ''
}

const saveEdit = () => {
  if (!selectedFile.value) return
  emit('save', selectedFile.value, editContent.value)
  isEditMode.value = false
  editContent.value = ''
  originalContent.value = ''
}

const highlightedCode = computed(() => {
  if (!selectedFile.value) return ''
  const code = selectedFile.value.content
  let lang = selectedFile.value.language || 'plaintext'

  if (lang.toLowerCase() === 'vue') {
    lang = 'html'
  }

  if (lang && hljs.getLanguage(lang)) {
    try {
      return hljs.highlight(code, { language: lang }).value
    } catch {
      return escapeHtml(code)
    }
  }
  return escapeHtml(code)
})

const escapeHtml = (text: string): string => {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

const getFileIcon = (file: CodeFile) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  const iconMap: Record<string, Component> = {
    html: Html5Outlined,
    htm: Html5Outlined,
    css: FileTextOutlined,
    js: CodeOutlined,
    ts: CodeOutlined,
    vue: CodeOutlined,
    json: FileTextOutlined,
    md: FileTextOutlined,
    png: FileImageOutlined,
    jpg: FileImageOutlined,
    svg: FileImageOutlined,
  }
  return iconMap[ext || ''] || FileOutlined
}

watch(
  () => selectedFile.value?.content,
  async () => {
    if (props.isGenerating && codeViewerRef.value) {
      await nextTick()
      codeViewerRef.value.scrollTop = codeViewerRef.value.scrollHeight
    }
  },
)

watch(
  () => selectedFile.value,
  async () => {
    if (props.isGenerating && codeViewerRef.value) {
      await nextTick()
      codeViewerRef.value.scrollTop = codeViewerRef.value.scrollHeight
    }
  },
)
</script>

<style scoped>
.code-file-browser {
  height: 100%;
  background: #f5f5f5;
}

.browser-container {
  display: flex;
  height: 100%;
  gap: 1px;
  background: #e8e8e8;
}

.file-tree-panel {
  width: 220px;
  min-width: 180px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e8e8e8;
}

.file-tree-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
}

.file-tree-title {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.file-tree-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.file-tree-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 16px;
  color: #999;
}

.code-panel {
  flex: 1;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.code-panel-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.code-panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
}

.code-panel-header .file-icon {
  font-size: 14px;
  color: #666;
}

.code-panel-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
}

.file-path {
  font-size: 12px;
  color: #666;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
}

.code-viewer {
  flex: 1;
  overflow: auto;
  background: #f8f8f8;
}

.code-viewer pre {
  margin: 0;
  padding: 16px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  tab-size: 2;
}

.code-viewer code {
  font-family: inherit;
}

.editor-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.code-panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.code-panel-empty .empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.code-panel-empty p {
  margin: 0;
  font-size: 14px;
}

.empty-animation {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
}

.empty-loading-text {
  color: #666;
  font-size: 12px;
}

.code-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.code-empty p {
  margin: 0;
  font-size: 14px;
}
</style>
