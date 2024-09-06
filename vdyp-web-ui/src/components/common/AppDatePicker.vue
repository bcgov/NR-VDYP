<template>
  <span>
    <Datepicker v-model="localSelectedDate" :placeholder="placeholder" :enable-time-picker="enableTimePicker" />
  </span>
</template>

<script setup>
import { ref, watch, defineProps, defineEmits } from 'vue'
import Datepicker from '@vuepic/vue-datepicker'
import '@vuepic/vue-datepicker/dist/main.css'

const props = defineProps({
  selectedDate: { type: Date, required: false, default: null },
  placeholder: { type: String, required: false, default: '' },
  enableTimePicker: { type: Boolean, required: false, default: false },
})

const emit = defineEmits(['update:selectedDate'])

const localSelectedDate = ref(props.selectedDate)

watch(localSelectedDate, (newValue) => {
  emit('update:selectedDate', newValue)
})
</script>

<style scoped lang="scss">
:deep(.dp__input) {
  border: 1px solid #a3a3a3;
  height: 40px;
}
</style>
