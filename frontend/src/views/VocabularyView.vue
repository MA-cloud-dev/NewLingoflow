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
  <div class="space-y-8 pb-10 max-w-4xl mx-auto">
    <!-- Dictionary Selector & Header -->
    <div class="space-y-6">
      <DictionarySelector @change="handleDictionaryChange" class="mb-4" />
      
      <div class="flex items-end justify-between border-b border-ink/10 pb-4">
        <div>
          <h1 class="text-4xl font-serif font-black text-ink italic tracking-tight">Lexicon</h1>
          <p class="text-ink/50 font-sans text-sm mt-1">Your personal knowledge repository</p>
        </div>
        <div class="font-sans font-bold text-ink/40 text-xs uppercase tracking-widest">
          {{ total }} Entries
        </div>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="relative group">
      <div class="absolute inset-y-0 left-4 flex items-center pointer-events-none text-ink/30 group-focus-within:text-ink/60 transition-colors">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
        </svg>
      </div>
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Search for words or meanings..."
        class="w-full pl-12 pr-4 py-4 bg-transparent border-b-2 border-ink/10 focus:border-ink text-ink font-serif text-xl placeholder:text-ink/20 focus:outline-none transition-all"
      />
      <button 
        v-if="searchQuery"
        @click="searchQuery = ''"
        class="absolute inset-y-0 right-4 flex items-center text-ink/20 hover:text-ink/60 transition-colors"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- Vocabulary List -->
    <div v-loading="loading" class="space-y-4">
      <div v-if="filteredVocabulary.length === 0 && !loading" class="py-24 text-center border border-dashed border-ink/10 rounded-lg">
        <div class="text-ink/30 font-serif italic text-xl">{{ searchQuery ? 'No matches found.' : 'Start your journey to build your lexicon.' }}</div>
      </div>
      
      <div 
        v-for="item in filteredVocabulary" 
        :key="item.id"
        class="group relative bg-[#F9F9F7] border-b border-ink/5 p-6 transition-all hover:bg-white hover:shadow-sm rounded-lg"
      >
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <!-- Word Info -->
          <div class="flex-1 space-y-2">
            <div class="flex items-baseline gap-4">
              <span class="text-2xl font-serif font-bold text-ink">{{ item.word.word }}</span>
              <span class="text-lg font-serif italic text-ink/40">/ {{ item.word.phonetic }} /</span>
              
              <!-- Tags -->
              <div class="flex gap-2 items-center">
                 <span :class="[
                  'px-2 py-0.5 text-[10px] font-bold uppercase tracking-widest border rounded-sm',
                  item.word.difficulty === 'hard' ? 'border-red-200 text-red-400 bg-red-50' : 
                  item.word.difficulty === 'easy' ? 'border-emerald-200 text-emerald-500 bg-emerald-50' : 
                  'border-amber-200 text-amber-500 bg-amber-50'
                ]">
                  {{ item.word.difficulty }}
                </span>
                <template v-if="item.word.levelTags">
                  <span 
                    v-for="tag in item.word.levelTags.split(',')" 
                    :key="tag"
                    class="px-2 py-0.5 text-[10px] font-bold bg-ink/5 text-ink/60 rounded-sm"
                  >
                    {{ tag }}
                  </span>
                </template>
              </div>
            </div>
            
            <div class="text-lg font-sans text-ink/80">{{ item.word.meaningCn }}</div>
            
            <div class="text-sm font-serif italic text-ink/50 pl-4 border-l-2 border-ink/10">
              "{{ item.word.exampleSentence }}"
            </div>
          </div>
          
          <!-- Actions & Stats -->
          <div class="flex items-center gap-6 opacity-40 group-hover:opacity-100 transition-opacity">
            <button 
              @click="speakWord(item.word.word)"
              class="w-8 h-8 flex items-center justify-center rounded-full border border-ink/20 text-ink/60 hover:text-ink hover:border-ink transition-all"
              title="Pronounce"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/></svg>
            </button>
            
            <div class="text-center">
              <div class="text-[10px] uppercase tracking-widest text-ink/40">Review</div>
              <div class="font-bold text-xs" :class="item.nextReviewDate && new Date(item.nextReviewDate) <= new Date() ? 'text-red-500' : 'text-ink/60'">
                {{ formatNextReviewDate(item.nextReviewDate) }}
              </div>
            </div>

            <button 
              @click="handleRemove(item)"
              class="w-8 h-8 flex items-center justify-center text-ink/20 hover:text-red-500 transition-colors"
              title="Remove"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-4v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Pagination -->
    <div v-if="total > pageSize" class="mt-12 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        :total="total"
        :page-size="pageSize"
        layout="prev, pager, next"
        class="editorial-pagination"
        @current-change="loadVocabulary"
      />
    </div>
  </div>
</template>

<style scoped>
.editorial-pagination :deep(.el-pager li) {
  background: transparent;
  font-family: 'Source Sans 3', sans-serif;
  font-weight: 600;
  color: #1C1C1E;
  opacity: 0.4;
  transition: all 0.2s;
}
.editorial-pagination :deep(.el-pager li.is-active) {
  opacity: 1;
  font-weight: 800;
  border-bottom: 2px solid #D4B483;
}
.editorial-pagination :deep(button) {
  background: transparent !important;
  color: #1C1C1E !important;
}
</style>
