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
  <div class="min-h-screen flex items-center justify-center bg-slate-50">
    <div class="bg-white p-8 rounded-2xl shadow-xl w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-sky-500 mb-2">创建账号</h1>
        <p class="text-slate-500">开始你的单词记忆之旅</p>
      </div>
      
      <el-form :model="form" @submit.prevent="handleRegister">
        <el-form-item>
          <el-input 
            v-model="form.username" 
            placeholder="请输入用户名"
            size="large"
          />
        </el-form-item>
        
        <el-form-item>
          <el-input 
            v-model="form.email" 
            placeholder="请输入邮箱（可选）"
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
        
        <el-form-item>
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="请确认密码"
            size="large"
            show-password
          />
        </el-form-item>
        
        <el-button 
          type="primary" 
          class="w-full" 
          size="large"
          :loading="loading"
          @click="handleRegister"
        >
          注册
        </el-button>
      </el-form>
      
      <div class="text-center text-sm text-slate-500 mt-4">
        已有账号？
        <router-link to="/login" class="text-sky-500 hover:underline">
          返回登录
        </router-link>
      </div>
    </div>
  </div>
</template>
