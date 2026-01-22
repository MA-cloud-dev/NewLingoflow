<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  getLearningWords, 
  batchAddToVocabulary,
  generateArticle, 
  submitSentence,
  updateLearningProgress,
  type Word, 
  type VocabularyItem,
  type ArticleData,
  type SentenceFeedback
} from '@/api/learning'
import LearningSettingsBar, { type LearningSettings } from '@/components/LearningSettingsBar.vue'
import ComprehensionQuiz from '@/components/ComprehensionQuiz.vue'

// 学习阶段
type LearningPhase = 'quiz' | 'study' | 'complete'
const phase = ref<LearningPhase>('quiz')

// 学习设置
const learningSettings = ref<LearningSettings>({
  theme: 'Daily Life',
  difficulty: 'medium',
  length: 'short'
})

// ========== 选词阶段 (Quiz) ==========
const allWords = ref<Word[]>([])
const currentWordIndex = ref(0)
const selectedWords = ref<Word[]>([])
const loadingWords = ref(false)
const noWordsAvailable = ref(false) // 标记是否无可学习词汇

// 四选一答题相关
const quizOptions = ref<string[]>([])
const selectedOption = ref<string | null>(null)
const isCorrect = ref<boolean | null>(null)
const showResult = ref(false)

const currentWord = computed(() => allWords.value[currentWordIndex.value])
const autoSpeak = ref(true)

// ========== 学习阶段 (文章+题目+造句一体化) ==========
const currentArticle = ref<ArticleData | null>(null)
const sessionId = ref<number | null>(null)
const vocabularyItems = ref<VocabularyItem[]>([])
const loadingArticle = ref(false)

// 造句相关
const selectedSentenceWordIndex = ref(0) // 当前选中的造句单词索引
const userSentence = ref('')
const feedback = ref<SentenceFeedback | null>(null)
const submittingSentence = ref(false)
const attemptCount = ref(0) // 当前单词的尝试次数
const maxAttempts = 3 // 最大尝试次数
const bestScores = ref<Map<number, number>>(new Map()) // 存储每个单词的最高分
const showTranslation = ref(false) // 控制文章翻译显示

const currentVocabulary = computed(() => vocabularyItems.value[selectedSentenceWordIndex.value])

const currentSentenceTask = computed(() => {
  if (!currentArticle.value?.sentenceMakingTasks) return null
  const wordStr = currentVocabulary.value?.word?.word
  return currentArticle.value.sentenceMakingTasks.find(t => t.word === wordStr)
})



// 剩余尝试次数
const remainingAttempts = computed(() => maxAttempts - attemptCount.value)

// ========== 朗读功能 ==========
function speakWord(word: string) {
  if ('speechSynthesis' in window) {
    speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(word)
    utterance.lang = 'en-US'
    utterance.rate = 0.8
    speechSynthesis.speak(utterance)
  }
}

watch(currentWord, (word) => {
  if (word && autoSpeak.value && phase.value === 'quiz') {
    setTimeout(() => speakWord(word.word), 300)
  }
  // 切换单词时重置状态
  if (word) {
    generateQuizOptions()
  }
})

// ========== 选词逻辑 ==========
async function loadWords() {
  loadingWords.value = true
  noWordsAvailable.value = false
  try {
    const res = await getLearningWords(20)
    if (res.code === 200) {
      allWords.value = res.data.words || []
      
      // 检测是否无可学习词汇
      if (allWords.value.length === 0) {
        noWordsAvailable.value = true
        ElMessage.info('暂无新单词可学习，请先添加词库或等待复习')
        return
      }
      
      // Restore state if available
      if (typeof res.data.currentIndex === 'number') {
        currentWordIndex.value = res.data.currentIndex
      } else {
        currentWordIndex.value = 0
      }
      
      if (res.data.selectedWords) {
        selectedWords.value = res.data.selectedWords
      }

      // Ensure index is valid
      if (currentWordIndex.value >= allWords.value.length) {
         currentWordIndex.value = 0
      }

      generateQuizOptions()
    }
  } catch (error) {
    ElMessage.error('加载单词失败')
  } finally {
    loadingWords.value = false
  }
}

