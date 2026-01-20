<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  getLearningWords, 
  batchAddToVocabulary,
  generateArticle, 
  submitSentence,
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

// 获取当前单词的最高分
const currentBestScore = computed(() => {
  if (!currentVocabulary.value) return null
  return bestScores.value.get(currentVocabulary.value.id) || null
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
  try {
    const res = await getLearningWords(20)
    if (res.code === 200) {
      allWords.value = res.data.words
      currentWordIndex.value = 0
      if (allWords.value.length > 0) {
        generateQuizOptions()
      }
    }
  } catch (error) {
    ElMessage.error('加载单词失败')
  } finally {
    loadingWords.value = false
  }
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
  }
  
  if (selectedWords.value.length >= 5) {
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
    // generateQuizOptions 由 watch 触发
  } else {
    if (selectedWords.value.length >= 5) {
      startStudyPhase()
    } else {
      ElMessage.warning(`需要至少 5 个单词，当前 ${selectedWords.value.length} 个`)
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
  // showMeaning.value = false
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
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- 顶部设置栏 -->
    <LearningSettingsBar v-model="learningSettings" />

    <!-- ========== 选词阶段 ========== -->
    <div v-if="phase === 'quiz'" class="space-y-8">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-black text-slate-800 tracking-tight">单词探索</h1>
          <p class="text-slate-500 font-medium">点击查看含义，选择你想学习的生词</p>
        </div>
        <div class="flex items-center gap-6">
          <label class="flex items-center gap-2 text-sm font-bold text-slate-400 cursor-pointer hover:text-blue-500 transition-colors">
            <input type="checkbox" v-model="autoSpeak" class="w-4 h-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500">
            自动朗读
          </label>
          <div class="px-4 py-2 rounded-2xl bg-blue-50 border border-blue-100 flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-blue-500"></span>
            <span class="text-sm font-bold text-blue-700">已选 {{ selectedWords.length }} / 5</span>
          </div>
        </div>
      </div>
      
      <!-- 候选单词预览 -->
      <div v-if="selectedWords.length > 0" class="flex flex-wrap gap-3">
        <span 
          v-for="w in selectedWords" 
          :key="w.id"
          class="px-4 py-1.5 bg-white border border-blue-100 text-blue-600 rounded-2xl text-sm font-bold shadow-sm"
        >
          {{ w.word }}
        </span>
      </div>
      
      <!-- 单词卡片 -->
      <div v-if="currentWord" v-loading="loadingWords" class="relative group max-w-xl mx-auto">
        <div class="absolute -inset-1 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-[2rem] blur opacity-10 group-hover:opacity-20 transition duration-1000"></div>
        
        <div class="relative bg-white rounded-[2rem] border border-slate-100 p-8 shadow-sm">
          <div class="text-center mb-8">
            <div class="inline-flex items-center gap-3 mb-2">
              <span class="text-4xl font-black text-slate-800 tracking-tighter">{{ currentWord.word }}</span>
              <button 
                @click="speakWord(currentWord.word)"
                class="w-8 h-8 rounded-full bg-slate-50 flex items-center justify-center text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition-all duration-300"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/>
                </svg>
              </button>
            </div>
            <div class="text-lg font-medium text-slate-400 font-serif italic">/ {{ currentWord.phonetic }} /</div>
          </div>
          
          
          <div class="flex flex-col gap-3 mb-8">
            <button
              v-for="(option, idx) in quizOptions"
              :key="idx"
              @click="selectOption(option)"
              :disabled="showResult"
              class="group relative p-4 rounded-[1rem] border-2 text-left transition-all duration-300 overflow-hidden"
              :class="[
                showResult && option === currentWord.meaningCn
                  ? 'border-emerald-500 bg-emerald-50/50 text-emerald-700 font-bold'
                  : showResult && option === selectedOption && !isCorrect
                    ? 'border-red-500 bg-red-50/50 text-red-700 font-bold'
                    : selectedOption === option
                      ? 'border-blue-500 bg-blue-50/50'
                      : 'border-slate-50 hover:border-blue-200 hover:bg-slate-50'
              ]"
            >
              <div class="flex items-center gap-3">
                <div class="w-6 h-6 rounded-md border border-current flex items-center justify-center text-xs font-bold opacity-30">
                  {{ String.fromCharCode(65 + idx) }}
                </div>
                <span class="text-base font-bold tracking-tight">{{ option }}</span>
              </div>
            </button>
          </div>
          
          <!-- 显示答案按钮 (未答题时显示) -->
          <div v-if="!showResult" class="text-center">
            <button 
              @click="giveUp"
              class="text-sm font-bold text-slate-400 hover:text-slate-600 transition-colors"
            >
              不知道？查看答案
            </button>
          </div>
          
          <div v-if="showResult" class="flex flex-col items-center animate-in fade-in slide-in-from-bottom-2 duration-300">
            <!-- 答案解析 -->
            <div class="text-center mb-6 p-4 bg-slate-50 rounded-xl w-full">
               <div class="text-xl font-bold text-slate-700">{{ currentWord.meaningCn }}</div>
               <div v-if="currentWord.exampleSentence" class="mt-2 text-sm text-slate-500 italic">
                 "{{ currentWord.exampleSentence }}"
               </div>
            </div>
             
             <div class="flex gap-4 w-full">
               <button 
                 v-if="isCorrect && !givenUp"
                 @click="skipWord" 
                 class="flex-1 py-3 px-4 rounded-xl border border-slate-200 font-bold text-slate-400 hover:bg-slate-50 transition-all text-sm"
               >
                 跳过
               </button>
               <button 
                 @click="addWord" 
                 class="flex-[2] py-3 px-4 rounded-xl bg-blue-600 text-white font-bold shadow-md hover:bg-blue-700 hover:-translate-y-0.5 transition-all outline-none text-sm"
               >
                 {{ (isCorrect && !givenUp) ? '加入生词本' : '强制加入' }}
               </button>
             </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 学习阶段（文章+题目+造句一体化） ========== -->
    <div v-else-if="phase === 'study'" class="space-y-6">
      <div v-if="loadingArticle" class="bg-white rounded-[2.5rem] border border-slate-100 p-20 text-center shadow-sm">
        <div class="relative w-16 h-16 mx-auto mb-8">
          <div class="absolute inset-0 border-4 border-blue-50 rounded-full"></div>
          <div class="absolute inset-0 border-4 border-blue-600 rounded-full border-t-transparent animate-spin"></div>
        </div>
        <div class="text-xl font-bold text-slate-400">正在雕琢专属文章...</div>
      </div>
      
      <template v-else-if="currentArticle">
        <!-- 顶部导航栏 -->
        <div class="flex items-center justify-between bg-white rounded-2xl border border-slate-100 p-4 shadow-sm">
          <div class="flex items-center gap-4">
            <button @click="resetLearning" class="text-slate-400 hover:text-slate-600 transition-colors">
              ← 退出
            </button>
            <h1 class="text-xl font-black text-slate-800">每日学习</h1>
          </div>
          <div class="text-sm font-bold text-slate-400">
            造句进度: {{ bestScores.size }} / {{ vocabularyItems.length }}
          </div>
        </div>

        <!-- 主内容区域：左右两栏布局 -->
        <div class="grid grid-cols-1 lg:grid-cols-5 gap-6">
          <!-- 左侧：文章 + 阅读理解 -->
          <div class="lg:col-span-3 space-y-6">
            <!-- 文章卡片 -->
            <div class="bg-white rounded-3xl border border-slate-100 p-8 shadow-sm">
              <!-- 文章头部 -->
              <div class="flex items-center justify-between mb-6">
                <div class="flex items-center gap-3">
                  <span class="px-3 py-1 bg-blue-100 text-blue-700 text-xs font-bold rounded-lg uppercase">
                    {{ learningSettings.theme }}
                  </span>
                  <span class="text-xs text-slate-400 font-medium">CET-4</span>
                </div>
                <div class="flex items-center gap-4">
                  <!-- 朗读文章按钮 -->
                  <button 
                    @click="speakWord(currentArticle.content.replace(/\*\*/g, ''))"
                    class="flex items-center gap-2 text-sm font-bold text-slate-400 hover:text-blue-500 transition-colors"
                  >
                     <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/>
                     </svg>
                     朗读全文
                  </button>
                </div>
              </div>
              
              <!-- 文章标题 -->
              <h2 class="text-2xl font-black text-slate-900 mb-6">{{ currentArticle.title }}</h2>
              
              <!-- 文章内容 -->
              <div class="text-lg text-slate-700 leading-relaxed font-serif">
                <template v-for="(seg, idx) in articleSegments" :key="idx">
                  <el-popover
                    v-if="seg.isHighlight"
                    placement="top"
                    :width="220"
                    trigger="hover"
                  >
                    <template #reference>
                      <span class="highlight-word px-1 py-0.5 rounded-md text-amber-700 font-bold bg-amber-100 cursor-pointer hover:bg-amber-200 transition-colors">
                        {{ seg.text }}
                      </span>
                    </template>
                    <div class="text-center space-y-2 p-2">
                      <div class="font-black text-slate-800 text-lg">{{ seg.text }}</div>
                      <div class="text-slate-600 font-medium">{{ seg.meaning }}</div>
                      <button 
                        @click="speakWord(seg.text)"
                        class="inline-flex items-center gap-1 text-xs font-bold text-blue-500 hover:text-blue-600"
                      >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/>
                        </svg>
                        朗读
                      </button>
                    </div>
                  </el-popover>
                  <span v-else>{{ seg.text }}</span>
                </template>
              </div>

              <!-- 翻译区域 -->
              <div v-if="currentArticle.chineseTranslation" class="mt-8 pt-6 border-t border-slate-100">
                <button 
                  @click="showTranslation = !showTranslation"
                  class="w-full flex items-center justify-between text-slate-400 hover:text-blue-600 transition-colors group"
                >
                  <span class="text-sm font-bold uppercase tracking-widest">中文翻译</span>
                  <svg 
                    class="w-5 h-5 transform transition-transform duration-300"
                    :class="showTranslation ? 'rotate-180' : ''"
                    fill="none" 
                    stroke="currentColor" 
                    viewBox="0 0 24 24"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                  </svg>
                </button>
                
                <div 
                  v-show="showTranslation"
                  class="mt-4 text-base text-slate-600 leading-relaxed font-sans bg-slate-50 p-6 rounded-2xl animate-in slide-in-from-top-2 duration-300"
                >
                  {{ currentArticle.chineseTranslation }}
                </div>
              </div>
            </div>
            
            <!-- 阅读理解题目 -->
            <div v-if="currentArticle.comprehensionQuestions && currentArticle.comprehensionQuestions.length > 0" class="bg-white rounded-3xl border border-slate-100 p-8 shadow-sm">
              <ComprehensionQuiz 
                :questions="currentArticle.comprehensionQuestions"
                @complete="() => {}"
              />
            </div>
          </div>
          
          <!-- 右侧：造句练习 -->
          <div class="lg:col-span-2">
            <div class="bg-white rounded-3xl border border-slate-100 p-6 shadow-sm sticky top-6">
              <div class="flex items-center justify-between mb-6">
                <h3 class="text-lg font-black text-slate-800 flex items-center gap-2">
                  ✏️ 造句练习
                </h3>
                <span class="text-sm font-bold text-slate-400">
                  {{ selectedSentenceWordIndex + 1 }} / {{ vocabularyItems.length }}
                </span>
              </div>
              
              <!-- 单词选择器 -->
              <div class="flex flex-wrap gap-2 mb-6">
                <button
                  v-for="(vocab, idx) in vocabularyItems"
                  :key="vocab.id"
                  @click="selectWordForSentence(idx)"
                  :class="[
                    'px-3 py-1.5 rounded-full text-sm font-bold transition-all',
                    selectedSentenceWordIndex === idx
                      ? 'bg-blue-600 text-white shadow-md'
                      : bestScores.has(vocab.id)
                        ? 'bg-emerald-100 text-emerald-700 border border-emerald-200'
                        : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  ]"
                >
                  {{ vocab.word?.word }}
                  <span v-if="bestScores.has(vocab.id)" class="ml-1">✓</span>
                </button>
              </div>
              
              <!-- 当前造句任务 -->
              <div v-if="currentVocabulary" class="space-y-4">
                <div class="p-4 bg-violet-50 rounded-2xl border border-violet-100">
                  <div class="text-xs font-bold text-violet-500 uppercase tracking-widest mb-2">🎯 题目要求</div>
                  <div class="text-sm font-bold text-slate-600 mb-1">目标单词:</div>
                  <div class="text-xl font-black text-violet-700">{{ currentVocabulary.word?.word }}</div>
                  
                  <div v-if="currentSentenceTask" class="mt-3">
                    <div class="text-sm font-bold text-slate-600 mb-1">主题:</div>
                    <div class="text-base text-slate-700">{{ currentSentenceTask.theme }}</div>
                    
                    <div class="mt-3 p-3 bg-white rounded-xl border border-violet-100">
                      <div class="text-xs text-slate-400 mb-1">参考示例:</div>
                      <div class="text-sm text-slate-700 italic">{{ currentSentenceTask.chineseExample }}</div>
                    </div>
                  </div>
                  
                  <!-- 最高分显示 -->
                  <div v-if="currentBestScore !== null" class="mt-3 flex items-center gap-2">
                    <span class="text-xs font-bold text-emerald-600">🏆 最高分:</span>
                    <span class="px-2 py-0.5 bg-emerald-100 text-emerald-700 rounded-full text-sm font-black">{{ currentBestScore }}</span>
                  </div>
                </div>
                
                <!-- 输入区域 -->
                <div>
                  <div class="flex items-center justify-between mb-2">
                    <label class="text-sm font-bold text-slate-500">你的句子:</label>
                    <span class="text-xs font-bold" :class="remainingAttempts > 1 ? 'text-blue-500' : 'text-red-500'">
                      剩余机会: {{ remainingAttempts }} / {{ maxAttempts }}
                    </span>
                  </div>
                  <textarea
                    v-model="userSentence"
                    rows="3"
                    class="w-full p-4 text-base rounded-xl border-2 border-slate-100 bg-slate-50 focus:bg-white focus:border-blue-500/30 focus:ring-4 focus:ring-blue-500/10 transition-all outline-none resize-none"
                    placeholder="在此输入你的英文句子..."
                    :disabled="!!feedback && remainingAttempts === 0"
                  ></textarea>
                </div>
                
                <!-- 提交按钮 -->
                <button 
                  v-if="!feedback || remainingAttempts > 0"
                  :disabled="submittingSentence || remainingAttempts === 0"
                  class="w-full py-3 rounded-xl bg-blue-600 text-white font-bold shadow-lg shadow-blue-500/20 hover:bg-blue-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                  @click="feedback ? retryCurrentWord() : submitUserSentence()"
                >
                  <span v-if="submittingSentence">AI 批改中...</span>
                  <span v-else-if="feedback">🔄 重新作答 ({{ remainingAttempts }}次机会)</span>
                  <span v-else>提交批改</span>
                </button>
                
                <!-- 反馈结果 -->
                <div v-if="feedback" class="p-4 rounded-2xl border space-y-3" :class="feedback.score >= 80 ? 'bg-emerald-50 border-emerald-200' : 'bg-amber-50 border-amber-200'">
                  <div class="flex items-center gap-3">
                    <span class="text-3xl font-black" :class="feedback.score >= 80 ? 'text-emerald-600' : 'text-amber-600'">{{ feedback.score }}</span>
                    <span class="text-sm font-bold" :class="feedback.isCorrect ? 'text-emerald-600' : 'text-amber-600'">
                      {{ feedback.isCorrect ? '优秀！' : '继续加油' }}
                    </span>
                  </div>
                  
                  <div class="space-y-2 text-sm">
                    <div>
                      <span class="font-bold text-slate-500">语法:</span>
                      <span class="text-slate-700 ml-1">{{ feedback.feedback.grammar }}</span>
                    </div>
                    <div>
                      <span class="font-bold text-slate-500">用法:</span>
                      <span class="text-slate-700 ml-1">{{ feedback.feedback.usage }}</span>
                    </div>
                    <div>
                      <span class="font-bold text-slate-500">建议:</span>
                      <span class="text-slate-700 ml-1">{{ feedback.feedback.suggestion }}</span>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 完成按钮 -->
              <button 
                v-if="bestScores.size === vocabularyItems.length"
                @click="finishLearning"
                class="w-full mt-6 py-4 rounded-xl bg-emerald-600 text-white font-black text-lg shadow-lg hover:bg-emerald-700 transition-all"
              >
                🎉 完成学习
              </button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- ========== 完成阶段 ========== -->
    <div v-else-if="phase === 'complete'" class="text-center py-20 animate-in zoom-in-95 duration-1000">
      <div class="w-32 h-32 bg-emerald-50 rounded-full flex items-center justify-center mx-auto mb-10">
        <svg class="w-16 h-16 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
        </svg>
      </div>
      <h1 class="text-6xl font-black text-slate-900 mb-4 tracking-tighter">卓越表现!</h1>
      <p class="text-2xl text-slate-400 font-medium mb-16">又一次完成了深度的语言进化</p>
      
      <div class="flex justify-center gap-6">
        <router-link to="/" class="py-5 px-12 rounded-3xl bg-slate-50 text-slate-800 font-black text-xl hover:bg-slate-100 transition-all">
          返回控制台
        </router-link>
        <button @click="resetLearning" class="py-5 px-12 rounded-3xl bg-blue-600 text-white font-black text-xl shadow-2xl shadow-blue-600/30 hover:bg-blue-700 transition-all hover:-translate-y-1">
          继续进阶
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
