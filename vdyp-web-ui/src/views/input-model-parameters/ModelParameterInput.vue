<template>
  <v-container fluid>
    <v-card class="pa-4" style="padding-bottom: 8px !important">
      <JobTypeSelection />
    </v-card>
    <v-spacer class="space"></v-spacer>
    <v-card class="pa-4">
      <v-card elevation="4">
        <v-tabs v-model="currentTab">
          <v-tab v-for="(tab, index) in tabs" :key="index">{{
            tab.label
          }}</v-tab>
        </v-tabs>
        <v-card-text>
          <v-tabs-window v-model="currentTab">
            <v-tabs-window-item
              v-for="(tab, index) in tabs"
              :key="index"
              :value="index"
            >
              <component :is="tab.component"></component>
            </v-tabs-window-item>
          </v-tabs-window>
        </v-card-text>
      </v-card>
    </v-card>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import JobTypeSelection from '@/components/JobTypeSelection.vue'

import ModelParameterSelection from '@/views/input-model-parameters/ModelParameterSelection.vue'
import Results from '@/views/input-model-parameters/SpeciesResults.vue'
import ViewLogFile from '@/views/input-model-parameters/ViewLogFile.vue'
import ViewErrorMessages from '@/views/input-model-parameters/ViewErrorMessages.vue'

const currentTab = ref(0)

const tabs = [
  { label: 'Model Parameter Selection', component: ModelParameterSelection },
  { label: 'Results', component: Results },
  { label: 'View Log File', component: ViewLogFile },
  { label: 'View Error Messages', component: ViewErrorMessages },
]
</script>

<style lang="scss" scoped>
.space {
  margin-top: 10px;
}
</style>
