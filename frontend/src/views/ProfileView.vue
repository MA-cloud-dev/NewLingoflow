<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserProfile, updateProfile, changePassword, type UserProfile, type UpdateProfileData } from '@/api/user'

// 用户信息
const profile = ref<UserProfile | null>(null)
const loading = ref(true)

// 编辑模式
const isEditing = ref(false)
const editForm = ref<UpdateProfileData>({
  email: '',
  dailyGoal: 20,
  difficultyLevel: 'medium'
})

// 修改密码
const showPasswordModal = ref(false)
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordLoading = ref(false)

// 难度等级选项
const difficultyOptions = [
  { value: 'easy', label: '基础', desc: '适合初学者的核心词汇' },
  { value: 'medium', label: '进阶', desc: '流利交流的标准词库' },
  { value: 'hard', label: '高级', desc: '复杂的专业术语' }
]

// 格式化注册时间
const formattedCreatedAt = computed(() => {
  if (!profile.value?.createdAt) return ''
  const date = new Date(profile.value.createdAt)
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
})

// 获取用户信息
async function loadProfile() {
  loading.value = true
  try {
    const res = await getUserProfile()
    if (res.code === 200) {
      profile.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载用户信息失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 开始编辑
function startEdit() {
  if (profile.value) {
    editForm.value = {
      email: profile.value.email || '',
      dailyGoal: profile.value.dailyGoal,
      difficultyLevel: profile.value.difficultyLevel
    }
    isEditing.value = true
  }
}

// 取消编辑
function cancelEdit() {
  isEditing.value = false
}

// 保存编辑
async function saveEdit() {
  try {
    const res = await updateProfile(editForm.value)
    if (res.code === 200) {
      profile.value = res.data
      isEditing.value = false
      ElMessage.success('个人资料更新成功')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '更新失败')
  }
}

// 打开修改密码弹窗
function openPasswordModal() {
  passwordForm.value = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  showPasswordModal.value = true
}

// 修改密码
async function submitChangePassword() {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.error('两次密码不一致')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    ElMessage.error('密码长度至少6位')
    return
  }
  
  passwordLoading.value = true
  try {
    const res = await changePassword(passwordForm.value)
    if (res.code === 200) {
      showPasswordModal.value = false
      ElMessage.success('密码修改成功')
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="max-w-4xl mx-auto py-8 font-sans text-ink">
    <!-- Header -->
    <header class="mb-12 border-b border-ink/10 pb-6">
      <div class="flex items-end gap-6">
        <div>
           <div class="text-xs font-bold text-ink/40 uppercase tracking-widest mb-2">学生档案</div>
          <h1 class="text-4xl font-serif font-black text-ink italic tracking-tight">学习记录</h1>
        </div>
      </div>
    </header>

    <div v-if="loading" class="py-24 text-center">
      <div class="inline-block w-8 h-8 border-2 border-ink/30 border-t-ink rounded-full animate-spin"></div>
      <div class="mt-4 text-ink/40 font-serif italic">正在获取记录...</div>
    </div>

    <div v-else-if="profile" class="space-y-8 animate-in fade-in duration-500">
      <!-- User Info Card -->
      <section class="paper-card p-10 relative overflow-hidden">
        <div class="relative z-10 flex flex-col md:flex-row items-start md:items-center gap-8">
          <!-- Avatar -->
          <div class="relative group">
            <div class="w-32 h-32 bg-ink text-white rounded-2xl flex items-center justify-center text-5xl font-serif font-bold italic shadow-xl rotate-3 transition-transform group-hover:rotate-0">
              {{ profile.username?.charAt(0).toUpperCase() }}
            </div>
            <div class="absolute -bottom-2 -right-2 bg-emerald-500 text-white p-2 rounded-full border-4 border-white">
              <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
            </div>
          </div>
          
          <div class="flex-1 space-y-4">
            <div>
              <h2 class="text-3xl font-serif font-bold text-ink mb-1">{{ profile.username }}</h2>
              <div class="flex items-center gap-3">
                 <span class="px-3 py-1 bg-ink text-white text-[10px] uppercase tracking-widest font-bold rounded-sm">认证学者</span>
                 <span class="text-sm text-ink/40 font-serif italic">注册于 {{ formattedCreatedAt }}</span>
              </div>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 pt-4 border-t border-ink/5">
              <!-- Email -->
              <div class="space-y-1">
                <label class="text-[10px] font-bold text-ink/40 uppercase tracking-widest">电子邮箱</label>
                <div v-if="!isEditing" class="font-medium text-lg">{{ profile.email || '未设置' }}</div>
                 <input v-else v-model="editForm.email" type="email" class="w-full bg-transparent border-b border-ink/30 focus:border-ink py-1 font-medium focus:outline-none" placeholder="输入邮箱" />
              </div>
              
               <!-- Account Action -->
               <div class="space-y-1">
                  <label class="text-[10px] font-bold text-ink/40 uppercase tracking-widest">安全</label>
                  <div>
                    <button @click="openPasswordModal" class="text-sm border-b border-ink/30 hover:border-ink transition-colors pb-0.5 font-medium">修改密码</button>
                  </div>
               </div>
            </div>
          </div>
          
          <button v-if="!isEditing" @click="startEdit" class="absolute top-8 right-8 text-ink/40 hover:text-ink transition-colors">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/></svg>
          </button>
        </div>
        
        <!-- Edit Actions -->
        <div v-if="isEditing" class="mt-8 flex justify-end gap-4 pt-6 border-t border-ink/5">
          <button @click="cancelEdit" class="px-6 py-2 rounded-lg font-bold text-sm text-ink/60 hover:bg-ink/5 transition-colors">取消</button>
          <button @click="saveEdit" class="px-6 py-2 rounded-lg bg-ink text-white font-bold text-sm shadow-lg hover:bg-ink/90 transition-all">保存更改</button>
        </div>
      </section>

      <!-- Stats Grid -->
      <section class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="paper-card p-8 group hover:border-gold/50 transition-colors">
          <div class="flex items-start justify-between mb-4">
             <div class="text-[10px] font-bold text-ink/40 uppercase tracking-widest">掌握词汇</div>
             <svg class="w-6 h-6 text-ink/20 group-hover:text-gold transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>
          </div>
          <div class="text-5xl font-serif font-black text-ink">{{ profile.totalWordsLearned }}</div>
          <div class="mt-2 text-sm text-ink/60 font-serif italic">个已掌握的单词</div>
        </div>
        
         <div class="paper-card p-8 group hover:border-gold/50 transition-colors">
          <div class="flex items-start justify-between mb-4">
             <div class="text-[10px] font-bold text-ink/40 uppercase tracking-widest">当前连胜</div>
             <svg class="w-6 h-6 text-ink/20 group-hover:text-gold transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
          </div>
          <div class="text-5xl font-serif font-black text-ink">{{ profile.streakDays }}</div>
          <div class="mt-2 text-sm text-ink/60 font-serif italic">连续学习天数</div>
        </div>
      </section>

      <!-- Settings Card -->
      <section class="paper-card p-10">
        <div class="mb-8 pb-4 border-b border-ink/5">
          <h2 class="text-2xl font-serif font-bold text-ink">学习偏好</h2>
          <p class="text-ink/40 text-sm mt-1">定制你的学习课程</p>
        </div>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-12">
          <!-- Daily Goal -->
          <div>
            <div class="text-xs font-bold text-ink/40 uppercase tracking-widest mb-4">每日负荷</div>
            <div class="flex items-baseline gap-4">
               <div v-if="!isEditing" class="text-3xl font-serif font-bold text-ink">{{ profile.dailyGoal }} <span class="text-lg font-normal text-ink/40 italic">词</span></div>
               <div v-else class="flex items-center gap-2">
                 <input v-model.number="editForm.dailyGoal" type="number" class="w-20 bg-ink/5 border-none rounded p-2 text-center font-serif font-bold text-xl focus:ring-1 focus:ring-ink focus:outline-none" />
                 <span class="text-lg text-ink/40 italic">词 / 天</span>
               </div>
            </div>
            <p class="mt-2 text-sm text-ink/60 leading-relaxed">调整每日学习的新词数量。</p>
          </div>

          <!-- Difficulty -->
          <div>
            <div class="text-xs font-bold text-ink/40 uppercase tracking-widest mb-4">课程等级</div>
            <div class="space-y-3">
              <label 
                v-for="option in difficultyOptions" 
                :key="option.value"
                class="block p-4 border rounded-lg transition-all"
                :class="[
                  (isEditing ? editForm.difficultyLevel : profile.difficultyLevel) === option.value 
                    ? 'border-ink bg-ink text-white shadow-md' 
                    : 'border-ink/10 hover:border-ink/30 bg-transparent text-ink/60',
                   !isEditing ? 'pointer-events-none' : 'cursor-pointer'
                ]"
              >
                <div class="flex items-center justify-between">
                  <span class="font-bold uppercase tracking-wider text-sm">{{ option.label }}</span>
                  <input type="radio" :value="option.value" v-model="editForm.difficultyLevel" class="hidden" :disabled="!isEditing">
                  <div v-if="(isEditing ? editForm.difficultyLevel : profile.difficultyLevel) === option.value" class="w-2 h-2 bg-white rounded-full"></div>
                </div>
              </label>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Password Modal -->
    <Teleport to="body">
      <div v-if="showPasswordModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-ink/20 backdrop-blur-sm" @click.self="showPasswordModal = false">
        <div class="bg-white rounded-2xl p-8 max-w-sm w-full shadow-2xl border border-ink/10 animate-in zoom-in-95 duration-200">
          <h3 class="text-2xl font-serif font-bold text-ink mb-6">安全设置</h3>
          
          <div class="space-y-4">
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-ink/40 uppercase tracking-widest">当前密码</label>
              <input v-model="passwordForm.currentPassword" type="password" class="w-full bg-[#F9F9F7] border border-ink/10 rounded p-3 text-ink focus:outline-none focus:border-ink transition-colors" />
            </div>
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-ink/40 uppercase tracking-widest">新密码</label>
              <input v-model="passwordForm.newPassword" type="password" class="w-full bg-[#F9F9F7] border border-ink/10 rounded p-3 text-ink focus:outline-none focus:border-ink transition-colors" />
            </div>
            <div class="space-y-1">
              <label class="text-[10px] font-bold text-ink/40 uppercase tracking-widest">确认密码</label>
              <input v-model="passwordForm.confirmPassword" type="password" class="w-full bg-[#F9F9F7] border border-ink/10 rounded p-3 text-ink focus:outline-none focus:border-ink transition-colors" />
            </div>
          </div>
          
          <div class="flex justify-end gap-3 mt-8">
            <button @click="showPasswordModal = false" class="px-4 py-2 text-sm font-bold text-ink/60 hover:text-ink">取消</button>
            <button @click="submitChangePassword" :disabled="passwordLoading" class="px-6 py-2 bg-ink text-white text-sm font-bold uppercase tracking-widest rounded shadow-lg hover:bg-ink/90 transition-all disabled:opacity-50">
              {{ passwordLoading ? '更新中...' : '更新' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* Scoped styles are minimal, relying on utility classes */
</style>
