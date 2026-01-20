<template>
  <div class="learning-settings-bar bg-white/80 backdrop-blur-xl rounded-2xl border border-slate-100 shadow-sm px-6 py-3">
    <div class="flex items-center gap-4 flex-wrap">
      <!-- 主题选择下拉框 -->
      <div class="settings-group flex items-center gap-2">
        <span class="text-xs font-bold text-slate-400 uppercase tracking-wide">Topic</span>
        <div class="relative">
          <select
            v-model="localSettings.theme"
            @change="emitChange"
            class="appearance-none bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5 pr-8 text-sm font-semibold text-slate-700 hover:border-blue-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 outline-none transition-all cursor-pointer"
          >
            <option v-for="theme in themes" :key="theme.value" :value="theme.value">
              {{ theme.icon }} {{ theme.label }}
            </option>
          </select>
          <svg class="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </div>

      <!-- 分隔线 -->
      <div class="h-6 w-px bg-slate-200"></div>

      <!-- 难度选择 -->
      <div class="settings-group flex items-center gap-2">
        <span class="text-xs font-bold text-slate-400 uppercase tracking-wide">Difficulty</span>
        <div class="flex bg-slate-100 rounded-lg p-0.5">
          <button
            v-for="diff in difficulties"
            :key="diff.value"
            @click="localSettings.difficulty = diff.value; emitChange()"
            :class="[
              'px-3 py-1 rounded-md text-xs font-bold transition-all duration-200',
              localSettings.difficulty === diff.value
                ? 'bg-white text-slate-800 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            {{ diff.label }}
          </button>
        </div>
      </div>

      <!-- 分隔线 -->
      <div class="h-6 w-px bg-slate-200"></div>

      <!-- 文章长度选择 -->
      <div class="settings-group flex items-center gap-2">
        <span class="text-xs font-bold text-slate-400 uppercase tracking-wide">Length</span>
        <div class="flex bg-slate-100 rounded-lg p-0.5">
          <button
            v-for="len in lengths"
            :key="len.value"
            @click="localSettings.length = len.value; emitChange()"
            :class="[
              'px-3 py-1 rounded-md text-xs font-bold transition-all duration-200',
              localSettings.length === len.value
                ? 'bg-white text-slate-800 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            {{ len.label }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

export interface LearningSettings {
  theme: string
  difficulty: string
  length: string
}

const props = defineProps<{
  modelValue: LearningSettings
}>()

const emit = defineEmits<{
  'update:modelValue': [settings: LearningSettings]
}>()

const localSettings = reactive<LearningSettings>({
  theme: props.modelValue.theme,
  difficulty: props.modelValue.difficulty,
  length: props.modelValue.length
})

// 监听 props 变化，同步到本地
watch(() => props.modelValue, (newVal) => {
  localSettings.theme = newVal.theme
  localSettings.difficulty = newVal.difficulty
  localSettings.length = newVal.length
}, { deep: true })

const themes = [
  { label: 'Daily Life', value: 'Daily Life', icon: '🏠' },
  { label: 'Sci-Fi', value: 'Science Fiction', icon: '🚀' },
  { label: 'Technology', value: 'Technology & Innovation', icon: '💻' },
  { label: 'Nature', value: 'Nature & Environment', icon: '🌿' },
  { label: 'Culture', value: 'Culture & Travel', icon: '🌍' },
  { label: 'Business', value: 'Business & Economy', icon: '💼' }
]

const difficulties = [
  { label: 'Easy', value: 'easy' },
  { label: 'Medium', value: 'medium' },
  { label: 'Hard', value: 'hard' }
]

const lengths = [
  { label: 'Short', value: 'short' },
  { label: 'Medium', value: 'medium' },
  { label: 'Long', value: 'long' }
]

const emitChange = () => {
  emit('update:modelValue', { ...localSettings })
}
</script>

<style scoped>
.settings-group {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .learning-settings-bar .flex {
    justify-content: center;
  }
  
  .h-6.w-px {
    display: none;
  }
}
</style>
