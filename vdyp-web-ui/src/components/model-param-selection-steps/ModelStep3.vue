<template>
  <v-container fluid class="mt-n3">
    <fieldset class="fieldset full-height">
      <legend class="legend">Stand Density</legend>

      <div class="species-row mt-n1">
        <v-row>
          <v-col cols="6">
            <v-text-field
              label="Percent Stockable Area"
              type="number"
              v-model="percentStockableArea"
            ></v-text-field>
          </v-col>
          <v-col cols="6">
            <v-text-field type="number" v-model="basalArea">
              <template v-slot:label> Basal Area (m<sup>2</sup>/ha) </template>
            </v-text-field>
          </v-col>
        </v-row>
      </div>
      <div class="species-row mt-n5">
        <v-row>
          <v-col cols="6">
            <v-text-field
              label="Trees per Hectare"
              type="number"
              v-model="treesPerHectare"
            ></v-text-field>
          </v-col>
          <v-col cols="6">
            <v-select
              label="Minimum DBH Limit"
              :items="minimumDBHLimits"
              v-model="selectedMinimumDBHLimit"
            ></v-select>
          </v-col>
        </v-row>
      </div>
      <div class="species-row mt-n5">
        <v-row>
          <v-col cols="6">
            <v-text-field
              label="Percent Crown Closure"
              type="number"
              v-model="percentCrownClosure"
            ></v-text-field>
          </v-col>
        </v-row>
      </div>
    </fieldset>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const minimumDBHLimits = ref(['Eco Zone 1', 'Eco Zone 2', 'Eco Zone 3'])
const percentStockableArea = ref(55)
const basalArea = ref()
const treesPerHectare = ref()
const selectedMinimumDBHLimit = ref('7.5 cm+')
const percentCrownClosure = ref(50)
</script>

<style scoped>
.full-height {
  height: calc(100vh - 410px);
  display: flex;
  flex-direction: column;
}
</style>
