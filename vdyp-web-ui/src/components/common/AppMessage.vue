<template>
  <div>
    <v-snackbar
      v-model="messageStore.isShow"
      timeout="2000"
      :color="messageStore.type"
      class="elevation-12"
      location="top"
    >
      <div class="d-flex align-center">
        <v-icon class="mr-2">{{ getIcon(messageStore.type) }}</v-icon>
        <span> {{ messageStore.message }}</span>
      </div>

      <template v-slot:actions>
        <v-btn icon variant="text" @click="messageStore.isShow = false">
          <v-icon>mdi-close</v-icon>
        </v-btn>
      </template>
    </v-snackbar>
  </div>
</template>
<script setup lang="ts">
import { useMessageStore } from '@/stores/common/messageStore'
import type { MessageType } from '@/types/types'

const messageStore = useMessageStore()

const getIcon = (type: MessageType) => {
  const icon: { [key in MessageType]: string } = {
    '': '',
    info: 'mdi-information',
    success: 'mdi-check-circle',
    error: 'mdi-alert-circle',
    warning: 'mdi-alert',
  }

  return icon[type]
}
</script>