function syncProgress() {
  updateLearningProgress(currentWordIndex.value, selectedWords.value)
}

function generateQuizOptions() {
  if (!currentWord.value) return
  
  // 正确答案
  const correctAnswer = currentWord.value.meaningCn
  
  // 从其他单词中随机抽取 3 个干扰项
  const otherWords = allWords.value.filter((w: Word) => w.id !== currentWord.value.id)
  const shuffled = otherWords.sort(() => Math.random() - 0.5)
  const distractors = shuffled.slice(0, 3).map((w: Word) => w.meaningCn)
  
  // 合并并打乱顺序
  const options = [correctAnswer, ...distractors].sort(() => Math.random() - 0.5)
  quizOptions.value = options
  
  // 重置状态
  selectedOption.value = null
  isCorrect.value = null
  showResult.value = false
  givenUp.value = false
}

const givenUp = ref(false)

function giveUp() {
  if (showResult.value) return
  givenUp.value = true
  showResult.value = true
  // 标记正确答案但不选中任何选项（或者高亮正确答案）
  isCorrect.value = false // 视为错误
}

function selectOption(option: string) {
  if (showResult.value) return // 已经选过了
  
  selectedOption.value = option
  isCorrect.value = option === currentWord.value.meaningCn
  showResult.value = true
}

async function addWord() {
  if (!currentWord.value) return
  
  if (!selectedWords.value.find((w: Word) => w.id === currentWord.value.id)) {
    selectedWords.value.push(currentWord.value)
    ElMessage.success(`已添加「${currentWord.value.word}」(${selectedWords.value.length}/5)`)
    // Sync after adding
    syncProgress()
  }
  
  // 如果已经选了5个单词，或者已经是最后一个单词，直接进入学习阶段
  const isLastWord = currentWordIndex.value >= allWords.value.length - 1
  if (selectedWords.value.length >= 5 || isLastWord) {
    await startStudyPhase()
  } else {
    nextWord()
  }
}

function skipWord() {
  nextWord()
}

function nextWord() {
  if (currentWordIndex.value < allWords.value.length - 1) {
    currentWordIndex.value++
    syncProgress()
    // generateQuizOptions 由 watch 触发
  } else {
    // 已经是最后一个单词，只要有选中的单词就进入学习阶段
    if (selectedWords.value.length > 0) {
      startStudyPhase()
    } else {
      ElMessage.warning('请选择要学习的单词')
      loadWords()
    }
  }
}

// ========== 学习阶段（一体化） ==========
async function startStudyPhase() {
  if (selectedWords.value.length === 0) {
    ElMessage.warning('请先选择单词')
    return
  }
  
  loadingArticle.value = true
  phase.value = 'study'
  
  try {
    // 使用批量接口替换原来的循环添加 + 查询逻辑
    const batchRes = await batchAddToVocabulary(selectedWords.value.map(w => w.id))
    
    if (batchRes.code === 200) {
      vocabularyItems.value = batchRes.data
    } else {
      ElMessage.error('处理生词失败')
      phase.value = 'quiz'
      return
    }
    
    if (vocabularyItems.value.length === 0) {
      ElMessage.error('未找到对应的生词本记录')
      return
    }
    
    const articleRes = await generateArticle(
      vocabularyItems.value.map((v: VocabularyItem) => v.id),
      learningSettings.value.difficulty,
      learningSettings.value.length,
      learningSettings.value.theme
    )
    
    if (articleRes.code === 200) {
      sessionId.value = articleRes.data.sessionId
      currentArticle.value = articleRes.data.article
    } else {
      ElMessage.error('生成文章失败')
      phase.value = 'quiz'
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '生成文章失败')
    phase.value = 'quiz'
  } finally {
    loadingArticle.value = false
  }
}

