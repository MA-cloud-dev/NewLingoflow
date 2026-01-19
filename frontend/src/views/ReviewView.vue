<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  getReviewQueue, 
  submitRating, 
  submitTestAnswer,
  type ReviewWord,
  type TestQuestion
} from '@/api/learning'

// 复习阶段
type ReviewPhase = 'loading' | 'rating' | 'test' | 'result' | 'complete'
const phase = ref<ReviewPhase>('loading')

// 复习队列
const reviewQueue = ref<ReviewWord[]>([])
const currentIndex = ref(0)
const currentWord = computed(() => reviewQueue.value[currentIndex.value])

// 错误队列 (vocabularyId 列表)
const errorQueue = ref<number[]>([])
const isInErrorQueue = ref(false)

// 测试题目
const testQuestion = ref<TestQuestion | null>(null)
const selectedOption = ref<string | null>(null)
const testResult = ref<{ isCorrect: boolean, correctAnswer: string } | null>(null)
const testStartTime = ref(0)

// 统计
const stats = ref({ correct: 0, wrong: 0, unknown: 0 })

// 朗读
function speakWord(word: string) {
  if ('speechSynthesis' in window) {
    const utterance = new SpeechSynthesisUtterance(word)
    utterance.lang = 'en-US'
    utterance.rate = 0.8
    speechSynthesis.speak(utterance)
  }
}

// 加载复习队列
async function loadReviewQueue() {
  phase.value = 'loading'
  try {
    const res = await getReviewQueue()
    if (res.code === 200) {
      reviewQueue.value = res.data.words
      if (reviewQueue.value.length > 0) {
        phase.value = 'rating'
        setTimeout(() => speakWord(currentWord.value.word), 300)
      } else {
        phase.value = 'complete'
      }
    }
  } catch (error) {
    ElMessage.error('加载复习队列失败')
  }
}

