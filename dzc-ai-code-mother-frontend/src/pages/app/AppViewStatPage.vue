<template>
  <div class="view-stat-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <a-button type="text" @click="goBack">
        <template #icon><ArrowLeftOutlined /></template>
      </a-button>
      <h1 class="page-title">浏览统计</h1>
      <span class="app-name">{{ appInfo?.appName || '加载中...' }}</span>
    </div>

    <!-- 加载状态 -->
    <a-spin :spinning="loading" tip="加载中...">
      <div class="stat-content">
        <!-- 统计概览卡片 -->
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-icon total">
              <EyeOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(statData?.totalViewCount || 0) }}</div>
              <div class="stat-label">总浏览量</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon today">
              <FireOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(statData?.todayViewCount || 0) }}</div>
              <div class="stat-label">今日浏览</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon yesterday">
              <CalendarOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(statData?.yesterdayViewCount || 0) }}</div>
              <div class="stat-label">昨日浏览</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon avg">
              <LineChartOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(statData?.avgViewCount || 0) }}</div>
              <div class="stat-label">日均浏览</div>
            </div>
          </div>
        </div>

        <!-- 图表区域 -->
        <div class="chart-container">
          <div class="chart-header">
            <h3 class="chart-title">浏览趋势</h3>
            <div class="chart-controls">
              <a-radio-group v-model:value="chartType" button-style="solid" size="small">
                <a-radio-button value="line">折线图</a-radio-button>
                <a-radio-button value="bar">柱状图</a-radio-button>
              </a-radio-group>
            </div>
          </div>
          <div ref="chartRef" class="chart-wrapper"></div>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { getAppViewStat, getAppVoById } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import {
    ArrowLeftOutlined,
    CalendarOutlined,
    EyeOutlined,
    FireOutlined,
    LineChartOutlined
} from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 数据状态
const loading = ref(false)
const appInfo = ref<API.AppVO>()
const statData = ref<API.AppViewStatVO>()
const chartType = ref<'line' | 'bar'>('line')

// 图表相关
const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

/**
 * 格式化数字显示
 * @param num 数字
 * @returns 格式化后的字符串
 */
const formatNumber = (num: number): string => {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return String(num)
}

/**
 * 获取应用信息和浏览统计
 */
const fetchData = async () => {
  const appId = route.params.id as string
  if (!appId || appId === 'undefined' || appId === 'null') {
    console.warn('应用ID不存在')
    initEmptyStatData()
    renderChart()
    loading.value = false
    return
  }

  loading.value = true
  try {
    // 获取应用信息（用于权限校验）
    const appRes = await getAppVoById({ id: Number(appId) })
    if (appRes.data.code === 0 && appRes.data.data) {
      appInfo.value = appRes.data.data
      
      // 权限校验：只有创建者可以查看
      if (appInfo.value.userId !== loginUserStore.loginUser.id) {
        console.warn('无权限查看该应用的浏览统计')
        initEmptyStatData()
        renderChart()
        return
      }
    } else {
      // 应用不存在或获取失败，静默展示0
      console.warn('获取应用信息失败:', appRes.data.message)
      initEmptyStatData()
      renderChart()
      return
    }

    // 获取浏览统计
    const statRes = await getAppViewStat(Number(appId))
    if (statRes.data.code === 0 && statRes.data.data) {
      statData.value = statRes.data.data
      renderChart()
    } else {
      // 没有统计数据，展示0
      initEmptyStatData()
      renderChart()
    }
  } catch (error: any) {
    // 任何错误都静默处理，展示空数据（0）
    console.warn('获取数据失败，展示空状态:', error?.response?.data?.message || error.message)
    initEmptyStatData()
    renderChart()
  } finally {
    loading.value = false
  }
}

/**
 * 初始化空统计数据
 */
const initEmptyStatData = () => {
  statData.value = {
    totalViewCount: 0,
    todayViewCount: 0,
    yesterdayViewCount: 0,
    avgViewCount: 0,
    viewTrend: {}
  }
}

/**
 * 渲染图表
 */
const renderChart = () => {
  if (!chartRef.value || !statData.value?.viewTrend) return

  // 初始化图表
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  // 处理数据
  const trendData = statData.value.viewTrend || {}
  const dates = Object.keys(trendData).sort()
  const values = dates.map(date => trendData[date] || 0)

  // 图表配置
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'rgba(34, 197, 94, 0.3)',
      borderWidth: 1,
      textStyle: {
        color: '#1f2937'
      },
      formatter: (params: any) => {
        const data = params[0]
        return `<div style="padding: 4px 8px;">
          <div style="font-weight: 600; margin-bottom: 4px;">${data.axisValue}</div>
          <div style="color: #22c55e;">浏览量：${data.value}</div>
        </div>`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: chartType.value === 'bar',
      data: dates.map(d => d.substring(5)), // 只显示月-日
      axisLine: {
        lineStyle: {
          color: 'rgba(34, 197, 94, 0.3)'
        }
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#6b7280',
        fontSize: 11
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(34, 197, 94, 0.1)'
        }
      }
    },
    series: [
      {
        name: '浏览量',
        type: chartType.value,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: values,
        itemStyle: {
          color: '#22c55e'
        },
        lineStyle: {
          width: 3,
          color: '#22c55e'
        },
        areaStyle: chartType.value === 'line' ? {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(34, 197, 94, 0.3)' },
            { offset: 1, color: 'rgba(34, 197, 94, 0.05)' }
          ])
        } : undefined,
        barMaxWidth: 30
      }
    ]
  }

  chartInstance.setOption(option)
}

/**
 * 返回上一页
 */
const goBack = () => {
  router.back()
}

/**
 * 窗口大小变化时重新渲染图表
 */
const handleResize = () => {
  chartInstance?.resize()
}

// 监听图表类型变化
watch(chartType, () => {
  renderChart()
})

// 生命周期
onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

<style scoped>
.view-stat-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 50%, #f0fdf4 100%);
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.08);
  backdrop-filter: blur(16px);
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  font-family: 'Lora', serif;
}

.app-name {
  font-size: 14px;
  color: #6b7280;
  margin-left: auto;
}

.stat-content {
  max-width: 1200px;
  margin: 0 auto;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.08);
  backdrop-filter: blur(16px);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(21, 128, 61, 0.15);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.total {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2), rgba(34, 197, 94, 0.1));
  color: #22c55e;
}

.stat-icon.today {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.2), rgba(251, 146, 60, 0.1));
  color: #fb923c;
}

.stat-icon.yesterday {
  background: linear-gradient(135deg, rgba(96, 165, 250, 0.2), rgba(96, 165, 250, 0.1));
  color: #60a5fa;
}

.stat-icon.avg {
  background: linear-gradient(135deg, rgba(167, 139, 250, 0.2), rgba(167, 139, 250, 0.1));
  color: #a78bfa;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  font-family: 'Lora', serif;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  margin-top: 4px;
}

.chart-container {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(21, 128, 61, 0.08);
  backdrop-filter: blur(16px);
  padding: 24px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.chart-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.chart-wrapper {
  width: 100%;
  height: 400px;
}

@media (max-width: 768px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .chart-wrapper {
    height: 300px;
  }
}
</style>
