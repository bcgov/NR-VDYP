import { defineStore } from 'pinia'
import type ProgressCircularState from '@/interfaces/ProgressCircularState'

export const useProgressCircularStore = defineStore({
  id: 'progressCircularStore',
  state: (): ProgressCircularState => ({
    isShow: false,
    message: '',
  }),
  getters: {
    showMessage: (state) => !!state.message,
  },
  actions: {
    showProgress(message?: string) {
      this.isShow = true
      this.message = message
    },
    hideProgress() {
      this.isShow = false
    },
  },
})
