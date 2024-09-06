<template>
  <ModelStep1 v-if="currentStep === 1" />
  <ModelStep2 v-if="currentStep === 2" />
  <ModelStep3 v-if="currentStep === 3" />

  <v-row justify="end" class="mt-n5 mb-n1 ma-2" no-gutters>
    <v-btn class="ma-2" @click="setAsDefault">Set as Default</v-btn>
    <v-btn class="ma-2" @click="cancel">Cancel</v-btn>
    <v-btn
      class="ma-2"
      @click="previousStep"
      :disabled="stepStore.currentStep === 1"
    >
      <v-icon left>mdi-chevron-left</v-icon> Previous
    </v-btn>
    <div class="ma-2" style="line-height: 36px">
      {{ stepStore.currentStep }} of 5
    </div>
    <v-btn
      class="ma-2"
      @click="nextStep"
      :disabled="stepStore.currentStep === 5"
    >
      Next <v-icon right>mdi-chevron-right</v-icon>
    </v-btn>
    <v-btn class="ma-2" color="primary" @click="runModel">Run Model</v-btn>
  </v-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useStepStore } from '@/stores/stepStore'

const stepStore = useStepStore()
const currentStep = computed(() => stepStore.currentStep)

import ModelStep1 from '@/components/model-param-selection-steps/ModelStep1.vue'
import ModelStep2 from '@/components/model-param-selection-steps/ModelStep2.vue'
import ModelStep3 from '@/components/model-param-selection-steps/ModelStep3.vue'

const nextStep = () => {
  stepStore.nextStep()
}

const previousStep = () => {
  stepStore.previousStep()
}

const setAsDefault = () => {}

const cancel = () => {}

const runModel = () => {}
</script>

<style scoped>
.v-row > div {
  line-height: 36px;
  font-size: 16px;
}
</style>
