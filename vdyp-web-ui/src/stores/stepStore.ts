import { defineStore } from 'pinia'

export const useStepStore = defineStore('stepStore', {
  state: () => ({
    currentStep: 1,
  }),
  actions: {
    nextStep() {
      if (this.currentStep < 5) this.currentStep++
    },
    previousStep() {
      if (this.currentStep > 1) this.currentStep--
    },
    setStep(step: number) {
      if (step >= 1 && step <= 5) this.currentStep = step
    },
  },
})
