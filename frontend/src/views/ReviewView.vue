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
async function handleRating(rating: 'known' | 'unknown' | 'fuzzy') {
  if (!currentWord.value) return
  
  try {
    const res = await submitRating(currentWord.value.vocabularyId, rating)
    if (res.code === 200) {
      if (rating === 'unknown' || rating === 'fuzzy') {
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
    console.error('Review submission failed:', error)
    ElMessage.error('Submission Failed. Please try again.')
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
  <div class="max-w-3xl mx-auto py-8">
    <!-- Loading State -->
    <div v-if="phase === 'loading'" class="paper-card p-24 text-center">
      <div class="animate-pulse flex flex-col items-center">
        <div class="w-16 h-16 rounded-full border-4 border-ink/10 border-t-ink animate-spin mb-8"></div>
        <div class="font-serif italic text-xl text-ink/40">Synchronizing memory banks...</div>
      </div>
    </div>
    
    <!-- Rating Phase -->
    <div v-else-if="phase === 'rating' && currentWord" class="space-y-8 animate-in fade-in duration-500">
      <div class="flex items-center justify-between border-b border-[#E5E5E0] pb-4">
        <div>
          <h1 class="text-3xl font-serif font-black text-ink italic">Daily Review</h1>
          <p class="text-ink/40 text-sm font-sans uppercase tracking-widest mt-1">Reinforce your knowledge</p>
        </div>
        <div class="px-4 py-1 border border-ink/20 rounded-full text-xs font-bold uppercase tracking-widest text-ink/60">
          {{ currentIndex + 1 }} / {{ reviewQueue.length }}
        </div>
      </div>
      
      <div class="paper-card p-12 md:p-16 text-center relative group hover:shadow-xl transition-shadow duration-500">
        <div class="flex flex-col items-center mb-12">
          <div class="inline-flex items-center gap-4 mb-6">
            <h2 class="text-6xl font-serif font-black text-ink tracking-tight">{{ currentWord.word }}</h2>
            <button 
              @click="speakWord(currentWord.word)" 
              class="w-12 h-12 rounded-full border border-ink/10 flex items-center justify-center text-ink/40 hover:text-ink hover:border-ink transition-all"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/></svg>
            </button>
          </div>
          <div class="text-2xl font-serif italic text-ink/40">/ {{ currentWord.phonetic }} /</div>
        </div>
        
        <div class="grid grid-cols-3 gap-4 border-t border-ink/5 pt-8">
          <button 
            @click="handleRating('unknown')"
            class="py-4 border border-transparent hover:border-red-200 hover:bg-red-50 text-red-400 font-bold uppercase tracking-widest text-sm transition-all rounded-lg"
          >
            Forgot
          </button>
           <button 
            @click="handleRating('fuzzy')"
            class="py-4 border border-transparent hover:border-amber-200 hover:bg-amber-50 text-amber-400 font-bold uppercase tracking-widest text-sm transition-all rounded-lg"
          >
            Unsure
          </button>
          <button 
            @click="handleRating('known')"
            class="py-4 bg-ink text-white hover:bg-ink/90 font-bold uppercase tracking-widest text-sm transition-all shadow-lg rounded-lg"
          >
            Known
          </button>
        </div>
      </div>
    </div>
    
    <!-- Test Phase -->
    <div v-else-if="phase === 'test' && testQuestion" class="space-y-8 animate-in fade-in duration-500">
      <div class="flex items-center justify-between border-b border-[#E5E5E0] pb-4">
        <h1 class="text-3xl font-serif font-black text-ink italic">
          {{ isInErrorQueue ? 'Reinforcement' : 'Verification' }}
        </h1>
        <div v-if="isInErrorQueue" class="px-4 py-1 bg-amber-50 text-amber-700 border border-amber-100 rounded-full text-xs font-bold uppercase tracking-widest">
          {{ errorQueue.length }} Remaining
        </div>
      </div>
      
      <div class="paper-card p-10 md:p-14">
        <div class="text-center mb-12">
          <div class="inline-flex items-center gap-4 mb-4">
            <span class="text-5xl font-serif font-black text-ink tracking-tight">{{ testQuestion.word }}</span>
            <button @click="speakWord(testQuestion.word)" class="w-10 h-10 rounded-full border border-ink/10 flex items-center justify-center text-ink/40 hover:text-ink hover:border-ink transition-all">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/></svg>
            </button>
          </div>
          <div class="text-xl font-serif italic text-ink/40">/ {{ testQuestion.phonetic }} /</div>
        </div>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <button
            v-for="(option, idx) in testQuestion.options"
            :key="idx"
            @click="handleAnswer(option)"
            :disabled="!!selectedOption"
            class="group relative p-6 border text-left transition-all duration-300 rounded-lg"
            :class="[
              testResult && option === testQuestion.correctAnswer
                ? 'border-green-600 bg-green-50 text-green-800'
                : testResult && option === selectedOption && !testResult.isCorrect
                  ? 'border-red-500 bg-red-50 text-red-800'
                  : selectedOption === option
                    ? 'border-ink bg-ink text-white'
                    : 'border-[#E5E5E0] hover:border-ink/40 hover:bg-[#F9F9F7]'
            ]"
          >
            <div class="flex items-center gap-4">
              <div class="font-serif italic text-ink/30 text-lg group-hover:text-ink/60">
                {{ String.fromCharCode(65 + idx) }}
              </div>
              <span class="text-lg font-medium tracking-wide">{{ option }}</span>
            </div>
          </button>
        </div>
      </div>
    </div>
    
    <!-- Result Phase -->
    <div v-else-if="phase === 'result' && currentWord" class="animate-in zoom-in-95 duration-500 max-w-xl mx-auto">
      <div class="paper-card p-12 text-center">
        <div class="text-5xl font-serif font-black text-ink mb-4 tracking-tight">{{ currentWord.word }}</div>
        <div class="text-xl font-serif italic text-ink/40 mb-8">/ {{ currentWord.phonetic }} /</div>
        
        <div v-if="testResult" class="mb-8">
           <div :class="['text-sm font-black uppercase tracking-[0.3em]', testResult.isCorrect ? 'text-green-600' : 'text-red-500']">
             {{ testResult.isCorrect ? 'Correct' : 'Incorrect' }}
           </div>
        </div>
        
        <div class="bg-[#F9F9F7] p-8 border-l-4 border-ink mb-10 text-left">
          <div class="text-[10px] font-bold text-ink/40 uppercase tracking-[0.2em] mb-2">Definition</div>
          <div class="text-2xl font-serif font-bold text-ink">{{ testResult?.correctAnswer || currentWord.meaningCn }}</div>
        </div>
        
        <button 
          @click="nextWord"
          class="w-full py-4 bg-ink text-white font-bold uppercase tracking-widest text-sm hover:bg-ink/90 transition-all flex items-center justify-center gap-2 group"
        >
          Next Card
          <svg class="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
        </button>
      </div>
    </div>
    
    <!-- Complete Phase -->
    <div v-else-if="phase === 'complete'" class="text-center py-20 max-w-2xl mx-auto">
      <div class="w-24 h-24 bg-[#F9F9F7] rounded-full border border-[#E5E5E0] flex items-center justify-center mx-auto mb-8">
        <svg class="w-10 h-10 text-ink" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>
      </div>
      <h1 class="text-5xl font-serif font-bold text-ink mb-4 italic">Review Complete</h1>
      <p class="text-xl text-ink/60 font-light mb-12">"Repetition is the mother of learning."</p>
      
      <div v-if="reviewQueue.length > 0" class="grid grid-cols-3 gap-6 mb-16 px-4">
        <div v-for="s in [
          { label: 'Correct', value: stats.correct },
          { label: 'Wrong', value: stats.wrong },
          { label: 'Unknown', value: stats.unknown }
        ]" :key="s.label" class="paper-card p-6">
          <div class="text-4xl font-serif font-black text-ink mb-2">{{ s.value }}</div>
          <div class="text-[10px] font-bold text-ink/40 uppercase tracking-widest">{{ s.label }}</div>
        </div>
      </div>
      
      <div v-if="reviewQueue.length === 0" class="text-lg text-ink/40 mb-12 font-serif italic">No review tasks for today. Enjoy your day!</div>
      
      <div class="flex justify-center gap-6">
        <router-link to="/" class="px-8 py-4 border border-[#E5E5E0] text-ink font-bold uppercase tracking-widest text-sm hover:border-ink transition-colors">
          Dashboard
        </router-link>
        <button @click="restart" class="px-8 py-4 bg-ink text-white font-bold uppercase tracking-widest text-sm shadow-lg hover:bg-ink/90 transition-colors">
          {{ reviewQueue.length === 0 ? 'Check for Updates' : 'Review Again' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Scoped styles minimal, relying on utility classes and global styles */
</style>

