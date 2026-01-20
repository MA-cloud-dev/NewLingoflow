<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserVocabulary, removeFromVocabulary, type VocabularyItem } from '@/api/learning'
import DictionarySelector from '@/components/DictionarySelector.vue'

const loading = ref(false)
const vocabulary = ref<VocabularyItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10) // 每页10个单词
const currentDictionaryId = ref<number>()
const searchQuery = ref('') // 搜索关键词

// 过滤后的词汇列表
const filteredVocabulary = computed(() => {
  if (!searchQuery.value.trim()) {
    return vocabulary.value
  }
  const query = searchQuery.value.toLowerCase().trim()
  return vocabulary.value.filter(item => 
    item.word.word.toLowerCase().includes(query) ||
    item.word.meaningCn.includes(query)
  )
})

// 计算复习进度（复习5次为100%）
function getReviewProgress(reviewCount: number): number {
  return Math.min(reviewCount * 20, 100) // 每次复习增加20%
}

// 格式化下次复习时间 - 显示剩余天数
function formatNextReviewDate(dateStr?: string): string {
  if (!dateStr) return '待安排'
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = date.getTime() - now.getTime()
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24))
  
  if (diffDays < 0) return '已到期'
  if (diffDays === 0) return '今天'
  return `${diffDays}天`
}

// 朗读单词
function speakWord(word: string) {
  if ('speechSynthesis' in window) {
    speechSynthesis.cancel() // 取消之前的朗读
    const utterance = new SpeechSynthesisUtterance(word)
    utterance.lang = 'en-US'
    utterance.rate = 0.8
    speechSynthesis.speak(utterance)
  }
}

async function loadVocabulary() {
  loading.value = true
  try {
    const res = await getUserVocabulary('all', currentPage.value, pageSize.value)
    if (res.code === 200) {
      vocabulary.value = res.data.vocabulary
      total.value = res.data.total
    }
  } catch (error: any) {
    ElMessage.error('加载生词本失败')
  } finally {
    loading.value = false
  }
}

async function handleRemove(item: VocabularyItem) {
  try {
    await ElMessageBox.confirm(`确定要删除「${item.word.word}」吗？`, '确认删除')
    await removeFromVocabulary(item.id)
    ElMessage.success('删除成功')
    loadVocabulary()
  } catch {
    // 取消删除
  }
}

const handleDictionaryChange = (dictionaryId: number) => {
  currentDictionaryId.value = dictionaryId
}

// 搜索防抖
let searchTimer: number | null = null
watch(searchQuery, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    // 搜索时重置到第一页
    if (searchQuery.value.trim()) {
      currentPage.value = 1
    }
  }, 300) as unknown as number
})

onMounted(() => {
  loadVocabulary()
})
</script>

