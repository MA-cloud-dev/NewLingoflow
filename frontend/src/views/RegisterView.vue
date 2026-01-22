<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

async function handleRegister() {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写必填项')
    return
  }
  
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  
  loading.value = true
  try {
    const res = await register(form)
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.msg)
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-paper p-4">
    <div class="paper-card w-full max-w-md p-10 md:p-12 text-center">
      <div class="mb-10">
        <div class="text-xs font-bold text-ink/40 uppercase tracking-widest mb-2">加入 LingoFlow</div>
        <h1 class="text-3xl font-serif font-black text-ink italic tracking-tight">创建你的账户</h1>
      </div>
      
      <el-form :model="form" @submit.prevent="handleRegister" class="space-y-6 text-left">
        <div class="space-y-4">
          <div class="space-y-1">
            <label class="text-xs font-bold text-ink/40 uppercase tracking-widest pl-1">用户名</label>
            <input 
              v-model="form.username" 
              type="text" 
              class="w-full px-4 py-3 bg-[#F9F9F7] border border-ink/10 rounded-lg text-ink font-medium focus:outline-none focus:border-ink transition-colors placeholder:text-ink/20"
              placeholder="设置用户名"
            />
          </div>
          
          <div class="space-y-1">
             <label class="text-xs font-bold text-ink/40 uppercase tracking-widest pl-1">邮箱（可选）</label>
            <input 
              v-model="form.email" 
              type="email" 
              class="w-full px-4 py-3 bg-[#F9F9F7] border border-ink/10 rounded-lg text-ink font-medium focus:outline-none focus:border-ink transition-colors placeholder:text-ink/20"
              placeholder="name@example.com"
            />
          </div>
          
          <div class="grid grid-cols-2 gap-4">
             <div class="space-y-1">
               <label class="text-xs font-bold text-ink/40 uppercase tracking-widest pl-1">密码</label>
              <input 
                v-model="form.password" 
                type="password" 
                class="w-full px-4 py-3 bg-[#F9F9F7] border border-ink/10 rounded-lg text-ink font-medium focus:outline-none focus:border-ink transition-colors placeholder:text-ink/20"
                placeholder="******"
              />
            </div>
             <div class="space-y-1">
               <label class="text-xs font-bold text-ink/40 uppercase tracking-widest pl-1">确认密码</label>
              <input 
                v-model="form.confirmPassword" 
                type="password" 
                class="w-full px-4 py-3 bg-[#F9F9F7] border border-ink/10 rounded-lg text-ink font-medium focus:outline-none focus:border-ink transition-colors placeholder:text-ink/20"
                placeholder="******"
              />
            </div>
          </div>
        </div>
        
        <button 
          @click="handleRegister"
          :disabled="loading"
          class="w-full py-4 bg-ink text-white font-bold uppercase tracking-widest text-sm rounded-lg hover:bg-ink/90 transition-all shadow-lg flex items-center justify-center gap-2 group disabled:opacity-70 disabled:cursor-not-allowed"
        >
          <span v-if="loading" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
          <span v-else>注册</span>
          <svg v-if="!loading" class="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
        </button>
      </el-form>
      
      <div class="mt-8 pt-8 border-t border-ink/5 text-sm text-ink/60 font-sans">
        已有账号？
         <router-link to="/login" class="text-ink font-bold hover:underline ml-1">
          立即登录
        </router-link>
      </div>
    </div>
  </div>
</template>