// ========== 造句相关 ==========
function selectWordForSentence(index: number) {
  selectedSentenceWordIndex.value = index
  userSentence.value = ''
  feedback.value = null
  attemptCount.value = 0
}

async function submitUserSentence() {
  if (!userSentence.value.trim()) {
    ElMessage.warning('请输入句子')
    return
  }
  
  if (!currentVocabulary.value || !sessionId.value) return
  if (attemptCount.value >= maxAttempts) {
    ElMessage.warning('已达到最大尝试次数')
    return
  }
  
  submittingSentence.value = true
  try {
    const res = await submitSentence(
      sessionId.value,
      currentVocabulary.value.id,
      userSentence.value
    )
    
    if (res.code === 200) {
      feedback.value = res.data
      attemptCount.value++
      
      // 更新最高分
      const currentScore = res.data.score
      const vocabId = currentVocabulary.value.id
      const prevBest = bestScores.value.get(vocabId) || 0
      if (currentScore > prevBest) {
        bestScores.value.set(vocabId, currentScore)
      }
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '提交失败')
  } finally {
    submittingSentence.value = false
  }
}

function retryCurrentWord() {
  userSentence.value = ''
  feedback.value = null
}

function finishLearning() {
  phase.value = 'complete'
}

function resetLearning() {
  phase.value = 'quiz'
  currentWordIndex.value = 0
  selectedWords.value = []
  currentArticle.value = null
  sessionId.value = null
  vocabularyItems.value = []
  selectedSentenceWordIndex.value = 0
  userSentence.value = ''
  feedback.value = null
  attemptCount.value = 0
  bestScores.value.clear()
  noWordsAvailable.value = false
  loadWords()
}

// 解析文章内容，高亮目标单词
const articleSegments = computed(() => {
  if (!currentArticle.value?.content) return []
  const parts = currentArticle.value.content.split(/(\*\*.*?\*\*)/g)
  return parts.map(part => {
    if (part.startsWith('**') && part.endsWith('**')) {
      const word = part.slice(2, -2)
      const vocabItem = vocabularyItems.value.find(v => v.word?.word?.toLowerCase() === word.toLowerCase())
      return {
        text: word,
        isHighlight: true,
        meaning: vocabItem?.word?.meaningCn || ''
      }
    }
    return { text: part, isHighlight: false, meaning: '' }
  })
})

