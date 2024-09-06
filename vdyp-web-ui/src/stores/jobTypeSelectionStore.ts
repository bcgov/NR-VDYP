import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useJobTypeSelectionStore = defineStore(
  'jobTypeSelectionStore',
  () => {
    const modelType = ref('Input Model Parameters')
    const modelName = ref('VDYP 8')
    const jobName = ref('Job_Name_DD/MM/YYYY')

    return { modelType, modelName, jobName }
  },
)
