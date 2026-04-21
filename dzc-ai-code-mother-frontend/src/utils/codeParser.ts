const FILE_NAME_REGEX = /^[\s]*\/\/[\s]*文件名[:：]\s*(.+)$/m

export interface CodeFile {
  name: string
  path: string
  language: string
  content: string
}

export interface ParsedCodeBlock {
  language: string
  content: string
  fileName?: string
}

function stripFileNameComment(rawContent: string): string {
  const lines = rawContent.split('\n')
  const filtered = lines.filter((line) => !line.match(FILE_NAME_REGEX))
  return filtered.join('\n').trimStart()
}

export function getStreamingLanguage(content: string): string | null {
  const lastBacktick = content.lastIndexOf('```')
  if (lastBacktick === -1) return null

  const remainingContent = content.substring(lastBacktick + 3)
  const langMatch = remainingContent.match(/^(\w+)\s*\n/)
  if (langMatch && langMatch[1]) {
    const hasClosingBacktick = remainingContent.includes('```')
    if (!hasClosingBacktick) {
      return langMatch[1]
    }
  }
  return null
}

export function parseCodeBlocks(content: string): ParsedCodeBlock[] {
  const blocks: ParsedCodeBlock[] = []

  const completeRegex = /```(\w+)\s*\n([\s\S]*?)```/g
  let match
  while ((match = completeRegex.exec(content)) !== null) {
    const language = match[1]
    const rawContent = match[2] || ''
    const fileNameMatch = rawContent.match(FILE_NAME_REGEX)
    const fileName = fileNameMatch ? fileNameMatch[1].trim() : undefined
    const cleanContent = stripFileNameComment(rawContent)

    blocks.push({ language, content: cleanContent, fileName })
  }

  const streamingLang = getStreamingLanguage(content)
  if (streamingLang) {
    const lastBacktick = content.lastIndexOf('```')
    const rawContent = content.substring(lastBacktick + 3).replace(/^\w+\s*\n/, '')
    const cleanContent = stripFileNameComment(rawContent)

    if (cleanContent.length > 0) {
      blocks.push({ language: streamingLang, content: cleanContent, fileName: undefined })
    }
  }

  return blocks
}

function guessFileName(language: string, blocks: ParsedCodeBlock[], currentIndex: number): string {
  const langLower = language.toLowerCase()
  
  // 计算当前语言在所有块中的索引
  let langIndex = 0
  for (let i = 0; i < currentIndex; i++) {
    if (blocks[i]?.language.toLowerCase() === langLower) {
      langIndex++
    }
  }
  
  if (langLower === 'html') return langIndex === 0 ? 'index.html' : `page${langIndex + 1}.html`
  if (langLower === 'css') return langIndex === 0 ? 'style.css' : `style${langIndex + 1}.css`
  if (langLower === 'js' || langLower === 'javascript') return langIndex === 0 ? 'app.js' : `script${langIndex + 1}.js`
  if (langLower === 'ts' || langLower === 'typescript') return langIndex === 0 ? 'app.ts' : `script${langIndex + 1}.ts`
  if (langLower === 'vue') return langIndex === 0 ? 'App.vue' : `Component${langIndex + 1}.vue`
  if (langLower === 'json') return 'package.json'
  
  return `file${langIndex + 1}.${language || 'txt'}`
}

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

function matchFileNameForBlock(
  lang: string,
  blockFileName: string | undefined,
  allToolFiles: string[],
  usedToolFiles: Set<string>,
  blocks: ParsedCodeBlock[],
  currentIndex: number
): string {
  if (blockFileName) return blockFileName

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

  return guessFileName(lang, blocks, currentIndex)
}

export function extractCodeFiles(content: string): CodeFile[] {
  const blocks = parseCodeBlocks(content)
  if (blocks.length === 0) return []

  const allToolFiles = extractAllToolFiles(content)
  const usedToolFiles = new Set<string>()
  const assignedNames = new Set<string>()

  return blocks.map((block, index) => {
    const fileName = matchFileNameForBlock(
      block.language,
      block.fileName,
      allToolFiles,
      usedToolFiles,
      blocks,
      index
    )

    // 如果文件名包含路径（如 src/App.vue），保留路径
    const hasPath = fileName.includes('/')
    let uniqueName = fileName
    let counter = 1

    // 对于有路径的文件，只检查完整路径是否重复
    // 对于没有路径的文件，检查文件名是否重复
    if (hasPath) {
      if (assignedNames.has(uniqueName)) {
        const parts = fileName.split('/')
        const baseName = parts.slice(0, -1).join('/')
        const lastPart = parts[parts.length - 1]
        const nameParts = lastPart.split('.')
        const ext = nameParts.pop()
        const baseFileName = nameParts.join('.')
        uniqueName = `${baseName}/${baseFileName}-${counter}.${ext}`
        while (assignedNames.has(uniqueName)) {
          counter++
          uniqueName = `${baseName}/${baseFileName}-${counter}.${ext}`
        }
      }
    } else {
      while (assignedNames.has(uniqueName)) {
        const parts = fileName.split('.')
        const ext = parts.pop()
        const baseName = parts.join('.')
        uniqueName = `${baseName}-${counter}.${ext}`
        counter++
      }
    }
    assignedNames.add(uniqueName)

    return {
      name: hasPath ? fileName.split('/').pop() || fileName : uniqueName,
      path: uniqueName,
      language: block.language,
      content: block.content,
    }
  })
}

export function hasCodeBlocks(content: string): boolean {
  const completeRegex = /```(\w+)\s*\n([\s\S]*?)```/g
  return completeRegex.test(content) || !!getStreamingLanguage(content)
}

export function replaceCodeBlocksWithCards(content: string): string {
  const codeBlocks = parseCodeBlocks(content)

  if (codeBlocks.length === 0) {
    return content
  }

  let result = content
  let index = 0

  const regex = /```(\w+)\s*\n([\s\S]*?)```/g

  result = result.replace(regex, (match, language) => {
    const block = codeBlocks[index]
    index++

    if (!block) return match

    const fileName = block.fileName || guessFileName(language || '', codeBlocks, index - 1)
    const langLabel = language || 'code'

    return `<div class="code-block-card" data-file-index="${index - 1}" data-file-name="${fileName}" data-language="${langLabel}">
      <div class="code-block-card-header">
        <span class="code-block-card-file">${fileName}</span>
        <span class="code-block-card-lang">${langLabel}</span>
      </div>
      <div class="code-block-card-body">
        <p>代码已移至右侧「代码」面板查看</p>
      </div>
    </div>`
  })

  return result
}