onMounted(() => {
  loadWords()
})
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-8">
    <!-- Settings Bar (Pass-through style update recommended inside component, but wrapper here helps) -->
    <div class="paper-card p-4 rounded-lg flex justify-between items-center bg-white sticky top-4 z-40 shadow-sm border border-[#E5E5E0]">
       <div class="font-serif font-bold italic text-ink text-lg">学习设置</div>
       <LearningSettingsBar v-model="learningSettings" class="!bg-transparent !p-0 !shadow-none" />
    </div>

    <!-- ========== QUIZ PHASE ========== -->
    <div v-if="phase === 'quiz'" class="max-w-3xl mx-auto py-12">
      <div class="text-center mb-12">
        <div class="inline-block border-b-2 border-[#D4B483] mb-4 pb-1 text-xs font-bold uppercase tracking-[0.2em] text-[#D4B483]">
          单词初识
        </div>
        <h1 class="text-5xl font-serif font-bold text-ink mb-4 italic">Expand Your Lexicon</h1>
        <p class="text-ink/60 font-light text-lg">选择单词以生成你的每日阅读列表。</p>
        
        <div class="mt-8 flex justify-center items-center gap-6 text-sm font-medium text-ink/60">
           <label class="flex items-center gap-2 cursor-pointer hover:text-ink transition-colors">
            <input type="checkbox" v-model="autoSpeak" class="w-4 h-4 rounded border-[#D4B483] text-[#D4B483] focus:ring-[#D4B483]">
            自动发音
          </label>
           <div class="h-4 w-px bg-ink/10"></div>
           <div>已选: <span class="font-bold text-ink">{{ selectedWords.length }}</span> / 5</div>
        </div>
      </div>
      
      <!-- Selected Words Pills -->
      <div v-if="selectedWords.length > 0" class="flex flex-wrap justify-center gap-3 mb-10">
        <span 
          v-for="w in selectedWords" 
          :key="w.id"
          class="px-4 py-1 bg-[#F9F9F7] border border-[#E5E5E0] text-ink rounded-full text-xs font-bold uppercase tracking-wider"
        >
          {{ w.word }}
        </span>
      </div>
      
      <!-- Empty State: No Words Available -->
      <div v-if="noWordsAvailable && !loadingWords" class="paper-card p-12 md:p-16 text-center">
        <div class="w-20 h-20 bg-[#F9F9F7] rounded-full flex items-center justify-center mx-auto mb-8 border border-[#E5E5E0]">
          <svg class="w-10 h-10 text-[#D4B483]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
        </div>
        <h2 class="text-3xl font-serif font-bold text-ink mb-4 italic">暂无可学习词汇</h2>
        <p class="text-ink/60 font-light text-lg mb-8 max-w-md mx-auto">
          你已经学习了当前词库中的所有单词，或者词库尚未添加新词汇。
        </p>
        <div class="flex flex-col sm:flex-row justify-center gap-4">
          <router-link 
            to="/vocabulary" 
            class="px-8 py-4 bg-ink text-white font-bold uppercase tracking-widest text-sm hover:bg-ink/90 transition-all shadow-lg"
          >
            前往词库
          </router-link>
          <router-link 
            to="/review" 
            class="px-8 py-4 border border-[#E5E5E0] text-ink font-bold uppercase tracking-widest text-sm hover:border-ink transition-colors"
          >
            复习单词
          </router-link>
        </div>
      </div>


      <!-- Quiz Card -->
      <div v-if="currentWord" v-loading="loadingWords" class="paper-card p-12 md:p-16 relative group transition-all duration-500 hover:shadow-xl">
        <div class="text-center mb-12">
          <div class="inline-flex items-center gap-4 mb-4">
            <h2 class="text-6xl font-serif font-black text-ink tracking-tight">{{ currentWord.word }}</h2>
            <button 
              @click="speakWord(currentWord.word)"
              class="w-10 h-10 rounded-full border border-ink/10 flex items-center justify-center text-ink/40 hover:text-ink hover:border-ink transition-all"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/></svg>
            </button>
          </div>
          <div class="text-xl text-ink/40 font-serif italic">/ {{ currentWord.phonetic }} /</div>
        </div>
        
        <div class="grid grid-cols-1 gap-4 mb-10">
          <button
            v-for="(option, idx) in quizOptions"
            :key="idx"
            @click="selectOption(option)"
            :disabled="showResult"
            class="group relative p-5 border text-left transition-all duration-300"
            :class="[
              showResult && option === currentWord.meaningCn
                ? 'border-green-600 bg-green-50/50 text-green-800'
                : showResult && option === selectedOption && !isCorrect
                  ? 'border-red-400 bg-red-50/50 text-red-800'
                  : selectedOption === option
                    ? 'border-ink bg-ink text-white'
                    : 'border-[#E5E5E0] hover:border-ink/40 hover:bg-[#F9F9F7]'
            ]"
          >
            <div class="flex items-center gap-4">
              <span class="font-serif italic text-ink/30 text-lg group-hover:text-ink/60">{{ String.fromCharCode(65 + idx) }}</span>
              <span class="text-lg font-medium tracking-wide">{{ option }}</span>
            </div>
          </button>
        </div>
        
        <!-- Controls -->
        <div v-if="!showResult" class="text-center">
          <button @click="giveUp" class="text-sm font-bold uppercase tracking-widest text-[#D4B483] hover:text-[#BFA175] transition-colors border-b border-transparent hover:border-[#BFA175]">
            查看释义
          </button>
        </div>
        
        <div v-if="showResult" class="animate-in fade-in slide-in-from-bottom-4 duration-500">
           <div class="text-center mb-8 p-6 bg-[#F9F9F7] border-l-4 border-ink">
              <div class="text-xl font-bold text-ink mb-2">{{ currentWord.meaningCn }}</div>
              <div v-if="currentWord.exampleSentence" class="text-ink/60 font-serif italic text-lg">
                "{{ currentWord.exampleSentence }}"
              </div>
           </div>
           
           <div class="flex gap-4">
             <button @click="skipWord" class="flex-1 py-4 border border-[#E5E5E0] bg-white hover:bg-[#F9F9F7] text-ink font-bold uppercase tracking-widest text-sm transition-all text-ink/40 hover:text-ink">
               跳过
             </button>
             <button @click="addWord" class="flex-[2] py-4 bg-ink text-white font-bold uppercase tracking-widest text-sm hover:bg-ink/90 transition-all shadow-lg">
               {{ (isCorrect && !givenUp) ? '添加到生词本' : '学习此词' }}
             </button>
           </div>
        </div>
      </div>
    </div>

    <!-- ========== STUDY PHASE ========== -->
    <div v-else-if="phase === 'study'" class="space-y-8">
      <!-- Loading State -->
      <div v-if="loadingArticle" class="paper-card p-24 text-center">
        <div class="animate-pulse flex flex-col items-center">
          <div class="h-1 w-24 bg-ink/10 mb-8 rounded-full"></div>
          <div class="h-4 w-64 bg-ink/10 mb-4 rounded-full"></div>
          <div class="h-4 w-48 bg-ink/10 mb-4 rounded-full"></div>
          <div class="h-4 w-56 bg-ink/10 rounded-full"></div>
          <p class="mt-8 font-serif italic text-ink/40">正在为你生成个性化文章...</p>
        </div>
      </div>
      
      <div v-else-if="currentArticle">
        <!-- Header -->
        <div class="flex items-center justify-between border-b border-ink/10 pb-6">
          <button @click="resetLearning" class="flex items-center gap-2 text-ink/40 hover:text-ink transition-colors font-bold uppercase tracking-widest text-xs">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18"/></svg>
            退出学习
          </button>
          <div class="font-serif italic text-ink/40">
            进度: <span class="text-ink not-italic font-sans font-bold">{{ bestScores.size }}</span> / {{ vocabularyItems.length }}
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-12 gap-10">
          <!-- Article Column (Left) -->
          <div class="lg:col-span-7 space-y-8">
            <article class="paper-card p-10 md:p-14">
               <div class="flex items-center justify-between mb-8">
                 <div class="flex gap-3">
                   <span class="px-3 py-1 border border-ink/20 text-ink/60 text-[10px] font-bold uppercase tracking-widest rounded-full">{{ learningSettings.theme }}</span>
                   <span class="px-3 py-1 border border-ink/20 text-ink/60 text-[10px] font-bold uppercase tracking-widest rounded-full">阅读</span>
                 </div>
                 <button @click="speakWord(currentArticle.content.replace(/\*\*/g, ''))" class="text-ink/40 hover:text-ink transition-colors" title="朗读全文">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/></svg>
                 </button>
               </div>
               
               <h2 class="text-4xl font-serif font-bold text-ink mb-8 leading-tight">{{ currentArticle.title }}</h2>
               
               <div class="prose prose-lg prose-slate max-w-none font-serif text-ink/80 leading-loose">
                 <template v-for="(seg, idx) in articleSegments" :key="idx">
                    <el-popover v-if="seg.isHighlight" placement="top" :width="240" trigger="hover" :show-arrow="false"
                      popper-class="!rounded-none !border !border-ink !shadow-[4px_4px_0_rgba(0,0,0,0.1)] !p-0"
                    >
                      <template #reference>
                        <span class="bg-[#FEF3C7] text-ink font-bold px-1 cursor-pointer hover:bg-[#FDE68A] transition-colors decoration-clone box-decoration-clone border-b border-[#D4B483] border-dashed">
                          {{ seg.text }}
                        </span>
                      </template>
                      <div class="p-4 bg-white">
                        <div class="font-serif font-bold text-xl text-ink mb-1">{{ seg.text }}</div>
                        <div class="text-ink/60 font-medium text-sm mb-3">{{ seg.meaning }}</div>
                        <button class="text-[10px] uppercase font-bold tracking-widest text-[#D4B483] hover:text-[#BFA175] flex items-center gap-1" @click="speakWord(seg.text)">
                           朗读 <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"/></svg>
                        </button>
                      </div>
                    </el-popover>
                    <span v-else>{{ seg.text }}</span>
                 </template>
               </div>
               
               <!-- Translation Toggle -->
               <div v-if="currentArticle.chineseTranslation" class="mt-12 pt-8 border-t border-ink/10">
                 <button @click="showTranslation = !showTranslation" class="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-ink/40 hover:text-ink transition-colors">
                    <span>{{ showTranslation ? '隐藏翻译' : '显示翻译' }}</span>
                    <svg class="w-4 h-4 transition-transform" :class="showTranslation ? 'rotate-180' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/></svg>
                 </button>
                 <div v-show="showTranslation" class="mt-6 font-serif leading-loose text-ink/60 text-lg bg-[#F9F9F7] p-8 border-l-2 border-[#D4B483]">
                    {{ currentArticle.chineseTranslation }}
                 </div>
               </div>
            </article>
            
            <div v-if="currentArticle.comprehensionQuestions?.length" class="paper-card p-8">
               <h3 class="font-serif font-bold text-2xl mb-6">阅读理解</h3>
               <ComprehensionQuiz :questions="currentArticle.comprehensionQuestions" @complete="() => {}" />
            </div>
          </div>
          
          <!-- Context Column (Right) -->
          <div class="lg:col-span-5 space-y-6">
            <div class="bg-white border border-[#E5E5E0] shadow-xl p-6 md:p-8 sticky top-6">
               <div class="mb-8 border-b border-ink/10 pb-4 flex justify-between items-end">
                 <div>
                    <h3 class="font-serif font-bold text-2xl italic">造句练习</h3>
                    <p class="text-sm text-ink/40 mt-1">在语境中运用单词</p>
                 </div>
                 <div class="text-3xl font-serif text-[#D4B483]">{{ selectedSentenceWordIndex + 1 }}<span class="text-base text-ink/20">/{{ vocabularyItems.length }}</span></div>
               </div>
               
               <!-- Word Selector -->
               <div class="flex flex-wrap gap-2 mb-8">
                  <button 
                    v-for="(vocab, idx) in vocabularyItems" 
                    :key="vocab.id"
                    @click="selectWordForSentence(idx)"
                    class="px-3 py-1 text-xs font-bold uppercase tracking-wider border transition-all"
                    :class="[
                      selectedSentenceWordIndex === idx 
                        ? 'bg-ink text-white border-ink' 
                        : bestScores.has(vocab.id)
                          ? 'bg-white text-green-700 border-green-200 line-through decoration-green-700/30'
                          : 'bg-white text-ink/40 border-ink/10 hover:border-ink/40'
                    ]"
                  >
                    {{ vocab.word?.word }}
                  </button>
               </div>
               
               <!-- Current Task -->
               <div v-if="currentVocabulary" class="space-y-6">
                  <div class="bg-[#F9F9F7] p-6 border-l-2 border-ink">
                     <div class="text-[10px] font-bold uppercase tracking-widest text-ink/40 mb-2">目标单词</div>
                     <div class="text-3xl font-serif font-black mb-4">{{ currentVocabulary.word?.word }}</div>
                     
                     <div v-if="currentSentenceTask">
                        <div class="font-serif italic text-lg text-ink/80 mb-4">"{{ currentSentenceTask.chineseExample }}"</div>
                        <div class="text-xs font-bold uppercase tracking-widest text-[#D4B483]">主题: {{ currentSentenceTask.theme }}</div>
                     </div>
                  </div>
                  
                  <div>
                    <textarea 
                      v-model="userSentence" 
                      rows="4" 
                      class="w-full bg-white border border-[#E5E5E0] p-4 font-serif text-lg outline-none focus:border-ink transition-colors resize-none placeholder:text-ink/20 placeholder:italic"
                      placeholder="用该词造句..."
                      :disabled="!!feedback && remainingAttempts === 0"
                    ></textarea>
                     <div class="flex justify-between mt-2 text-[10px] font-bold uppercase tracking-widest text-ink/40">
                        <span>剩余次数: {{ remainingAttempts }}</span> 
                        <span v-if="userSentence.length > 0">正在输入</span>
                     </div>
                  </div>
                  
                  <button 
                    v-if="!feedback || remainingAttempts > 0"
                    :disabled="submittingSentence || remainingAttempts === 0"
                    @click="feedback ? retryCurrentWord() : submitUserSentence()"
                    class="w-full py-4 bg-ink text-white font-bold uppercase tracking-widest hover:bg-ink/90 transition-all disabled:opacity-50"
                  >
                    {{ submittingSentence ? '分析中...' : feedback ? '重试' : '提交检查' }}
                  </button>
                  
                  <!-- Feedback -->
                  <div v-if="feedback" class="animate-in fade-in slide-in-from-bottom-2 duration-300">
                     <div class="p-6 border" :class="feedback.score >= 80 ? 'bg-green-50/30 border-green-200' : 'bg-amber-50/30 border-amber-200'">
                        <div class="flex items-center gap-3 mb-4">
                           <span class="text-4xl font-serif font-bold">{{ feedback.score }}</span>
                           <span class="text-xs font-bold uppercase tracking-widest">{{ feedback.isCorrect ? '优秀' : '需改进' }}</span>
                        </div>
                        <div class="space-y-2 text-sm text-ink/70 font-medium">
                           <p><span class="font-bold text-ink">语法:</span> {{ feedback.feedback.grammar }}</p>
                           <p><span class="font-bold text-ink">用法:</span> {{ feedback.feedback.usage }}</p>
                           <p><span class="font-bold text-ink">建议:</span> {{ feedback.feedback.suggestion }}</p>
                        </div>
                     </div>
                  </div>
                  
                  <button 
                    v-if="bestScores.size === vocabularyItems.length"
                    @click="finishLearning"
                    class="w-full py-4 border-2 border-ink bg-white text-ink font-bold uppercase tracking-widest hover:bg-ink hover:text-white transition-all"
                  >
                    完成今日学习
                  </button>
               </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== COMPLETE PHASE ========== -->
    <div v-else-if="phase === 'complete'" class="max-w-2xl mx-auto py-20 text-center">
       <div class="w-20 h-20 bg-[#F9F9F7] rounded-full flex items-center justify-center mx-auto mb-8 border border-[#E5E5E0]">
          <svg class="w-8 h-8 text-ink" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
       </div>
       <h1 class="text-5xl font-serif font-bold mb-6 italic">Session Concluded</h1>
       <p class="text-lg text-ink/60 font-light mb-12">"Language is the road map of a culture. It tells you where its people come from and where they are going."</p>
       
       <div class="flex justify-center gap-6">
          <router-link to="/" class="px-8 py-4 border border-[#E5E5E0] text-ink font-bold uppercase tracking-widest text-sm hover:border-ink transition-colors">
            返回首页
          </router-link>
          <button @click="resetLearning" class="px-8 py-4 bg-ink text-white font-bold uppercase tracking-widest text-sm hover:bg-ink/90 transition-colors">
            开始新学习
          </button>
       </div>
    </div>
  </div>
</template>

<style scoped>
.highlight-word {
  display: inline;
}
</style>
