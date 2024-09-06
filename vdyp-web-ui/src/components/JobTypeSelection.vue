<template>
  <v-container fluid class="no-padding">
    <v-row>
      <v-col cols="4 no-padding-bottom" sm="4" md="4">
        <v-select
          v-model="modelType"
          :items="modelTypes"
          label="Select Model Type"
        ></v-select>
      </v-col>
      <v-col cols="4 no-padding-bottom" sm="4" md="4">
        <v-select
          v-model="modelName"
          :items="modelNames"
          label="Select Model Name"
        ></v-select>
      </v-col>
      <v-col cols="4 no-padding-bottom" sm="4" md="4">
        <v-text-field v-model="jobName" label="Job Name"></v-text-field>
      </v-col>
    </v-row>
  </v-container>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useJobTypeSelectionStore } from '@/stores/jobTypeSelectionStore'

const jobTypeSelectionStore = useJobTypeSelectionStore()
const modelType = ref(jobTypeSelectionStore.modelType)
const modelName = ref(jobTypeSelectionStore.modelName)
const jobName = ref(jobTypeSelectionStore.jobName)

const modelTypes = ['File Upload', 'Input Model Parameters']
const modelNames = ['VDYP 8', 'VDYP 9']
</script>

<style scoped>
.no-padding {
  padding: 0px;
}
.no-padding-bottom {
  padding-bottom: 0px;
}
</style>
