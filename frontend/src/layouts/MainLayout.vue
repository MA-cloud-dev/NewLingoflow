<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

async function handleLogout() {
  try {
    await logout()
  } finally {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

const menuItems = [
  { name: '首页', path: '/', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
  { name: '开始学习', path: '/learn', icon: 'M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253' },
  { name: '每日复习', path: '/review', icon: 'M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15' },
  { name: '我的生词本', path: '/vocabulary', icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z' }
]
</script>

<template>
  <div class="min-h-screen bg-[#FDFDFD] flex">
    <!-- 侧边栏 -->
    <aside class="fixed left-0 top-0 h-full w-72 bg-[#0F172A] text-white shadow-2xl z-50 overflow-hidden">
      <!-- 装饰背景 -->
      <div class="absolute inset-0 opacity-20 pointer-events-none">
        <img src="/sidebar-bg.png" class="w-full h-full object-cover" alt="" />
      </div>
      
      <div class="relative h-full flex flex-col p-6">
        <div class="mb-10 flex items-center gap-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/30">
            <svg class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
            </svg>
          </div>
          <div>
            <h1 class="text-xl font-bold tracking-tight">LingoFlow</h1>
            <p class="text-[10px] text-blue-400 uppercase tracking-widest font-semibold">Premium Edition</p>
          </div>
        </div>
        
        <nav class="flex-1 space-y-2">
          <router-link 
            v-for="item in menuItems" 
            :key="item.path"
            :to="item.path" 
            class="group flex items-center gap-4 px-4 py-3.5 rounded-xl transition-all duration-300 relative overflow-hidden"
            :class="[
              route.path === item.path 
                ? 'bg-blue-600/10 text-blue-400 shadow-[inset_0_0_0_1px_rgba(59,130,246,0.2)]' 
                : 'text-slate-400 hover:text-white hover:bg-white/5'
            ]"
          >
            <!-- 激活状态指示器 -->
            <div v-if="route.path === item.path" class="absolute left-0 top-1/4 bottom-1/4 w-1 bg-blue-500 rounded-r-full"></div>
            
            <svg class="w-5 h-5 transition-transform duration-300 group-hover:scale-110" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.icon"/>
            </svg>
            <span class="font-medium tracking-wide">{{ item.name }}</span>
          </router-link>
        </nav>
        
        <div class="mt-auto pt-6 border-t border-white/5">
          <div class="flex items-center gap-3 px-4 py-3 mb-4 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10">
            <div class="w-10 h-10 rounded-full bg-gradient-to-br from-slate-700 to-slate-800 flex items-center justify-center text-sm font-bold border border-white/10">
              {{ userStore.userInfo?.username?.charAt(0).toUpperCase() }}
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm font-semibold truncate">{{ userStore.userInfo?.username }}</p>
              <p class="text-[10px] text-slate-500 uppercase tracking-tighter">Verified Learner</p>
            </div>
          </div>
          <button 
            @click="handleLogout"
            class="flex items-center gap-3 px-4 py-3 rounded-xl text-slate-500 hover:text-red-400 hover:bg-red-400/10 w-full transition-all duration-300 group"
          >
            <svg class="w-5 h-5 transition-transform group-hover:-translate-x-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
            </svg>
            <span class="font-medium">退出登录</span>
          </button>
        </div>
      </div>
    </aside>
    
    <!-- 内容区 -->
    <main class="flex-1 ml-72 min-h-screen relative">
      <!-- 页面顶部模糊背景装饰 -->
      <div class="absolute top-0 right-0 w-[500px] h-[500px] bg-blue-500/5 rounded-full blur-[100px] -z-10"></div>
      <div class="absolute bottom-0 left-0 w-[300px] h-[300px] bg-indigo-500/5 rounded-full blur-[80px] -z-10"></div>
      
      <div class="p-10 max-w-7xl mx-auto">
        <router-view v-slot="{ Component }">
          <transition 
            name="fade-slide" 
            mode="out-in"
          >
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<style scoped>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #E2E8F0;
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: #CBD5E1;
}
</style>
