<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  
  loading.value = true
  try {
    const res = await login(form)
    if (res.code === 200) {
      userStore.setLoginData(res.data)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.msg)
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-paper p-4">
    <div class="paper-card w-full max-w-md p-10 md:p-12 text-center">
      <div class="mb-10">
        <div class="w-16 h-16 bg-ink text-white rounded-xl flex items-center justify-center text-3xl font-serif italic font-bold mx-auto mb-6 shadow-lg">
          L
        </div>
        <h1 class="text-4xl font-serif font-black text-ink italic mb-2 tracking-tight">LingoFlow</h1>
        <p class="text-ink/50 font-sans text-sm tracking-wide">The Editor's Choice for Learning</p>
      </div>
      
      <el-form :model="form" @submit.prevent="handleLogin" class="space-y-6 text-left">
        <div class="space-y-4">
          <div class="space-y-1">
            <label class="text-xs font-bold text-ink/40 uppercase tracking-widest pl-1">Username</label>
            <input 
              v-model="form.username" 
              type="text" 
              class="w-full px-4 py-3 bg-[#F9F9F7] border border-ink/10 rounded-lg text-ink font-medium focus:outline-none focus:border-ink transition-colors placeholder:text-ink/20"
              placeholder="Enter your username"
            />
          </div>
          
          <div class="space-y-1">
             <label class="text-xs font-bold text-ink/40 uppercase tracking-widest pl-1">Password</label>
            <input 
              v-model="form.password" 
              type="password" 
              class="w-full px-4 py-3 bg-[#F9F9F7] border border-ink/10 rounded-lg text-ink font-medium focus:outline-none focus:border-ink transition-colors placeholder:text-ink/20"
              placeholder="Enter your password"
            />
          </div>
        </div>
        
        <button 
          @click="handleLogin"
          :disabled="loading"
          class="w-full py-4 bg-ink text-white font-bold uppercase tracking-widest text-sm rounded-lg hover:bg-ink/90 transition-all shadow-lg flex items-center justify-center gap-2 group disabled:opacity-70 disabled:cursor-not-allowed"
        >
          <span v-if="loading" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
          <span v-else>Sign In</span>
          <svg v-if="!loading" class="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
        </button>
      </el-form>
      
      <div class="mt-8 pt-8 border-t border-ink/5 text-sm text-ink/60 font-sans">
        New to LingoFlow?
        <router-link to="/register" class="text-ink font-bold hover:underline ml-1">
          Create an account
        </router-link>
      </div>
    </div>
  </div>
</template>
