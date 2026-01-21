<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, TitleComponent, LegendComponent } from 'echarts/components'
import type { WeeklyData } from '@/api/stats'

use([CanvasRenderer, BarChart, GridComponent, TooltipComponent, TitleComponent, LegendComponent])

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
      axisPointer: { type: 'shadow' },
      formatter: function (params: any[]) {
        let result = `<div style="font-weight:600;margin-bottom:4px">${params[0].axisValue}</div>`
        params.forEach((item: any) => {
          result += `<div style="display:flex;align-items:center;gap:6px">
            <span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:${item.color}"></span>
            <span>${item.seriesName}: ${item.value} 词</span>
          </div>`
        })
        return result
      }
    },
    legend: {
      data: ['学习', '复习'],
      top: 30,
      textStyle: { color: '#64748B', fontWeight: 500 },
      itemStyle: { borderWidth: 0 }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 70,
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
        name: '学习',
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
          itemStyle: { color: '#1D4ED8' }
        },
        barWidth: '30%'
      },
      {
        name: '复习',
        type: 'bar',
        data: props.data.map(d => d.reviewCount || 0),
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: '#FB923C' },
              { offset: 1, color: '#EA580C' }
            ]
          }
        },
        emphasis: {
          itemStyle: { color: '#C2410C' }
        },
        barWidth: '30%'
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
