<template>
  <div class="mb-8">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-xl font-bold text-slate-700">当前词库</h2>
      <el-select
        v-model="selectedDictionaryId"
        placeholder="切换词库"
        @change="handleDictionaryChange"
        size="large"
        class="w-48 !border-none !shadow-none"
      >
        <el-option
          v-for="dict in dictionaries"
          :key="dict.id"
          :label="dict.name"
          :value="dict.id"
        >
          <div class="flex justify-between items-center w-full min-w-[200px]">
            <span class="font-bold text-slate-700">{{ dict.name }}</span>
            <span class="text-xs text-slate-400 ml-4">{{ dict.description }}</span>
          </div>
        </el-option>
      </el-select>
    </div>
    
    <div v-if="progress" class="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm flex items-center gap-6">
      <div class="flex-1">
        <div class="flex justify-between text-sm font-bold text-slate-500 mb-2">
          <span>学习进度</span>
          <span>{{ progress.progress }}%</span>
        </div>
        <el-progress 
          :percentage="progress.progress" 
          :color="progressColor" 
          :stroke-width="10" 
          :show-text="false"
          class="!m-0"
        />
      </div>
      <div class="text-right border-l border-slate-100 pl-6">
        <div class="text-2xl font-black text-slate-800">{{ progress.learnedWords }}</div>
        <div class="text-xs font-bold text-slate-400 uppercase tracking-wider">Mastered</div>
      </div>
       <div class="text-right">
        <div class="text-2xl font-black text-slate-300">{{ progress.totalWords }}</div>
        <div class="text-xs font-bold text-slate-300 uppercase tracking-wider">Total</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getAllDictionaries, getDictionaryProgress } from '@/api/dictionary'
import type { Dictionary, DictionaryProgress } from '@/api/dictionary'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{
  change: [dictionaryId: number]
}>()

const dictionaries = ref<Dictionary[]>([])
const selectedDictionaryId = ref<number>()
const progress = ref<DictionaryProgress | null>(null)

const progressColor = computed(() => {
  if (!progress.value) return '#3b82f6'
  const p = progress.value.progress
  if (p < 30) return '#f87171' // red-400
  if (p < 70) return '#fbbf24' // amber-400
  return '#10b981' // emerald-500
})

const loadDictionaries = async () => {
  try {
    const response = await getAllDictionaries()
    if (response.code === 200 && response.data) {
      dictionaries.value = response.data
      if (dictionaries.value.length > 0) {
        selectedDictionaryId.value = dictionaries.value[0].id
        await loadProgress()
      }
    }
  } catch (error) {
    console.error('加载词典列表失败:', error)
    ElMessage.error('加载词典列表失败')
  }
}

const loadProgress = async () => {
  if (!selectedDictionaryId.value) return
  
  try {
    const response = await getDictionaryProgress(selectedDictionaryId.value)
    if (response.code === 200 && response.data) {
      progress.value = response.data
    }
  } catch (error) {
    console.error('加载进度失败:', error)
  }
}

const handleDictionaryChange = () => {
  if (selectedDictionaryId.value) {
    loadProgress()
    emit('change', selectedDictionaryId.value)
  }
}

onMounted(() => {
  loadDictionaries()
})
</script>
