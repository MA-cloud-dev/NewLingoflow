<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getStatsOverview, getWeeklyStats, type StatsOverview, type WeeklyData } from '@/api/stats'
import WeeklyChart from '@/components/WeeklyChart.vue'

const userStore = useUserStore()

const stats = ref<StatsOverview>({
  todayLearned: 0,
  pendingReview: 0,
  totalWords: 0,
  streakDays: 0,
  dailyGoal: 20
})
const weeklyData = ref<WeeklyData[]>([])
const loading = ref(true)

async function loadStats() {
  loading.value = true
  try {
    const [overviewRes, weeklyRes] = await Promise.all([
      getStatsOverview(),
      getWeeklyStats()
    ])
    if (overviewRes.code === 200) stats.value = overviewRes.data
    if (weeklyRes.code === 200) weeklyData.value = weeklyRes.data
  } catch (error) {
    console.error('加载统计失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="space-y-10 pb-10">
    <!-- 顶部欢迎区 -->
    <header class="relative rounded-3xl overflow-hidden bg-slate-900 h-64 shadow-2xl">
      <img src="/learning-header.png" class="absolute inset-0 w-full h-full object-cover opacity-60" alt="" />
      <div class="absolute inset-0 bg-gradient-to-r from-slate-900 via-slate-900/40 to-transparent"></div>
      <div class="relative h-full flex flex-col justify-center px-12">
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/20 border border-blue-500/30 backdrop-blur-md mb-4 w-fit">
          <span class="relative flex h-2 w-2">
            <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
            <span class="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
          </span>
          <span class="text-[10px] font-bold text-blue-300 uppercase tracking-widest">System Active</span>
        </div>
        <h2 class="text-4xl font-black text-white mb-2 tracking-tight">
          欢迎回来, <span class="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-300">{{ userStore.userInfo?.username }}</span>
        </h2>
        <p class="text-slate-300 text-lg max-w-md font-medium">今天也要继续挑战自我，探索语言的无限可能 💪</p>
      </div>
    </header>
    
    <!-- 核心统计数据 -->
    <section>
      <div class="flex items-center justify-between mb-6">
        <h3 class="text-xl font-bold text-slate-800 flex items-center gap-2">
          <span class="w-1.5 h-6 bg-blue-600 rounded-full"></span>
          学习概览
        </h3>
        <div class="text-sm font-medium text-slate-400">更新于: {{ new Date().toLocaleTimeString() }}</div>
      </div>
      
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
        <!-- 学习统计卡片 -->
        <div v-for="(card, index) in [
          { title: '今日学习', value: stats.todayLearned, unit: '词', color: 'blue', icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' },
          { title: '待复习', value: stats.pendingReview, unit: '词', color: 'orange', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
          { title: '连续学习', value: stats.streakDays, unit: '天', color: 'emerald', icon: 'M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z' },
          { title: '总词汇量', value: stats.totalWords, unit: '词', color: 'indigo', icon: 'M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10' }
        ]" :key="index" class="group relative bg-white p-6 rounded-[2rem] border border-slate-100 shadow-sm transition-all duration-300 hover:shadow-xl hover:shadow-slate-200/50 hover:-translate-y-1">
          <div class="flex items-center justify-between mb-4">
            <div :class="`w-12 h-12 rounded-2xl bg-${card.color}-50 flex items-center justify-center transition-colors group-hover:bg-${card.color}-100`">
              <svg :class="`w-6 h-6 text-${card.color}-600`" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="card.icon"/>
              </svg>
            </div>
            <div v-if="card.title === '今日学习'" class="text-[10px] font-bold text-slate-300 uppercase tracking-widest">Goal: {{ stats.dailyGoal }}</div>
          </div>
          <div class="flex items-baseline gap-1">
            <div class="text-3xl font-black text-slate-800 tracking-tight">{{ card.value }}</div>
            <div class="text-sm font-semibold text-slate-400">{{ card.unit }}</div>
          </div>
          <div class="text-sm font-bold text-slate-500 mt-1 uppercase tracking-wider text-[11px]">{{ card.title }}</div>
        </div>
      </div>
    </section>
    
    <!-- 图表与快速入口 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <div class="lg:col-span-2">
        <WeeklyChart :data="weeklyData" class="!bg-white !rounded-[2.5rem] !shadow-sm !p-8 !border-slate-100" />
      </div>
      
      <div class="space-y-6">
        <h3 class="text-xl font-bold text-slate-800 flex items-center gap-2 px-2">
          <span class="w-1.5 h-6 bg-indigo-600 rounded-full"></span>
          快速访问
        </h3>
        
        <router-link to="/learn" class="block group">
          <div class="relative overflow-hidden bg-[#2563EB] p-6 rounded-[2rem] text-white shadow-lg shadow-blue-500/20 transition-all duration-300 group-hover:shadow-2xl group-hover:shadow-blue-500/30 group-hover:-translate-y-1">
            <div class="absolute -right-4 -top-4 w-24 h-24 bg-white/10 rounded-full blur-2xl group-hover:bg-white/20 transition-colors"></div>
            <div class="relative flex items-center gap-5">
              <div class="w-14 h-14 bg-white/10 backdrop-blur-md rounded-2xl flex items-center justify-center border border-white/20">
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M13 10V3L4 14h7v7l9-11h-7z"/>
                </svg>
              </div>
              <div>
                <h4 class="text-xl font-black tracking-tight">开启新旅程</h4>
                <p class="text-blue-100 text-sm font-medium">AI 沉浸式文章生成</p>
              </div>
            </div>
          </div>
        </router-link>
        
        <router-link to="/review" class="block group">
          <div class="relative overflow-hidden bg-slate-800 p-6 rounded-[2rem] text-white shadow-lg transition-all duration-300 group-hover:shadow-2xl group-hover:-translate-y-1">
            <div class="absolute -right-4 -top-4 w-24 h-24 bg-white/5 rounded-full blur-2xl group-hover:bg-white/10 transition-colors"></div>
            <div class="relative flex items-center gap-5">
              <div class="w-14 h-14 bg-white/5 backdrop-blur-md rounded-2xl flex items-center justify-center border border-white/10">
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                </svg>
              </div>
              <div>
                <h4 class="text-xl font-black tracking-tight">巩固知识点</h4>
                <p class="text-slate-400 text-sm font-medium">{{ stats.pendingReview }} 个单词待复习</p>
              </div>
            </div>
          </div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 确保某些 Tailwind 类名能正确工作 */
.bg-blue-50 { background-color: rgb(239 246 255); }
.bg-orange-50 { background-color: rgb(255 247 237); }
.bg-emerald-50 { background-color: rgb(236 253 245); }
.bg-indigo-50 { background-color: rgb(238 242 255); }

.bg-blue-100 { background-color: rgb(219 234 254); }
.bg-orange-100 { background-color: rgb(255 237 213); }
.bg-emerald-100 { background-color: rgb(209 250 229); }
.bg-indigo-100 { background-color: rgb(224 231 255); }

.text-blue-600 { color: rgb(37 99 235); }
.text-orange-600 { color: rgb(234 88 12); }
.text-emerald-600 { color: rgb(5 150 105); }
.text-indigo-600 { color: rgb(79 70 229); }
</style>

