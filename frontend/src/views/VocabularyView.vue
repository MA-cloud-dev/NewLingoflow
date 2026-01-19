<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserVocabulary, removeFromVocabulary, type VocabularyItem } from '@/api/learning'
import DictionarySelector from '@/components/DictionarySelector.vue'

const loading = ref(false)
const vocabulary = ref<VocabularyItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const currentDictionaryId = ref<number>()

async function loadVocabulary() {
  loading.value = true
  try {
    const res = await getUserVocabulary('all', currentPage.value, 20)
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
  // TODO: 如果API支持，这里可以按词典筛选
  // loadVocabulary()
}


onMounted(() => {
  loadVocabulary()
})
</script>

<template>
  <div class="space-y-10 pb-10">
    <!-- 词典选择与进度 -->
    <DictionarySelector @change="handleDictionaryChange" class="mb-8" />

    <div class="flex items-center justify-between px-2">
      <div>
        <h1 class="text-3xl font-black text-slate-800 tracking-tight">知识库</h1>
        <p class="text-slate-500 font-medium">你正在构建的个人词汇资产</p>
      </div>
      <div class="px-6 py-3 rounded-2xl bg-white border border-slate-100 flex items-center gap-3 shadow-sm">
        <span class="w-2 h-2 rounded-full bg-blue-500 animate-pulse"></span>
        <span class="text-sm font-black text-slate-700 uppercase tracking-widest">{{ total }} <span class="text-slate-300 ml-1">Entries</span></span>
      </div>
    </div>

    <!-- Decorative Illustration Header -->
    <div class="relative h-48 rounded-[3rem] overflow-hidden bg-slate-900 shadow-2xl group">
      <img src="/library-header.png" class="absolute inset-0 w-full h-full object-cover opacity-60 group-hover:scale-105 transition-transform duration-1000" />
      <div class="absolute inset-0 bg-gradient-to-r from-slate-900 via-transparent to-transparent"></div>
      <div class="relative h-full flex flex-col justify-center px-12">
        <div class="inline-block px-3 py-1 bg-blue-500 text-[10px] font-black text-white uppercase tracking-widest rounded-lg mb-2">Resource Library</div>
        <div class="text-3xl font-black text-white tracking-tight">持久化的记忆资产</div>
        <div class="text-slate-400 font-medium max-w-md mt-2">系统生成的生词库，通过科学的间隔重复算法，将短期记忆转化为长期资产。</div>
      </div>
    </div>

    <div v-loading="loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-1 gap-6">
      <div v-if="vocabulary.length === 0 && !loading" class="col-span-full py-32 text-center bg-white rounded-[3rem] border border-slate-100">
        <div class="w-20 h-20 bg-slate-50 rounded-full flex items-center justify-center mx-auto mb-6">
          <svg class="w-10 h-10 text-slate-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
          </svg>
        </div>
        <div class="text-xl font-bold text-slate-300">词像大海，期待你的每一次探寻</div>
      </div>
      
      <div 
        v-for="item in vocabulary" 
        :key="item.id"
        class="group bg-white p-5 rounded-3xl border border-slate-100 hover:shadow-xl hover:shadow-slate-200/50 transition-all duration-300 relative overflow-hidden"
      >
        <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div class="flex-1 space-y-2">
            <div class="flex items-center gap-3">
              <span class="text-xl font-black text-slate-800 tracking-tight">{{ item.word.word }}</span>
              <span class="text-sm font-medium text-slate-400 font-serif italic">/ {{ item.word.phonetic }} /</span>
              <div :class="[
                'px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-widest',
                item.word.difficulty === 'hard' ? 'bg-red-50 text-red-500' : item.word.difficulty === 'easy' ? 'bg-emerald-50 text-emerald-500' : 'bg-amber-50 text-amber-500'
              ]">
                {{ item.word.difficulty }}
              </div>
            </div>
            
            <div class="text-base font-bold text-slate-700">{{ item.word.meaningCn }}</div>
            
            <div class="text-sm text-slate-400 font-medium italic line-clamp-1 group-hover:line-clamp-none transition-all">
              {{ item.word.exampleSentence }}
            </div>
          </div>
          
          <div class="flex items-center gap-6 lg:border-l lg:border-slate-50 lg:pl-6">
            <div class="flex items-center gap-3">
              <div class="text-[10px] font-black text-slate-300 uppercase tracking-widest">Mastery</div>
              <div class="relative w-10 h-10 flex items-center justify-center">
                <svg class="absolute inset-0 w-full h-full -rotate-90">
                  <circle cx="20" cy="20" r="18" fill="none" stroke="#F8FAFC" stroke-width="3" />
                  <circle cx="20" cy="20" r="18" fill="none" stroke="currentColor" stroke-width="3" 
                    :class="item.familiarity >= 80 ? 'text-emerald-500' : item.familiarity >= 50 ? 'text-amber-500' : 'text-red-500'" 
                    :stroke-dasharray="113" :stroke-dashoffset="113 - (113 * item.familiarity) / 100" stroke-linecap="round" />
                </svg>
                <div class="text-xs font-black text-slate-800">{{ item.familiarity }}%</div>
              </div>
            </div>
            
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
    
    <div v-if="total > 20" class="mt-12 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        :total="total"
        :page-size="20"
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

