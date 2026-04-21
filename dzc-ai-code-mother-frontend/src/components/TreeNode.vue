<template>
  <div>
    <div
      class="file-tree-item"
      :class="{
        active: node.file && selectedPath === node.file.path,
        generating: node.file && generatingPath === node.file.path,
        'folder-item': node.isFolder,
      }"
      :style="{ paddingLeft: `${(depth ?? 0) * 12 + 16}px` }"
      @click="$emit('select', node)"
    >
      <div class="file-tree-item-left">
      <FolderOutlined v-if="node.isFolder" class="file-icon folder-icon" :class="{ expanded: node.expanded }" />
      <component :is="getFileIcon(node.file!)" v-else class="file-icon" />
      <span class="file-name">{{ node.name }}</span>
    </div>
    <div v-if="node.file && generatingPath === node.file.path" class="file-generating-indicator">
      <a-spin size="small" />
    </div>
    </div>
    <div v-if="node.isFolder && node.expanded" class="folder-children">
      <TreeNode
        v-for="child in node.children"
        :key="child.key"
        :node="child"
        :selected-path="selectedPath"
        :generating-path="generatingPath"
        :depth="(depth ?? 0) + 1"
        @select="$emit('select', $event)"
        @toggle="$emit('toggle', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { CodeOutlined, FileImageOutlined, FileOutlined, FileTextOutlined, FolderOutlined, Html5Outlined } from '@ant-design/icons-vue'
import { type Component } from 'vue'

interface CodeFile {
  name: string
  path: string
  language: string
  content: string
}

interface TreeNode {
  key: string
  name: string
  isFolder: boolean
  children: TreeNode[]
  file?: CodeFile
  expanded: boolean
}

defineProps<{
  node: TreeNode
  selectedPath: string
  generatingPath: string
  depth?: number
}>()

defineEmits<{
  (e: 'select', node: TreeNode): void
  (e: 'toggle', node: TreeNode): void
}>()

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
</script>

<style scoped>
.file-tree-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 2px solid transparent;
  position: relative;
}

.file-tree-item:hover {
  background: #f0f7ff;
}

.file-tree-item.active {
  background: #e6f4ff;
  border-left-color: #1890ff;
}

.file-tree-item.generating {
  animation: generating-pulse 2s ease-in-out infinite;
  border-left-color: #52c41a;
  background: linear-gradient(90deg, rgba(82, 196, 26, 0.05) 0%, rgba(255, 255, 255, 0) 100%);
}

.file-tree-item.generating .file-name {
  color: #52c41a;
  font-weight: 500;
}

.file-tree-item.folder-item {
  font-weight: 500;
}

@keyframes generating-pulse {
  0%, 100% {
    background: rgba(255, 255, 255, 0);
    box-shadow: none;
  }
  50% {
    background: rgba(82, 196, 26, 0.08);
    box-shadow: 0 0 8px rgba(82, 196, 26, 0.2);
  }
}

.file-tree-item-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.file-generating-indicator {
  flex-shrink: 0;
}

.file-icon {
  font-size: 14px;
  color: #666;
  flex-shrink: 0;
}

.file-icon.folder-icon {
  color: #faad14;
}

.file-icon.folder-icon.expanded {
  transform: rotate(90deg);
  transition: transform 0.2s ease;
}

.file-tree-item.active .file-icon {
  color: #1890ff;
}

.file-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-children {
  margin-left: 0;
}
</style>
