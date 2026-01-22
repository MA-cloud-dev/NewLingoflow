<template>
  <div class="comprehension-quiz max-w-2xl mx-auto space-y-8 animate-in slide-in-from-right duration-500">
    <!-- Header -->
    <div class="flex items-center justify-between px-2">
      <div>
        <h2 class="text-3xl font-black text-slate-800 tracking-tight">阅读理解</h2>
        <p class="text-slate-500 font-medium">检验你的文章理解程度</p>
      </div>
      <div class="px-5 py-2 rounded-full bg-slate-100 text-slate-500 font-bold text-xs tracking-widest">
        {{ currentIndex + 1 }} / {{ questions.length }}
      </div>
    </div>

    <!-- Question Card -->
    <div class="bg-white p-10 rounded-[2.5rem] border border-slate-100 shadow-sm relative overflow-hidden">
      
    <!-- Question Progress Bar / Navigator -->
      <div class="mb-8">
        <div class="flex items-center gap-2 mb-2">
          <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">题目进度</span>
        </div>
        <div class="flex items-center gap-2">
          <button
            v-for="(_, idx) in questions"
            :key="idx"
            @click="goToQuestion(idx)"
            class="flex-1 h-2.5 rounded-full transition-all duration-300 relative group cursor-pointer"
            :class="getProgressBarClass(idx)"
          >
            <!-- Tooltip showing question number -->
            <div class="absolute -top-8 left-1/2 -translate-x-1/2 px-2 py-1 bg-slate-800 text-white text-xs font-bold rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
              第 {{ idx + 1 }} 题
            </div>
          </button>
        </div>
        <!-- Question type indicator -->
        <div class="flex justify-between mt-2">
          <span 
            v-for="(q, idx) in questions" 
            :key="'type-' + idx"
            class="flex-1 text-center text-[10px] font-medium"
            :class="idx === currentIndex ? 'text-blue-600' : 'text-slate-300'"
          >
            {{ q.type === 'main_idea' ? '主旨' : '词汇' }}
          </span>
        </div>
      </div>

      <!-- Tag -->
      <div class="mb-6">
        <span 
          class="px-3 py-1 rounded-lg text-xs font-bold uppercase tracking-wider"
          :class="currentQuestion.type === 'main_idea' ? 'bg-purple-100 text-purple-600' : 'bg-blue-100 text-blue-600'"
        >
          {{ currentQuestion.type === 'main_idea' ? '主旨大意' : '词汇聚焦' }}
        </span>
      </div>

      <!-- Question Text -->
      <h3 class="text-xl font-bold text-slate-800 leading-relaxed mb-8">
        {{ currentQuestion.question }}
      </h3>

      <!-- Word Context (if applicable) -->
      <div v-if="currentQuestion.word" class="mb-8 p-4 bg-slate-50 rounded-xl border border-slate-100 text-sm text-slate-500">
        关键词: <span class="font-bold text-slate-700">{{ currentQuestion.word }}</span>
      </div>

      <!-- Options -->
      <div class="space-y-4">
        <button
          v-for="(option, idx) in currentQuestion.options"
          :key="idx"
          @click="selectOption(idx)"
          :disabled="answeredQuestions[currentIndex]"
          class="w-full p-5 rounded-2xl border-2 text-left transition-all duration-200 flex items-start gap-4 group"
          :class="getOptionClass(idx)"
        >
          <span class="w-6 h-6 rounded-full border-2 flex-shrink-0 flex items-center justify-center text-xs font-bold mt-0.5 transition-colors"
             :class="getOptionBadgeClass(idx)"
          >
            {{ String.fromCharCode(65 + idx) }}
          </span>
          <span class="text-base font-medium leading-relaxed">{{ option }}</span>
        </button>
      </div>
    </div>

    <!-- Feedback & Next -->
    <div v-if="hasAnswered" class="animate-in fade-in slide-in-from-bottom-4 duration-300">
      <div class="bg-white p-6 rounded-2xl border border-slate-100 shadow-lg mb-6">
         <div class="flex items-center gap-3 mb-2">
            <div :class="['w-8 h-8 rounded-full flex items-center justify-center font-bold text-white', isCorrect ? 'bg-emerald-500' : 'bg-red-500']">
              {{ isCorrect ? '✓' : '×' }}
            </div>
            <span class="font-black text-lg" :class="isCorrect ? 'text-emerald-600' : 'text-red-600'">
              {{ isCorrect ? '回答正确!' : '回答错误' }}
            </span>
         </div>
         <p class="text-slate-600 pl-11">{{ currentQuestion.explanation }}</p>
      </div>

      <button 
        @click="nextQuestion"
        class="w-full py-4 rounded-2xl bg-[#0F172A] text-white font-bold text-lg shadow-xl hover:bg-slate-900 transition-all flex items-center justify-center gap-2"
      >
        {{ isLastQuestion ? '完成测试' : '下一题' }}
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import type { ComprehensionQuestion } from '@/api/learning'

