<template>
  <v-dialog
    v-model="dialog"
    :max-width="options.width"
    :style="{ zIndex: options.zIndex }"
    @keydown.esc="cancel"
  >
    <v-card>
      <v-toolbar dense flat>
        <v-toolbar-title
          :style="options.titleStyle"
          class="text-body-2 font-weight-bold grey--text"
          >{{ title }}</v-toolbar-title
        >
      </v-toolbar>
      <!-- eslint-disable vue/no-v-text-v-html-on-component -->
      <v-card-text
        v-show="!!message"
        class="pa-4 black--text"
        style="font-size: 14px"
        v-html="message"
      ></v-card-text>
      <v-card-actions class="pt-3">
        <v-spacer></v-spacer>
        <v-btn
          v-if="!options.noconfirm"
          color="grey"
          class="body-2 font-weight-bold"
          style="letter-spacing: 0.25px"
          @click="cancel"
          >Cancel</v-btn
        >
        <v-btn
          color="primary"
          variant="outlined"
          class="body-2 font-weight-bold"
          style="letter-spacing: 0.25px"
          @click="agree"
          >OK</v-btn
        >
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useConfirmDialogStore } from '@/stores/common/confirmDialogStore'

const confirmDialogStore = useConfirmDialogStore()

const dialog = computed(() => confirmDialogStore.isOpen)
const title = computed(() => confirmDialogStore.dialogTitle)
const message = computed(() => confirmDialogStore.dialogMessage)
const options = computed(() => confirmDialogStore.dialogOptions)

const agree = () => {
  confirmDialogStore.agree()
}

const cancel = () => {
  confirmDialogStore.cancel()
}
</script>

<style scoped>
/* confirm dialog header whole box */
header.v-toolbar.v-toolbar--flat.v-toolbar--density-default.v-theme--defaultTheme.v-locale--is-ltr {
  height: 48px !important;
}
</style>
