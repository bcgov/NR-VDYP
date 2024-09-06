<template>
  <div class="date-range-container">
    <div class="start-date-range-item">
      <span class="start-date-range-label search-label">Date From</span>
      <Datepicker
        v-model="localStartDate"
        placeholder="Start Date"
        :enable-time-picker="false"
        class="start-date-range-picker"
      />
    </div>
    <div class="end-date-range-item">
      <span class="end-date-range-label search-label">Date To</span>
      <Datepicker
        v-model="localEndDate"
        placeholder="End Date"
        :enable-time-picker="false"
        class="end-date-range-picker"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, defineProps, defineEmits } from 'vue'
import Datepicker from '@vuepic/vue-datepicker'
import '@vuepic/vue-datepicker/dist/main.css'

const props = defineProps({
  startDate: Date,
  endDate: Date,
})

const emit = defineEmits(['update:startDate', 'update:endDate'])

const localStartDate = ref(props.startDate)
const localEndDate = ref(props.endDate)

watch(localStartDate, (newValue) => {
  emit('update:startDate', newValue)
})

watch(localEndDate, (newValue) => {
  emit('update:endDate', newValue)
})
</script>

<style scoped lang="scss">
/* start date whole box */
div.dp__main.dp__theme_light.start-date-range-picker {
  margin-top: 0px;
}

/* end date whole box */
div.dp__main.dp__theme_light.end-date-range-picker {
  margin-top: 0px;
}

.date-range-container {
  display: flex;
  gap: 15px;

  .start-date-range-item {
    display: flex;
    flex-direction: column;

    .start-date-range-label {
      font-size: 14px;
    }

    .start-date-range-picker {
      padding: 0px;
      margin: 0px;
    }
  }

  .end-date-range-item {
    display: flex;
    flex-direction: column;

    .end-date-range-label {
      font-size: 14px;
    }

    .end-date-range-picker {
      padding: 0px;
      margin: 0px;
    }
  }
}

/* input box border */
:deep(.dp__input) {
  border: 1px solid #a3a3a3;
  height: 40px;
}
</style>