const props = defineProps<{
  questions: ComprehensionQuestion[]
}>()

const emit = defineEmits<{
  complete: []
}>()

const currentIndex = ref(0)
// 存储每个题目的选择状态
const selectedOptions = reactive<(number | null)[]>(
  Array(props.questions.length).fill(null)
)
// 存储每个题目是否已回答
const answeredQuestions = reactive<boolean[]>(
  Array(props.questions.length).fill(false)
)

const currentQuestion = computed(() => props.questions[currentIndex.value])
const isLastQuestion = computed(() => currentIndex.value === props.questions.length - 1)

// 当前题目的选中索引
const selectedOptionIndex = computed(() => selectedOptions[currentIndex.value])
// 当前题目是否已回答
const hasAnswered = computed(() => answeredQuestions[currentIndex.value])

// Helper to extract option index "A", "B", "C", "D" from answer string
const correctIndex = computed(() => {
  const ans = currentQuestion.value.correctAnswer.trim().toUpperCase()
  return ans.charCodeAt(0) - 65
})

const isCorrect = computed(() => selectedOptionIndex.value === correctIndex.value)

const selectOption = (idx: number) => {
  if (answeredQuestions[currentIndex.value]) return
  selectedOptions[currentIndex.value] = idx
  answeredQuestions[currentIndex.value] = true
}

// 切换到指定题目
const goToQuestion = (idx: number) => {
  if (idx >= 0 && idx < props.questions.length) {
    currentIndex.value = idx
  }
}

const nextQuestion = () => {
  if (isLastQuestion.value) {
    emit('complete')
  } else {
    currentIndex.value++
  }
}

// 进度条样式
const getProgressBarClass = (idx: number) => {
  if (idx === currentIndex.value) {
    // 当前题目 - 高亮
    return 'bg-blue-500 ring-2 ring-blue-300 ring-offset-1'
  }
  if (answeredQuestions[idx]) {
    // 已回答 - 绿色
    return 'bg-emerald-400 hover:bg-emerald-500'
  }
  // 未回答 - 灰色
  return 'bg-slate-200 hover:bg-slate-300'
}

const getOptionClass = (idx: number) => {
  if (!hasAnswered.value) {
    return 'border-slate-100 hover:border-blue-200 hover:bg-slate-50 text-slate-600'
  }
  
  if (idx === correctIndex.value) {
    return 'border-emerald-500 bg-emerald-50 text-emerald-800'
  }
  
  if (idx === selectedOptionIndex.value && idx !== correctIndex.value) {
    return 'border-red-500 bg-red-50 text-red-800'
  }
  
  return 'border-slate-100 opacity-50'
}

const getOptionBadgeClass = (idx: number) => {
   if (!hasAnswered.value) return 'border-slate-300 text-slate-300 group-hover:border-blue-300 group-hover:text-blue-300'
   
   if (idx === correctIndex.value) return 'border-emerald-600 bg-emerald-600 text-white'
   if (idx === selectedOptionIndex.value && idx !== correctIndex.value) return 'border-red-500 bg-red-500 text-white'
   
   return 'border-slate-200 text-slate-200'
}
</script>
