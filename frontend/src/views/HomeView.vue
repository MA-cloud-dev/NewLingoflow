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
  <div class="space-y-12 pb-10">
    <!-- Header Section -->
    <header class="flex flex-col md:flex-row justify-between items-end border-b border-[#E5E5E0] pb-8">
      <div>
        <div class="flex items-center gap-3 mb-2">
           <span class="px-2 py-0.5 border border-ink/20 text-ink/60 text-[10px] font-bold uppercase tracking-widest">
             {{ new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' }) }}
           </span>
           <span class="w-1.5 h-1.5 rounded-full bg-green-500"></span>
           <span class="text-[10px] uppercase tracking-wider text-green-600 font-bold">Online</span>
        </div>
        <h2 class="text-4xl md:text-5xl font-serif font-bold text-ink leading-tight italic">Morning Briefing</h2>
        <p class="text-ink/60 mt-2 font-light text-lg">Welcome back, {{ userStore.userInfo?.username }}. Ready to expand your lexicon?</p>
      </div>
      
      <div class="hidden md:block">
        <div class="text-right">
             <div class="text-3xl font-serif font-bold text-ink">{{ stats.streakDays }} <span class="text-sm font-sans font-normal text-ink/40 uppercase tracking-widest">Days</span></div>
             <p class="text-xs text-ink/40 uppercase tracking-widest">Current Streak</p>
        </div>
      </div>
    </header>
    
    <!-- Hero / Featured Action -->
    <section>
       <div class="paper-card p-0 flex flex-col md:flex-row overflow-hidden group cursor-pointer transition-all hover:shadow-lg">
           <div class="w-full md:w-2/5 h-64 md:h-auto relative overflow-hidden">
               <img src="/hero-morning.png" alt="Morning Focus" class="absolute inset-0 w-full h-full object-cover transition-transform duration-700 group-hover:scale-105 filter grayscale-[20%] group-hover:grayscale-0">
           </div>
           <div class="w-full md:w-3/5 p-8 md:p-12 flex flex-col justify-center">
               <div class="flex items-center gap-2 mb-4">
                  <span class="text-xs font-bold uppercase tracking-widest text-[#D4B483]">Focus of the Day</span>
               </div>
               <h3 class="text-3xl font-serif text-ink mb-4 group-hover:underline underline-offset-4 decoration-1 decoration-[#D4B483]">The Art of Conversation</h3>
               <p class="text-ink/70 mb-8 leading-relaxed font-light text-lg">
                   Immerse yourself in today's AI-curated article designed to improve your conversational 
                   flow and introduce 15 new idiomatic expressions.
               </p>
               <div class="flex gap-4">
                   <router-link to="/learn" class="px-6 py-3 bg-ink text-white text-sm font-medium hover:bg-ink/90 transition-colors flex items-center gap-2">
                       Start Reading <svg class="w-4 h-4 text-[#D4B483]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
                   </router-link>
               </div>
           </div>
       </div>
    </section>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
        <!-- Review Card -->
        <div class="paper-card p-6 flex flex-col justify-between border-t-4 border-t-[#D4B483]">
            <div>
                <div class="flex justify-between items-start mb-2">
                    <span class="text-xs font-bold uppercase tracking-widest text-ink/40">Pending Review</span>
                    <svg class="w-5 h-5 text-[#D4B483]" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>
                </div>
                <div class="text-4xl font-serif text-ink mb-1">{{ stats.pendingReview }}</div>
                <p class="text-sm text-ink/60">Words need your attention.</p>
            </div>
            <router-link to="/review" class="mt-4 text-sm font-bold border-b border-ink/10 pb-1 hover:border-ink transition-colors w-fit">Review Now</router-link>
        </div>

        <!-- Learned Card -->
        <div class="paper-card p-6 flex flex-col justify-between border-t-4 border-t-ink/20">
             <div>
                <div class="flex justify-between items-start mb-2">
                    <span class="text-xs font-bold uppercase tracking-widest text-ink/40">Total Learned</span>
                    <svg class="w-5 h-5 text-ink/20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>
                </div>
                <div class="text-4xl font-serif text-ink mb-1">{{ stats.totalWords }}</div>
                <p class="text-sm text-ink/60">Lexicon size.</p>
            </div>
        </div>
        
        <!-- Daily Goal -->
         <div class="paper-card p-6 flex flex-col justify-between border-t-4 border-t-ink/20">
             <div>
                <div class="flex justify-between items-start mb-2">
                    <span class="text-xs font-bold uppercase tracking-widest text-ink/40">Today's Goal</span>
                    <svg class="w-5 h-5 text-ink/20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                </div>
                <div class="flex items-baseline gap-2">
                     <div class="text-4xl font-serif text-ink mb-1">{{ stats.todayLearned }}</div>
                     <span class="text-sm text-ink/40">/ {{ stats.dailyGoal }}</span>
                </div>
                <div class="w-full bg-ink/5 h-1 mt-2 mb-1">
                    <div class="bg-[#D4B483] h-1 transition-all duration-500" :style="{ width: Math.min((stats.todayLearned / stats.dailyGoal) * 100, 100) + '%' }"></div>
                </div>
            </div>
        </div>

        <!-- Placeholder for Weekly (Coming soon visually) -->
         <div class="paper-card p-6 flex items-center justify-center border border-dashed border-ink/20 bg-transparent hover:bg-white transition-colors cursor-pointer">
            <div class="text-center">
                <div class="w-10 h-10 rounded-full bg-ink/5 flex items-center justify-center mx-auto mb-3">
                    <svg class="w-5 h-5 text-ink/40" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
                </div>
                <span class="text-sm font-bold text-ink/60">View Analytics</span>
            </div>
        </div>
    </div>
    
    <!-- Weekly Chart Section -->
    <section class="mt-8">
         <h4 class="text-lg font-serif italic text-ink mb-6 border-b border-[#E5E5E0] pb-2 inline-block pr-8">Weekly Performance</h4>
         <div class="paper-card p-8">
             <WeeklyChart :data="weeklyData" />
         </div>
    </section>

  </div>
</template>

<style scoped>
/* Override any specific chart styles if needed, but WeeklyChart usually handles its own */
</style>

