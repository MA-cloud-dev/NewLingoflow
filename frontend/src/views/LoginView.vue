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
  <div class="min-h-screen flex items-center justify-center bg-slate-50">
    <div class="bg-white p-8 rounded-2xl shadow-xl w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-sky-500 mb-2">LingoFlow</h1>
        <p class="text-slate-500">AI 驱动的沉浸式单词记忆</p>
      </div>
      
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input 
            v-model="form.username" 
            placeholder="请输入用户名"
            size="large"
          />
        </el-form-item>
        
        <el-form-item>
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="请输入密码"
            size="large"
            show-password
          />
        </el-form-item>
        
        <el-button 
          type="primary" 
          class="w-full" 
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
      
      <div class="text-center text-sm text-slate-500 mt-4">
        还没有账号？
        <router-link to="/register" class="text-sky-500 hover:underline">
          立即注册
        </router-link>
      </div>
    </div>
  </div>
</template>
