<template>
  <div class="learning-customizer bg-white rounded-3xl p-8 shadow-xl border border-slate-100 max-w-lg mx-auto">
    <div class="text-center mb-8">
      <h2 class="text-2xl font-black text-slate-800 mb-2">定制你的学习旅程</h2>
      <p class="text-slate-500">AI 将为你量身打造专属内容</p>
    </div>

    <div class="space-y-8">
      <!-- 主题选择 -->
      <div class="space-y-3">
        <label class="text-sm font-bold text-slate-700 uppercase tracking-wide">文章主题</label>
        <div class="grid grid-cols-2 gap-3">
          <button
            v-for="theme in themes"
            :key="theme.value"
            @click="settings.theme = theme.value"
            :class="[
              'px-4 py-3 rounded-xl border text-sm font-bold transition-all duration-200 text-left flex items-center gap-2',
              settings.theme === theme.value
                ? 'border-blue-500 bg-blue-50 text-blue-600 shadow-sm ring-1 ring-blue-500'
                : 'border-slate-200 text-slate-600 hover:border-blue-200 hover:bg-slate-50'
            ]"
          >
            <span>{{ theme.icon }}</span>
            {{ theme.label }}
          </button>
        </div>
      </div>

      <!-- 难度选择 -->
      <div class="space-y-3">
        <label class="text-sm font-bold text-slate-700 uppercase tracking-wide">难度等级</label>
        <div class="flex p-1 bg-slate-100 rounded-xl">
          <button
            v-for="diff in difficulties"
            :key="diff.value"
            @click="settings.difficulty = diff.value"
            :class="[
              'flex-1 py-2 rounded-lg text-sm font-bold transition-all duration-200',
              settings.difficulty === diff.value
                ? 'bg-white text-slate-800 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            {{ diff.label }}
          </button>
        </div>
      </div>
      
       <!-- 文章长度选择 -->
      <div class="space-y-3">
        <label class="text-sm font-bold text-slate-700 uppercase tracking-wide">文章长度</label>
        <div class="flex p-1 bg-slate-100 rounded-xl">
          <button
            v-for="len in lengths"
            :key="len.value"
            @click="settings.length = len.value"
            :class="[
              'flex-1 py-2 rounded-lg text-sm font-bold transition-all duration-200',
              settings.length === len.value
                ? 'bg-white text-slate-800 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            {{ len.label }}
          </button>
        </div>
      </div>

      <!-- 确认按钮 -->
      <button
        @click="confirmSettings"
        class="w-full py-4 bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white font-bold rounded-2xl shadow-lg shadow-blue-200 transition-all duration-200 transform hover:-translate-y-0.5"
      >
        开始生成
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

const emit = defineEmits<{
  confirm: [settings: LearningSettings]
}>()

export interface LearningSettings {
  theme: string
  difficulty: string
  length: string
}

const settings = reactive<LearningSettings>({
  theme: 'Daily Life',
  difficulty: 'medium',
  length: 'short'
})

const themes = [
  { label: '日常生活', value: 'Daily Life', icon: '🏠' },
  { label: '科幻', value: 'Science Fiction', icon: '🚀' },
  { label: '科技创新', value: 'Technology & Innovation', icon: '💻' },
  { label: '自然环境', value: 'Nature & Environment', icon: '🌿' },
  { label: '文化旅行', value: 'Culture & Travel', icon: '🌍' },
  { label: '商业经济', value: 'Business & Economy', icon: '💼' }
]

const difficulties = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' }
]

const lengths = [
  { label: '短', value: 'short' },
  { label: '中', value: 'medium' },
  { label: '长', value: 'long' }
]

const confirmSettings = () => {
  emit('confirm', { ...settings })
}
</script>
