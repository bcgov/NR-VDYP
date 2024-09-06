<template>
  <v-container fluid class="mt-n3">
    <fieldset class="fieldset full-height">
      <legend class="legend">Site Information</legend>

      <div class="species-row mt-n1">
        <v-row>
          <v-col cols="6">
            <v-select
              label="BEC Zone"
              :items="becZones"
              v-model="selectedBecZone"
            ></v-select>
          </v-col>
          <v-col cols="6">
            <v-select
              label="Eco Zone"
              :items="ecoZones"
              v-model="selectedEcoZone"
            ></v-select>
          </v-col>
        </v-row>
      </div>

      <div class="species-row mt-n2">
        <v-row class="mt-n7">
          <v-col cols="6 pl-0">
            <v-checkbox
              label="Include Secondary Dominant Height in Yield Table"
              v-model="includeSecondaryHeight"
            ></v-checkbox>
          </v-col>
          <v-col cols="6">
            <span
              ><label class="readonly__label">Species Group</label>
              <p class="mt-3 readonly__text">AC</p></span
            >
          </v-col>
        </v-row>
      </div>

      <div class="species-row mt-n2">
        <v-row class="mt-n7">
          <v-col cols="6">
            <v-select
              label="Site Species"
              :items="siteSpecies"
              v-model="selectedSpecies"
            ></v-select>
          </v-col>
          <v-col cols="6">
            <v-text-field
              label="Site Index Curve"
              v-model="siteIndexCurve"
              readonly
              append-inner-icon="mdi-information-outline"
            ></v-text-field>
            <div class="site-index-info">
              *Ministry Default Curve for this Species
            </div>
          </v-col>
        </v-row>
      </div>

      <div class="species-row mt-n2">
        <v-row class="mt-n7" style="display: inline-flex; align-items: center">
          <v-col cols="auto" style="margin-bottom: 20px">
            <span>Site Species Values:</span>
          </v-col>
          <v-col cols="auto">
            <v-radio-group v-model="siteSpeciesValues" inline>
              <v-radio label="Computed" value="computed"></v-radio>
              <v-radio label="Supplied" value="supplied"></v-radio>
            </v-radio-group>
          </v-col>
        </v-row>
      </div>

      <div class="species-row mt-n2">
        <v-row class="mt-n7">
          <v-col cols="3">
            <v-select
              label="Age Type"
              :items="ageTypes"
              v-model="selectedAgeType"
            ></v-select>
          </v-col>
          <v-col cols="3">
            <v-text-field
              label="Age (years)"
              type="number"
              v-model="ageYears"
            ></v-text-field>
          </v-col>
          <v-col cols="3">
            <v-radio-group v-model="floatOptions" row>
              <v-radio label="Float" value="float"></v-radio>
            </v-radio-group>
          </v-col>
        </v-row>
      </div>

      <div class="species-row mt-n2">
        <v-row class="mt-n7">
          <v-col cols="3"> &nbsp; </v-col>
          <v-col cols="3">
            <v-text-field
              label="Height (meters)"
              type="number"
              v-model="heightMeters"
            ></v-text-field>
          </v-col>
          <v-col cols="3">
            <v-radio-group v-model="floatOptions" row>
              <v-radio label="Float" value="float"></v-radio>
            </v-radio-group>
          </v-col>
        </v-row>
      </div>

      <div class="species-row mt-n2">
        <v-row class="mt-n7">
          <v-col cols="3"> &nbsp; </v-col>
          <v-col cols="3">
            <v-text-field
              label="BHA 50 Site Index"
              type="number"
              v-model="siteIndex"
            ></v-text-field>
          </v-col>
          <v-col cols="3">
            <v-radio-group v-model="floatOptions" row>
              <v-radio label="Float" value="float"></v-radio>
            </v-radio-group>
          </v-col>
        </v-row>
      </div>
    </fieldset>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const becZones = ref(['IDF - Interior Douglas Fir', 'xxx'])
const ecoZones = ref(['Eco Zone 1', 'Eco Zone 2', 'Eco Zone 3'])
const selectedBecZone = ref('IDF - Interior Douglas Fir')
const selectedEcoZone = ref('')

const includeSecondaryHeight = ref(false)

const siteSpecies = ref(['PL', 'AC', 'H', 'S'])
const selectedSpecies = ref('')

const speciesGroups = ref(['Group 1', 'Group 2', 'Group 3'])
const selectedSpeciesGroup = ref('')

const siteIndexCurve = ref('Thrower (1994)')

const siteSpeciesValues = ref('computed')

const ageTypes = ref(['Total', 'Partial'])
const selectedAgeType = ref('Total')

const ageYears = ref(60)
const heightMeters = ref(17)
const siteIndex = ref(30.0)

const floatOptions = ref('float')
</script>

<style scoped>
.site-index-info {
  margin-top: -12px;
  margin-left: 16px;
  font-size: 0.875rem;
  color: #666;
}
.full-height {
  height: calc(100vh - 410px);
  display: flex;
  flex-direction: column;
}
</style>
