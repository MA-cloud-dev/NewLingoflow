<template>
  <div class="article-reader space-y-8 animate-in fade-in duration-700">
    <div class="bg-white p-12 md:p-16 rounded-[3rem] border border-slate-100 shadow-xl shadow-slate-200/50 relative overflow-hidden">
      <!-- 背景装饰 -->
      <div class="absolute top-0 right-0 w-64 h-64 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-bl-[10rem] opacity-50 pointer-events-none"></div>
      
      <!-- 标题 -->
      <h2 class="relative text-3xl md:text-4xl font-black text-slate-900 mb-10 pb-8 border-b border-slate-50 tracking-tight leading-tight">
        {{ title }}
      </h2>
      
      <!-- 文章内容 -->
      <div class="relative text-lg md:text-xl text-slate-700 leading-loose font-serif text-justify">
        <template v-for="(seg, idx) in segments" :key="idx">
          <el-popover
            v-if="seg.isHighlight"
            placement="top"
            :width="200"
            trigger="hover"
            popper-class="word-popover"
          >
            <template #reference>
              <span 
                class="highlight-word px-1.5 rounded-lg text-blue-700 font-bold bg-blue-50/80 decoration-blue-200 decoration-2 underline-offset-4 underline cursor-pointer hover:bg-blue-100 transition-colors"
                @mouseenter="handleWordHover"
              >
                {{ seg.text }}
              </span>
            </template>
            <div class="text-center space-y-2">
              <div class="font-bold text-slate-800 text-lg">{{ seg.text }}</div>
              <button 
                @click="speak(seg.text)"
                class="inline-flex items-center gap-1 text-xs font-bold text-blue-500 hover:text-blue-600 uppercase tracking-widest"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/>
                </svg>
                发音
              </button>
            </div>
          </el-popover>
          <span v-else>{{ seg.text }}</span>
        </template>
      </div>

      <!-- 工具栏 -->
      <div class="mt-12 flex justify-between items-center border-t border-slate-50 pt-8">
        <button 
          @click="togglePlayArticle"
          class="flex items-center gap-3 px-5 py-2.5 rounded-full bg-slate-50 text-slate-600 font-bold text-sm hover:bg-blue-50 hover:text-blue-600 transition-all"
        >
          <span v-if="isPlaying">
            <svg class="w-5 h-5 block animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 9v6m4-6v6m7-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          </span>
          <span v-else>
            <svg class="w-5 h-5 block" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          </span>
          {{ isPlaying ? '停止朗读' : '朗读全文' }}
        </button>
        
        <div class="text-xs font-bold text-slate-300 uppercase tracking-widest">
          AI 生成内容
        </div>
      </div>
    </div>
    
    <div class="flex justify-center">
      <button 
        class="group py-5 pr-10 pl-12 rounded-3xl bg-[#0F172A] text-white font-bold text-xl shadow-xl hover:bg-slate-900 hover:-translate-y-1 transition-all flex items-center gap-4"
        @click="$emit('complete')"
      >
        开始实战测试
        <svg class="w-6 h-6 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M17 8l4 4m0 0l-4 4m4-4H3"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onUnmounted } from 'vue'

const props = defineProps<{
  title: string
  content: string
}>()

defineEmits<{
  complete: []
}>()

const isPlaying = ref(false)
const utterance = ref<SpeechSynthesisUtterance | null>(null)

const segments = computed(() => {
  if (!props.content) return []
  // Split by markdown bold syntax **word**
  const parts = props.content.split(/(\*\*.*?\*\*)/g)
  return parts.map(part => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return {
        text: part.slice(2, -2),
        isHighlight: true
      }
    }
    return { text: part, isHighlight: false }
  })
})

const speak = (text: string) => {
  // Stop any current speech
  window.speechSynthesis.cancel()
  
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'en-US'
  u.rate = 0.9
  window.speechSynthesis.speak(u)
}

const handleWordHover = () => {
  // Optional: Auto pronounce on hover? Maybe too annoying.
  // Just pre-load could be good.
}

const togglePlayArticle = () => {
  if (isPlaying.value) {
    window.speechSynthesis.cancel()
    isPlaying.value = false
    return
  }
  
  // Create plain text content for reading (remove markdown)
  const plainText = props.content.replace(/\*\*/g, '')
  
  utterance.value = new SpeechSynthesisUtterance(props.title + ". " + plainText)
  utterance.value.lang = 'en-US'
  utterance.value.rate = 0.9
  
  utterance.value.onend = () => {
    isPlaying.value = false
  }
  
  window.speechSynthesis.speak(utterance.value)
  isPlaying.value = true
}

onUnmounted(() => {
  window.speechSynthesis.cancel()
})
</script>

<style>
.word-popover {
  border-radius: 16px !important;
  padding: 16px !important;
}
</style>
