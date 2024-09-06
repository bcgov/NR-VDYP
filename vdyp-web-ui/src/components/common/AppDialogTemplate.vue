<template>
  <v-dialog v-model="localShow" max-width="600px">
    <v-card>
      <v-card-title class="popup-header">Dialog Title</v-card-title>
      <v-card-text>This is some dialog content.</v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="primary" @click="closeDialog">Save</v-btn>
        <v-btn color="primary" variant="outlined" @click="closeDialog">Close</v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import { ref, defineProps, watch, defineEmits } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    required: true,
  },
})

const localShow = ref(props.show)
const emit = defineEmits(['close'])

watch(
  () => props.show,
  (newVal) => {
    localShow.value = newVal
  },
)

const closeDialog = () => {
  localShow.value = false
  emit('close')
}

watch(localShow, (newVal) => {
  if (!newVal) {
    emit('close')
  }
})
</script>
