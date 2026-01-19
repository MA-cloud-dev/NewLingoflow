<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  getLearningWords, 
  addToVocabulary, 
  getUserVocabulary,
  generateArticle, 
  submitSentence,
  type Word, 
  type VocabularyItem,
  type ArticleData,
  type SentenceFeedback
} from '@/api/learning'
import LearningCustomizer, { type LearningSettings } from '@/components/LearningCustomizer.vue'
import ArticleReader from '@/components/ArticleReader.vue'
import ComprehensionQuiz from '@/components/ComprehensionQuiz.vue'

// 学习阶段
type LearningPhase = 'quiz' | 'article' | 'comprehension' | 'sentence' | 'complete'
const phase = ref<LearningPhase>('quiz')
const showCustomizer = ref(false)
const learningSettings = ref<LearningSettings | null>(null)

// ========== 选词阶段 (Quiz) ==========
const allWords = ref<Word[]>([])
const currentWordIndex = ref(0)
const selectedWords = ref<Word[]>([]) // 候选单词列表
const loadingWords = ref(false)

// 当前单词和选项
const currentWord = computed(() => allWords.value[currentWordIndex.value])
const quizOptions = ref<string[]>([])
const selectedOption = ref<string | null>(null)
const isCorrect = ref<boolean | null>(null)
const showResult = ref(false)

// 朗读设置
const autoSpeak = ref(true) // 被动朗读开关

// ========== 文章阶段 ==========
const currentArticle = ref<ArticleData | null>(null)
const sessionId = ref<number | null>(null)
const vocabularyItems = ref<VocabularyItem[]>([])
const loadingArticle = ref(false)

// ========== 造句阶段 ==========
const sentenceWordIndex = ref(0)
const userSentence = ref('')
const feedback = ref<SentenceFeedback | null>(null)
const submittingSentence = ref(false)

const currentVocabulary = computed(() => vocabularyItems.value[sentenceWordIndex.value])

const currentSentenceTask = computed(() => {
  if (!currentArticle.value?.sentenceMakingTasks) return null
  const wordStr = currentVocabulary.value?.word?.word
  return currentArticle.value.sentenceMakingTasks.find(t => t.word === wordStr)
})

const showHint = ref(false)

// ========== 朗读功能 ==========
function speakWord(word: string) {
  if ('speechSynthesis' in window) {
    const utterance = new SpeechSynthesisUtterance(word)
    utterance.lang = 'en-US'
    utterance.rate = 0.8
    speechSynthesis.speak(utterance)
  }
}

// 监听当前单词变化，自动朗读
watch(currentWord, (word) => {
  if (word && autoSpeak.value && phase.value === 'quiz') {
    setTimeout(() => speakWord(word.word), 300)
  }
})

