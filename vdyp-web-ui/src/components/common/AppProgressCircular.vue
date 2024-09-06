<template>
  <div
    class="centered-progress progress-wrapper"
    v-show="progressCircularStore.isShow"
  >
    <v-progress-circular
      indeterminate
      :size="70"
      :width="5"
      color="primary"
    ></v-progress-circular>
    <div v-show="progressCircularStore.showMessage" class="message">
      {{ progressCircularStore.message }}
    </div>
  </div>
</template>
<script setup lang="ts">
import { useProgressCircularStore } from '@/stores/common/progressCircularStore'

const progressCircularStore = useProgressCircularStore()
</script>
<style scoped>
.centered-progress {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 9999;
}
.progress-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}
.message {
  font-weight: bold;
  color: rgba(0, 0, 0, 0.87);
  margin-top: 10px;
}
</style>
