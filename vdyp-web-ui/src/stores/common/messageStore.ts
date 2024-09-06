import { defineStore } from 'pinia'
import type { MessageType } from '@/types/types'

interface MessageState {
  isShow: boolean
  message: string
  type: MessageType
}

export const useMessageStore = defineStore({
  id: 'messageStore',
  state: (): MessageState => ({
    isShow: false,
    message: '',
    type: '',
  }),
  getters: {},
  actions: {
    showMessage(message: string) {
      this.isShow = true
      this.message = message
      this.type = ''
    },

    showErrorMessage(message: string) {
      this.isShow = true
      this.message = message
      this.type = 'error'
    },
    showSuccessMessage(message: string) {
      this.isShow = true
      this.message = message
      this.type = 'success'
    },
    showInfoMessage(message: string) {
      this.isShow = true
      this.message = message
      this.type = 'info'
    },
    showWarningMessage(message: string) {
      this.isShow = true
      this.message = message
      this.type = 'warning'
    },
  },
})