<template>
  <div class="space-y-6 pb-10">
    <!-- 词典选择与进度 - 紧凑版 -->
    <DictionarySelector @change="handleDictionaryChange" class="mb-4" />

    <!-- 标题和统计 -->
    <div class="flex items-center justify-between px-2">
      <div>
        <h1 class="text-3xl font-black text-slate-800 tracking-tight">知识库</h1>
        <p class="text-slate-500 font-medium text-sm">你正在构建的个人词汇资产</p>
      </div>
      <div class="px-5 py-2 rounded-2xl bg-white border border-slate-100 flex items-center gap-3 shadow-sm">
        <span class="w-2 h-2 rounded-full bg-blue-500 animate-pulse"></span>
        <span class="text-sm font-black text-slate-700 uppercase tracking-widest">{{ total }} <span class="text-slate-300 ml-1">Entries</span></span>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="relative">
      <div class="absolute inset-y-0 left-4 flex items-center pointer-events-none">
        <svg class="w-5 h-5 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
        </svg>
      </div>
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索单词或释义..."
        class="w-full pl-12 pr-4 py-3.5 bg-white border border-slate-100 rounded-2xl text-slate-700 font-medium placeholder:text-slate-300 focus:outline-none focus:border-blue-300 focus:ring-4 focus:ring-blue-500/10 transition-all"
      />
      <button 
        v-if="searchQuery"
        @click="searchQuery = ''"
        class="absolute inset-y-0 right-4 flex items-center text-slate-300 hover:text-slate-500 transition-colors"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- 单词列表 -->
    <div v-loading="loading" class="space-y-4">
      <div v-if="filteredVocabulary.length === 0 && !loading" class="py-20 text-center bg-white rounded-3xl border border-slate-100">
        <div class="w-16 h-16 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-slate-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
          </svg>
        </div>
        <div class="text-lg font-bold text-slate-300">{{ searchQuery ? '未找到匹配的单词' : '开始学习，构建你的词汇库' }}</div>
      </div>
      
      <div 
        v-for="item in filteredVocabulary" 
        :key="item.id"
        class="group bg-white p-5 rounded-2xl border border-slate-100 hover:shadow-lg hover:shadow-slate-100/50 transition-all duration-300"
      >
        <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <!-- 单词信息区域 -->
          <div class="flex-1 space-y-1.5">
            <div class="flex items-center gap-3">
              <!-- 朗读按钮 -->
              <button 
                @click="speakWord(item.word.word)"
                class="w-8 h-8 rounded-lg bg-blue-50 text-blue-500 hover:bg-blue-100 transition-colors flex items-center justify-center flex-shrink-0"
                title="朗读单词"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/>
                </svg>
              </button>
              
              <span class="text-xl font-black text-slate-800 tracking-tight">{{ item.word.word }}</span>
              <span class="text-sm font-medium text-slate-400 font-serif italic">/ {{ item.word.phonetic }} /</span>
              <div :class="[
                'px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-widest',
                item.word.difficulty === 'hard' ? 'bg-red-50 text-red-500' : item.word.difficulty === 'easy' ? 'bg-emerald-50 text-emerald-500' : 'bg-amber-50 text-amber-500'
              ]">
                {{ item.word.difficulty }}
              </div>
              <!-- 词库级别标签 -->
              <template v-if="item.word.levelTags">
                <span 
                  v-for="tag in item.word.levelTags.split(',')" 
                  :key="tag"
                  class="px-2 py-0.5 rounded-md text-[10px] font-bold bg-blue-50 text-blue-600 border border-blue-100"
                >
                  {{ tag }}
                </span>
              </template>
            </div>
            
            <div class="text-base font-bold text-slate-700">{{ item.word.meaningCn }}</div>
            
            <div class="text-sm text-slate-400 font-medium italic line-clamp-1 group-hover:line-clamp-none transition-all">
              {{ item.word.exampleSentence }}
            </div>
          </div>
          
          <!-- 复习信息区域 -->
          <div class="flex items-center gap-4 lg:border-l lg:border-slate-50 lg:pl-6 flex-shrink-0">
            <!-- 下次复习时间 -->
            <div class="text-center min-w-[70px]">
              <div class="text-[10px] font-black text-slate-300 uppercase tracking-widest mb-1">Next</div>
              <div class="text-xs font-bold" :class="item.nextReviewDate && new Date(item.nextReviewDate) <= new Date() ? 'text-red-500' : 'text-slate-500'">
                {{ formatNextReviewDate(item.nextReviewDate) }}
              </div>
            </div>
            
            <!-- 复习进度环形图 (复用 Mastery 样式) -->
            <div class="flex items-center gap-2">
              <div class="text-[10px] font-black text-slate-300 uppercase tracking-widest">Mastery</div>
              <div class="relative w-10 h-10 flex items-center justify-center">
                <svg class="absolute inset-0 w-full h-full -rotate-90">
                  <circle cx="20" cy="20" r="18" fill="none" stroke="#F8FAFC" stroke-width="3" />
                  <circle cx="20" cy="20" r="18" fill="none" stroke="currentColor" stroke-width="3" 
                    :class="getReviewProgress(item.reviewCount) >= 100 ? 'text-emerald-500' : getReviewProgress(item.reviewCount) >= 60 ? 'text-amber-500' : 'text-blue-500'" 
                    :stroke-dasharray="113" :stroke-dashoffset="113 - (113 * getReviewProgress(item.reviewCount)) / 100" stroke-linecap="round" />
                </svg>
                <div class="text-[10px] font-black text-slate-700">{{ item.reviewCount }}/5</div>
              </div>
            </div>
            
            <!-- 删除按钮 -->
            <button 
              @click="handleRemove(item)"
              class="w-8 h-8 rounded-xl bg-slate-50 text-slate-300 hover:bg-red-50 hover:text-red-500 transition-all duration-300 flex items-center justify-center"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-4v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 分页 -->
    <div v-if="total > pageSize" class="mt-8 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        :total="total"
        :page-size="pageSize"
        layout="prev, pager, next"
        class="premium-pagination"
        @current-change="loadVocabulary"
      />
    </div>
  </div>
</template>

<style scoped>
.premium-pagination :deep(.el-pager li) {
  background: white;
  border-radius: 12px;
  margin: 0 4px;
  font-weight: 800;
  color: #64748B;
  border: 1px solid #F1F5F9;
}
.premium-pagination :deep(.el-pager li.is-active) {
  background: #3B82F6 !important;
  color: white !important;
  border-color: #3B82F6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}
.premium-pagination :deep(button) {
  background: white !important;
  border-radius: 12px;
  border: 1px solid #F1F5F9;
}
</style>
