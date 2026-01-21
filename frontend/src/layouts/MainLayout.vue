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

  <div class="min-h-screen bg-paper flex font-sans text-ink">
    <!-- 侧边栏 Sidebar -->
    <aside class="fixed left-0 top-0 h-full w-72 bg-ink text-[#F9F9F7] flex flex-col z-50 shadow-2xl transition-all duration-300">
      
      <!-- Logo Section -->
      <div class="p-8 pb-4">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-10 h-10 rounded-lg bg-[#F9F9F7] flex items-center justify-center text-ink font-serif font-bold text-xl italic shadow-md">
            L
          </div>
          <div>
            <h1 class="font-serif text-2xl tracking-wide font-bold italic">LingoFlow</h1>
            <p class="text-[10px] text-[#A1A1AA] uppercase tracking-[0.2em] font-medium">Editor's Edition</p>
          </div>
        </div>
      </div>
      
      <!-- Navigation -->
      <nav class="flex-1 px-4 space-y-1">
        <router-link 
          v-for="item in menuItems" 
          :key="item.path"
          :to="item.path" 
          class="group flex items-center gap-4 px-4 py-3 rounded-lg transition-all duration-200"
          :class="[
            route.path === item.path 
              ? 'bg-[#333335] text-white shadow-inner font-medium' 
              : 'text-[#A1A1AA] hover:text-[#F9F9F7] hover:bg-white/5'
          ]"
        >
          <svg class="w-5 h-5 opacity-80 group-hover:opacity-100 transition-opacity" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.icon"/>
          </svg>
          <span class="tracking-wide text-sm">{{ item.name }}</span>
        </router-link>
      </nav>
      
      <!-- User Profile (Bottom) -->
      <div class="p-4 mt-auto border-t border-white/10 m-4 mb-6">
        <div 
        @click="router.push('/profile')"
        class="flex items-center gap-3 p-2 rounded-lg cursor-pointer hover:bg-white/5 transition-colors group"
      >
        <div class="w-10 h-10 rounded-full bg-[#E2E2DF] flex items-center justify-center text-ink font-bold border-2 border-transparent group-hover:border-white/20 transition-all">
          {{ userStore.userInfo?.username?.charAt(0).toUpperCase() }}
        </div>
        <div class="flex-1 min-w-0">
           <div class="flex justify-between items-center">
              <p class="text-sm font-semibold truncate text-[#F9F9F7]">{{ userStore.userInfo?.username }}</p>
              <button @click.stop="handleLogout" class="text-xs text-[#A1A1AA] hover:text-red-400 transition-colors p-1" title="退出">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg>
              </button>
           </div>
           <p class="text-xs text-[#A1A1AA]">Pro Member</p>
        </div>
      </div>
      </div>
    </aside>
    
    <!-- 内容区 Main Content -->
    <main class="flex-1 ml-72 min-h-screen relative bg-paper text-ink transition-all duration-300">
      <div class="p-12 max-w-6xl mx-auto">
        <router-view v-slot="{ Component }">
          <transition 
            name="fade" 
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
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Scrollbar styling for a cleaner look */
::-webkit-scrollbar {
  width: 8px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #E5E5E0;
  border-radius: 4px;
}
::-webkit-scrollbar-thumb:hover {
  background: #D4D4D0;
}
</style>