// 提交自评
async function handleRating(rating: 'known' | 'unknown') {
  if (!currentWord.value) return
  
  try {
    const res = await submitRating(currentWord.value.vocabularyId, rating)
    if (res.code === 200) {
      if (rating === 'unknown') {
        stats.value.unknown++
        errorQueue.value.push(currentWord.value.vocabularyId)
        // 显示答案
        testResult.value = { 
          isCorrect: false, 
          correctAnswer: res.data.correctAnswer || currentWord.value.meaningCn 
        }
        phase.value = 'result'
      } else if (res.data.needTest && res.data.testQuestion) {
        testQuestion.value = res.data.testQuestion
        selectedOption.value = null
        testResult.value = null
        testStartTime.value = Date.now()
        phase.value = 'test'
      } else {
        nextWord()
      }
    }
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

// 提交测试答案
async function handleAnswer(option: string) {
  if (!currentWord.value || selectedOption.value) return
  
  selectedOption.value = option
  const responseTime = Date.now() - testStartTime.value
  
  try {
    const res = await submitTestAnswer(
      currentWord.value.vocabularyId, 
      option, 
      isInErrorQueue.value,
      responseTime
    )
    
    if (res.code === 200) {
      testResult.value = res.data
      
      if (!isInErrorQueue.value) {
        // 正常队列：统计
        if (res.data.isCorrect) {
          stats.value.correct++
        } else {
          stats.value.wrong++
          errorQueue.value.push(currentWord.value.vocabularyId)
        }
      } else {
        // 错误队列：正确则移出
        if (res.data.isCorrect) {
          const idx = errorQueue.value.indexOf(currentWord.value.vocabularyId)
          if (idx > -1) errorQueue.value.splice(idx, 1)
        }
      }
      
      phase.value = 'result'
    }
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

// 下一个单词
function nextWord() {
  testQuestion.value = null
  selectedOption.value = null
  testResult.value = null
  
  if (isInErrorQueue.value) {
    // 错误队列模式
    if (errorQueue.value.length > 0) {
      // 找到错误队列中的下一个单词
      const nextVocabId = errorQueue.value[0]
      const word = reviewQueue.value.find(w => w.vocabularyId === nextVocabId)
      if (word) {
        currentIndex.value = reviewQueue.value.indexOf(word)
        phase.value = 'test'
        // 为错误队列生成测试题
        generateErrorQueueTest()
        setTimeout(() => speakWord(word.word), 300)
        return
      }
    }
    // 错误队列清空
    phase.value = 'complete'
  } else {
    // 正常队列
    if (currentIndex.value < reviewQueue.value.length - 1) {
      currentIndex.value++
      phase.value = 'rating'
      setTimeout(() => speakWord(currentWord.value.word), 300)
    } else {
      // 正常队列完成，检查错误队列
      if (errorQueue.value.length > 0) {
        isInErrorQueue.value = true
        ElMessage.info(`进入错误复习，共 ${errorQueue.value.length} 个单词`)
        nextWord()
      } else {
        phase.value = 'complete'
      }
    }
  }
}

// 为错误队列生成测试题
function generateErrorQueueTest() {
  if (!currentWord.value) return
  
  const correctAnswer = currentWord.value.meaningCn
  const otherWords = reviewQueue.value.filter(w => w.vocabularyId !== currentWord.value.vocabularyId)
  const distractors = otherWords
    .sort(() => Math.random() - 0.5)
    .slice(0, 3)
    .map(w => w.meaningCn)
  
  const options = [correctAnswer, ...distractors].sort(() => Math.random() - 0.5)
  
  testQuestion.value = {
    type: 'choice',
    word: currentWord.value.word,
    phonetic: currentWord.value.phonetic,
    options,
    correctAnswer
  }
}

// 重新开始
function restart() {
  currentIndex.value = 0
  errorQueue.value = []
  isInErrorQueue.value = false
  stats.value = { correct: 0, wrong: 0, unknown: 0 }
  loadReviewQueue()
}

onMounted(() => {
  loadReviewQueue()
})
</script>

<template>
  <div class="max-w-4xl mx-auto">
    <!-- 加载中 -->
    <div v-if="phase === 'loading'" class="py-32 text-center bg-white rounded-[3rem] border border-slate-100 shadow-sm">
      <div class="relative w-16 h-16 mx-auto mb-8">
        <div class="absolute inset-0 border-4 border-blue-50 rounded-full"></div>
        <div class="absolute inset-0 border-4 border-blue-600 rounded-full border-t-transparent animate-spin"></div>
      </div>
      <div class="text-xl font-bold text-slate-400">正在同步学习进度...</div>
    </div>
    
    <!-- 自评阶段 -->
    <div v-else-if="phase === 'rating' && currentWord" class="space-y-10 animate-in fade-in duration-700">
      <div class="flex items-center justify-between px-2">
        <div>
          <h1 class="text-3xl font-black text-slate-800 tracking-tight">今日复习</h1>
          <p class="text-slate-500 font-medium">温故而知新，让我们重温这些词汇</p>
        </div>
        <div class="px-5 py-2 rounded-full bg-blue-50 text-blue-500 font-black text-xs tracking-widest border border-blue-100">
          {{ currentIndex + 1 }} / {{ reviewQueue.length }}
        </div>
      </div>
      
      <div class="relative group">
        <div class="absolute -inset-1 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-[3rem] blur opacity-10 group-hover:opacity-20 transition duration-1000"></div>
        <div class="relative bg-white rounded-[3rem] border border-slate-100 p-16 text-center shadow-xl shadow-slate-200/50">
          <div class="flex flex-col items-center mb-12">
            <div class="inline-flex items-center gap-4 mb-4">
              <span class="text-6xl font-black text-slate-800 tracking-tighter">{{ currentWord.word }}</span>
              <button 
                @click="speakWord(currentWord.word)" 
                class="w-14 h-14 rounded-full bg-slate-50 flex items-center justify-center text-slate-400 hover:text-blue-600 hover:bg-blue-50 transition-all duration-300"
              >
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/>
                </svg>
              </button>
            </div>
            <div class="text-2xl font-medium text-slate-400 font-serif italic mb-8">/ {{ currentWord.phonetic }} /</div>
            <div class="text-xl font-bold text-slate-500 mb-2 uppercase tracking-[0.2em] opacity-30">Do you recall this word?</div>
          </div>
          
          <div class="flex justify-center gap-6">
            <button 
              @click="handleRating('unknown')"
              class="flex-1 py-6 px-10 rounded-2xl border-2 border-slate-100 text-slate-400 font-black text-xl hover:bg-slate-50 hover:border-slate-200 transition-all"
            >
              不认识
            </button>
            <button 
              @click="handleRating('known')"
              class="flex-[2] py-6 px-10 rounded-2xl bg-blue-600 text-white font-black text-xl shadow-xl shadow-blue-500/20 hover:bg-blue-700 hover:-translate-y-1 transition-all"
            >
              认识
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 测试阶段 -->
    <div v-else-if="phase === 'test' && testQuestion" class="space-y-10 animate-in fade-in duration-700">
      <div class="flex items-center justify-between px-2">
        <h1 class="text-3xl font-black text-slate-800 tracking-tight">
          {{ isInErrorQueue ? '错题加固' : '辨析释义' }}
        </h1>
        <div v-if="isInErrorQueue" class="px-4 py-2 rounded-full bg-amber-50 text-amber-600 font-black text-xs tracking-widest border border-amber-100">
          剩余 {{ errorQueue.length }} 个
        </div>
      </div>
      
      <div class="bg-white rounded-[3rem] border border-slate-100 p-16 shadow-xl shadow-slate-200/50">
        <div class="text-center mb-16">
          <div class="inline-flex items-center gap-4 mb-4">
            <span class="text-6xl font-black text-slate-800 tracking-tighter">{{ testQuestion.word }}</span>
            <button @click="speakWord(testQuestion.word)" class="w-14 h-14 rounded-full bg-slate-50 flex items-center justify-center text-slate-400 hover:text-blue-600 transition-all">
              <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/>
              </svg>
            </button>
          </div>
          <div class="text-2xl font-medium text-slate-400 font-serif italic">/ {{ testQuestion.phonetic }} /</div>
        </div>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <button
            v-for="(option, idx) in testQuestion.options"
            :key="idx"
            @click="handleAnswer(option)"
            :disabled="!!selectedOption"
            class="group relative p-8 rounded-[1.5rem] border-2 text-left transition-all duration-300"
            :class="[
              testResult && option === testQuestion.correctAnswer
                ? 'border-emerald-500 bg-emerald-50 text-emerald-700 font-bold'
                : testResult && option === selectedOption && !testResult.isCorrect
                  ? 'border-red-500 bg-red-50 text-red-700 font-bold'
                  : selectedOption === option
                    ? 'border-blue-500 bg-blue-50/50'
                    : 'border-slate-50 hover:border-blue-100 hover:bg-slate-50/50'
            ]"
          >
            <div class="flex items-center gap-4">
              <div class="w-8 h-8 rounded-lg border border-current flex items-center justify-center font-bold opacity-20">
                {{ String.fromCharCode(65 + idx) }}
              </div>
              <span class="text-xl font-bold tracking-tight">{{ option }}</span>
            </div>
          </button>
        </div>
      </div>
    </div>
    
    <!-- 结果阶段 -->
    <div v-else-if="phase === 'result' && currentWord" class="animate-in zoom-in-95 duration-500">
      <div class="bg-white rounded-[3rem] border border-slate-100 p-20 text-center shadow-2xl shadow-slate-200/60 max-w-2xl mx-auto">
        <div class="text-6xl font-black text-slate-800 tracking-tighter mb-2">{{ currentWord.word }}</div>
        <div class="text-2xl font-medium text-slate-300 font-serif italic mb-10">/ {{ currentWord.phonetic }} /</div>
        
        <div v-if="testResult" :class="['text-3xl font-black uppercase tracking-widest mb-8', testResult.isCorrect ? 'text-emerald-500' : 'text-red-500']">
          {{ testResult.isCorrect ? 'Correct!' : 'Incorrect' }}
        </div>
        
        <div class="bg-slate-50 p-8 rounded-[2rem] mb-12">
          <div class="text-[10px] font-black text-slate-300 uppercase tracking-[0.3em] mb-4">Core Definition</div>
          <div class="text-3xl font-bold text-slate-700">{{ testResult?.correctAnswer || currentWord.meaningCn }}</div>
        </div>
        
        <button 
          @click="nextWord"
          class="w-full py-6 rounded-2xl bg-[#0F172A] text-white font-black text-xl hover:bg-slate-900 transition-all flex items-center justify-center gap-3 group"
        >
          继续下一个
          <svg class="w-6 h-6 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M14 5l7 7m0 0l-7 7m7-7H3"/>
          </svg>
        </button>
      </div>
    </div>
    
    <!-- 完成阶段 -->
    <div v-else-if="phase === 'complete'" class="text-center py-20 animate-in zoom-in-95 duration-1000">
      <div class="w-32 h-32 bg-emerald-50 rounded-full flex items-center justify-center mx-auto mb-10">
        <svg class="w-16 h-16 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"/>
        </svg>
      </div>
      <h1 class="text-6xl font-black text-slate-900 mb-4 tracking-tighter">复习达成!</h1>
      <p class="text-2xl text-slate-400 font-medium mb-16">学习就是不断战胜遗忘的过程</p>
      
      <div v-if="reviewQueue.length > 0" class="grid grid-cols-3 gap-6 max-w-lg mx-auto mb-16 px-4">
        <div v-for="s in [
          { label: 'Correct', value: stats.correct, color: 'emerald' },
          { label: 'Wrong', value: stats.wrong, color: 'red' },
          { label: 'Unknow', value: stats.unknown, color: 'slate' }
        ]" :key="s.label" :class="`bg-${s.color}-50 p-6 rounded-[2rem] border border-${s.color}-100 transition-transform hover:scale-105`">
          <div :class="`text-4xl font-black text-${s.color}-500 mb-1`">{{ s.value }}</div>
          <div :class="`text-[10px] font-black text-${s.color}-400 uppercase tracking-widest`">{{ s.label }}</div>
        </div>
      </div>
      
      <div v-if="reviewQueue.length === 0" class="text-xl text-slate-400 mb-12 font-medium">今日暂无复习任务，休息一下吧</div>
      
      <div class="flex justify-center gap-6">
        <router-link to="/" class="py-5 px-12 rounded-3xl bg-slate-50 text-slate-800 font-black text-xl hover:bg-slate-100 transition-all">
          返回控制台
        </router-link>
        <button @click="restart" class="py-5 px-12 rounded-3xl bg-blue-600 text-white font-black text-xl shadow-2xl shadow-blue-600/30 hover:bg-blue-700 transition-all">
          {{ reviewQueue.length === 0 ? '检查更新' : '再次回顾' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 辅助样式 */
.bg-emerald-50 { background-color: rgb(236 253 245); }
.bg-red-50 { background-color: rgb(254 242 242); }
.bg-slate-50 { background-color: rgb(248 250 252); }
.border-emerald-100 { border-color: rgb(209 250 229); }
.border-red-100 { border-color: rgb(254 226 226); }
.text-emerald-500 { color: rgb(16 185 129); }
.text-red-500 { color: rgb(239 68 68); }
</style>

