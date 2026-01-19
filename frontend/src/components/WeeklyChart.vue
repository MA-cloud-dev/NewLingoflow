<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, TitleComponent } from 'echarts/components'
import type { WeeklyData } from '@/api/stats'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, TitleComponent])

const props = defineProps<{
  data: WeeklyData[]
}>()

const option = ref({})

function updateChart() {
  option.value = {
    title: {
      text: '本周学习曲线',
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 600,
        color: '#1E293B'
      }
    },
    tooltip: {
      trigger: 'axis',
      formatter: '{b}: {c} 词'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: props.data.map(d => d.date),
      axisLine: { lineStyle: { color: '#E2E8F0' } },
      axisLabel: { color: '#64748B' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#F1F5F9' } },
      axisLabel: { color: '#64748B' }
    },
    series: [
      {
        type: 'bar',
        data: props.data.map(d => d.count),
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#3B82F6' },
              { offset: 1, color: '#2563EB' }
            ]
          }
        },
        emphasis: {
          itemStyle: {
            color: '#1D4ED8'
          }
        },
        barWidth: '60%'
      }
    ]
  }
}

watch(() => props.data, updateChart, { immediate: true })

onMounted(updateChart)
</script>

<template>
  <div class="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
    <v-chart :option="option" style="height: 280px" autoresize />
  </div>
</template>
