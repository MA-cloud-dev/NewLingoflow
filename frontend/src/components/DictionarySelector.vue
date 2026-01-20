<template>
  <div class="bg-white/80 backdrop-blur-xl rounded-2xl border border-slate-100 shadow-sm p-4">
    <div class="flex items-center justify-between gap-6 flex-wrap">
      <!-- 词库选择 -->
      <div class="flex items-center gap-3">
        <span class="text-xs font-bold text-slate-400 uppercase tracking-wide">词库</span>
        <el-select
          v-model="selectedDictionaryId"
          placeholder="选择词库"
          @change="handleDictionaryChange"
          size="default"
          class="w-44"
        >
          <el-option
            v-for="dict in dictionaries"
            :key="dict.id"
            :label="dict.name"
            :value="dict.id"
          >
            <div class="flex justify-between items-center">
              <span class="font-bold text-slate-700">{{ dict.name }}</span>
              <span class="text-xs text-slate-400 ml-2">{{ dict.totalWords }}词</span>
            </div>
          </el-option>
        </el-select>
      </div>
      
      <!-- 进度指示器 -->
      <div v-if="progress" class="flex items-center gap-4 flex-1 min-w-[200px]">
        <div class="flex-1 max-w-xs">
          <div class="flex justify-between text-xs font-bold text-slate-500 mb-1.5">
            <span>学习进度</span>
            <span>{{ progress.progress }}%</span>
          </div>
          <el-progress 
            :percentage="progress.progress" 
            :color="progressColor" 
            :stroke-width="8" 
            :show-text="false"
            class="!m-0"
          />
        </div>
        
        <!-- 数字统计 -->
        <div class="flex items-center gap-4 text-center">
          <div>
            <div class="text-lg font-black text-slate-800">{{ progress.learnedWords }}</div>
            <div class="text-[10px] font-bold text-slate-400 uppercase">已学</div>
          </div>
          <div class="text-slate-200">/</div>
          <div>
            <div class="text-lg font-black text-slate-300">{{ progress.totalWords }}</div>
            <div class="text-[10px] font-bold text-slate-300 uppercase">总计</div>
          </div>
        </div>
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

<style scoped>
:deep(.el-select .el-input__wrapper) {
  border-radius: 10px;
  box-shadow: none !important;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

:deep(.el-select .el-input__wrapper:hover) {
  border-color: #93c5fd;
}

:deep(.el-select .el-input.is-focus .el-input__wrapper) {
  border-color: #3b82f6;
}
</style>