// ========== 选词逻辑 ==========
async function loadWords() {
  loadingWords.value = true
  try {
    const res = await getLearningWords(20) // 加载更多单词用于选择
    if (res.code === 200) {
      allWords.value = res.data.words
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
}

function selectOption(option: string) {
  if (showResult.value) return // 已经选过了
  
  selectedOption.value = option
  isCorrect.value = option === currentWord.value.meaningCn
  showResult.value = true
}

function skipWord() {
  nextWord()
}

async function addWord() {
  if (!currentWord.value) return
  
  // 添加到候选列表
  if (!selectedWords.value.find((w: Word) => w.id === currentWord.value.id)) {
    selectedWords.value.push(currentWord.value)
    ElMessage.success(`已添加「${currentWord.value.word}」(${selectedWords.value.length}/5)`)
  }
  
  // 检查是否收集够 5 个
  if (selectedWords.value.length >= 5) {
    await startArticlePhase()
  } else {
    nextWord()
  }
}

function nextWord() {
  if (currentWordIndex.value < allWords.value.length - 1) {
    currentWordIndex.value++
    generateQuizOptions()
  } else {
    // 单词用完了，检查候选数量
    if (selectedWords.value.length >= 5) {
      startArticlePhase()
    } else {
      ElMessage.warning(`需要至少 5 个单词，当前 ${selectedWords.value.length} 个`)
      // 重新加载单词
      loadWords()
    }
  }
}

// ========== 文章阶段 ==========
async function startArticlePhase() {
  if (selectedWords.value.length === 0) {
    ElMessage.warning('请先选择单词')
    return
  }
  
  if (!learningSettings.value) {
    ElMessage.error('缺少定制配置')
    return
  }
  
  loadingArticle.value = true
  phase.value = 'article'
  
  try {
    // 先将选中的单词加入生词本
    for (const word of selectedWords.value) {
      try {
        await addToVocabulary(word.id)
      } catch {
        // 可能已经在生词本中
      }
    }
    
    // 获取用户生词本
    const vocabRes = await getUserVocabulary('all', 1, 100)
    if (vocabRes.code === 200) {
      vocabularyItems.value = vocabRes.data.vocabulary.filter((v: VocabularyItem) => 
        selectedWords.value.some((sw: Word) => sw.id === v.wordId)
      )
    }
    
    if (vocabularyItems.value.length === 0) {
      ElMessage.error('未找到对应的生词本记录')
      return
    }
    
    // 生成 AI 文章
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

async function handleConfirmSettings(settings: LearningSettings) {
  learningSettings.value = settings
  showCustomizer.value = false
  await loadWords()
  phase.value = 'quiz'
}

// function startComprehensionPhase() {
//   // Merged into article phase
// }

function startSentencePractice() {
  sentenceWordIndex.value = 0
  userSentence.value = ''
  feedback.value = null
  showHint.value = false
  phase.value = 'sentence'
}

async function submitUserSentence() {
  if (!userSentence.value.trim()) {
    ElMessage.warning('请输入句子')
    return
  }
  
  if (!currentVocabulary.value || !sessionId.value) return
  
  submittingSentence.value = true
  try {
    const res = await submitSentence(
      sessionId.value,
      currentVocabulary.value.id,
      userSentence.value
    )
    
    if (res.code === 200) {
      feedback.value = res.data
    }
  } catch (error: any) {
    ElMessage.error(error.response?.data?.msg || '提交失败')
  } finally {
    submittingSentence.value = false
  }
}

function nextSentenceWord() {
  if (sentenceWordIndex.value < vocabularyItems.value.length - 1) {
    sentenceWordIndex.value++
    userSentence.value = ''
    feedback.value = null
    showHint.value = false
  } else {
    phase.value = 'complete'
  }
}

function resetLearning() {
  phase.value = 'quiz'
  currentWordIndex.value = 0
  selectedWords.value = []
  currentArticle.value = null
  sessionId.value = null
  vocabularyItems.value = []
  sentenceWordIndex.value = 0
  userSentence.value = ''
  feedback.value = null
  loadWords()
}

onMounted(() => {
  showCustomizer.value = true
})
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <!-- 定制弹窗 -->
    <div v-if="showCustomizer" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-300">
      <LearningCustomizer @confirm="handleConfirmSettings" />
    </div>

    <!-- ========== 选词阶段 ========== -->
    <div v-if="phase === 'quiz'" class="space-y-8">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-black text-slate-800 tracking-tight">单词探索</h1>
          <p class="text-slate-500 font-medium">通过测试筛选你想要深入学习的生词</p>
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
          class="px-4 py-1.5 bg-white border border-blue-100 text-blue-600 rounded-2xl text-sm font-bold shadow-sm animate-in fade-in zoom-in duration-300"
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
          
          <div v-if="showResult" class="flex flex-col items-center animate-in fade-in slide-in-from-bottom-2 duration-300">
             <!-- Result message removed to save space or kept minimal -->
             <div class="flex gap-4 w-full">
               <button 
                 v-if="isCorrect"
                 @click="skipWord" 
                 class="flex-1 py-3 px-4 rounded-xl border border-slate-200 font-bold text-slate-400 hover:bg-slate-50 transition-all text-sm"
               >
                 跳过已会
               </button>
               <button 
                 @click="addWord" 
                 class="flex-[2] py-3 px-4 rounded-xl bg-blue-600 text-white font-bold shadow-md hover:bg-blue-700 hover:-translate-y-0.5 transition-all outline-none text-sm"
               >
                 {{ isCorrect ? '加入生词本' : '强制加入' }}
               </button>
             </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 文章阅读阶段 ========== -->
    <!-- ========== 文章阅读 & 阅读理解一体化阶段 ========== -->
    <div v-else-if="phase === 'article'" class="space-y-8 max-w-4xl mx-auto">
      <div v-if="loadingArticle" class="bg-white rounded-[2.5rem] border border-slate-100 p-20 text-center shadow-sm">
        <div class="relative w-16 h-16 mx-auto mb-8">
          <div class="absolute inset-0 border-4 border-blue-50 rounded-full"></div>
          <div class="absolute inset-0 border-4 border-blue-600 rounded-full border-t-transparent animate-spin"></div>
        </div>
        <div class="text-xl font-bold text-slate-400">正在雕琢专属文章...</div>
      </div>
      
      <template v-else-if="currentArticle">
        <!-- 文章区：限制高度，紧凑布局 -->
        <ArticleReader 
          :title="currentArticle.title"
          :content="currentArticle.content"
          @complete="() => {}" 
        />
        
        <!-- 阅读理解区：位于文章下方 -->
        <div v-if="currentArticle.comprehensionQuestions && currentArticle.comprehensionQuestions.length > 0" class="pt-8 border-t border-slate-100">
           <ComprehensionQuiz 
            :questions="currentArticle.comprehensionQuestions"
            @complete="startSentencePractice"
          />
        </div>
        
        <div v-else class="flex justify-center pt-8">
             <button @click="startSentencePractice" class="btn-primary">Start Practice</button>
        </div>
      </template>
    </div>

    <!-- Phase comprehension removed, merged above -->

    <!-- ========== 造句练习阶段 ========== -->
    <div v-else-if="phase === 'sentence' && currentVocabulary" class="space-y-10 max-w-2xl mx-auto">
      <div class="flex items-center justify-between px-2">
        <h1 class="text-3xl font-black text-slate-800 tracking-tight">造句演练</h1>
        <div class="px-5 py-2 rounded-full bg-slate-100 text-slate-500 font-bold text-xs tracking-widest">
          {{ sentenceWordIndex + 1 }} / {{ vocabularyItems.length }}
        </div>
      </div>
      
      <div class="bg-white p-8 rounded-[2.5rem] border border-slate-100 shadow-sm space-y-6">
        <div class="text-center p-6 bg-slate-50 rounded-[2rem]">
          <h2 class="text-4xl font-black text-blue-600 mb-2 truncate">{{ currentVocabulary.word?.word }}</h2>
          <div class="text-slate-400 font-medium italic mb-4">/ {{ currentVocabulary.word?.phonetic }} /</div>
          
          <div v-if="currentSentenceTask" class="mb-4">
            <div class="inline-block px-3 py-1 rounded-lg bg-violet-100 text-violet-700 font-bold text-xs mb-3">
              Theme: {{ currentSentenceTask.theme }}
            </div>
            
            <!-- 直接显示例句 -->
            <div class="animate-in fade-in duration-500 space-y-3">
              <div class="text-lg font-medium text-slate-800 bg-white p-4 rounded-xl shadow-sm border border-slate-100 italic leading-relaxed">
                "{{ currentSentenceTask.chineseExample }}"
              </div>
              <!-- 释义隐藏，可点击查看 -->
              <button 
                v-if="!showHint"
                @click="showHint = true" 
                class="text-xs font-bold text-slate-300 hover:text-blue-500 transition-colors uppercase tracking-widest"
              >
                Show Definition
              </button>
              <div v-else class="text-base font-bold text-slate-500 animate-in fade-in">
                {{ currentVocabulary.word?.meaningCn }}
              </div>
            </div>
          </div>
          
          <div v-else class="text-xl font-bold text-slate-700">{{ currentVocabulary.word?.meaningCn }}</div>
        </div>
        
        <div class="space-y-4">
          <textarea
            v-model="userSentence"
            rows="3"
            class="w-full p-6 text-xl rounded-[1.5rem] border-2 border-slate-50 bg-slate-50 focus:bg-white focus:border-blue-500/20 focus:ring-[8px] focus:ring-blue-500/5 transition-all outline-none resize-none font-serif placeholder:text-slate-200"
            placeholder="Build a creative sentence..."
            :disabled="!!feedback"
          ></textarea>
        </div>
        
        <button 
          v-if="!feedback"
          :disabled="submittingSentence"
          class="w-full py-4 rounded-xl bg-blue-600 text-white font-black text-lg shadow-lg shadow-blue-500/20 hover:bg-blue-700 transition-all"
          @click="submitUserSentence"
        >
          <span v-if="submittingSentence" class="flex items-center justify-center gap-2">
            AI 批改中...
          </span>
          <span v-else>请求 AI 批改</span>
        </button>
      </div>
      
      <div v-if="feedback" class="bg-white p-12 rounded-[3rem] border border-slate-100 shadow-2xl space-y-10 animate-in slide-in-from-bottom-8 duration-500">
        <div class="flex items-center gap-8 border-b border-slate-50 pb-10">
          <div class="text-6xl font-black" :class="feedback.score >= 80 ? 'text-emerald-500' : 'text-amber-500'">{{ feedback.score }}</div>
          <div>
            <div class="text-xs font-black text-slate-300 uppercase tracking-widest mb-1">Score Result</div>
            <div class="text-2xl font-black uppercase tracking-tight" :class="feedback.isCorrect ? 'text-emerald-600' : 'text-amber-600'">
              {{ feedback.isCorrect ? 'Perfect' : 'Improve' }}
            </div>
          </div>
        </div>
        
        <div class="grid gap-8">
          <div v-for="(val, key) in { 'Grammar': feedback.feedback.grammar, 'Usage': feedback.feedback.usage, 'Suggestion': feedback.feedback.suggestion }" :key="key">
            <div class="text-[10px] font-black text-slate-300 uppercase tracking-widest mb-2 px-1">{{ key }}</div>
            <p class="text-slate-600 text-lg font-medium leading-relaxed">{{ val }}</p>
          </div>
        </div>
        
        <button 
          class="w-full py-5 rounded-2xl bg-[#0F172A] text-white font-black text-xl hover:bg-slate-900 transition-all flex items-center justify-center gap-3 group"
          @click="nextSentenceWord"
        >
          {{ sentenceWordIndex < vocabularyItems.length - 1 ? '继续下一个' : '查看总结' }}
          <svg class="w-6 h-6 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M14 5l7 7m0 0l-7 7m7-7H3"/>
          </svg>
        </button>
      </div>
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
