<template>
  <v-container fluid class="mt-n3">
    <fieldset class="fieldset full-height">
      <legend class="legend">Species Information</legend>

      <div class="species-row mt-n5 mb-n5">
        <v-row style="display: inline-flex; align-items: center">
          <v-col cols="auto" style="margin-bottom: 20px">
            <span>Species% derived by:</span>
          </v-col>
          <v-col cols="auto">
            <v-radio-group v-model="derivedBy" inline>
              <v-radio label="Volume" value="volume"></v-radio>
              <v-radio label="Basal Area" value="basalArea"></v-radio>
            </v-radio-group>
          </v-col>
        </v-row>
      </div>

      <div class="species-row">
        <v-row>
          <v-col cols="3" sm="3" md="3">
            <v-select
              :items="speciesOptions"
              v-model="species1"
              item-title="label"
              item-value="value"
              label="Species #1"
            ></v-select>
          </v-col>
          <v-col cols="3" sm="3" md="3">
            <v-text-field
              type="number"
              v-model="speciesPercent1"
              label="Species #1 Percent"
            ></v-text-field>
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Species Group</label>
              <p class="mt-3 readonly__text">PL</p></span
            >
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Species Group Percent</label>
              <p class="mt-3 readonly__text">30.0</p></span
            >
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Site Species</label>
              <p class="mt-3 readonly__text">PL</p></span
            >
          </v-col>
        </v-row>
      </div>
      <div class="species-row mt-n5">
        <v-row>
          <v-col cols="3" sm="3" md="3">
            <v-select
              :items="speciesOptions"
              v-model="species2"
              item-title="label"
              item-value="value"
              label="Species #2"
            ></v-select>
          </v-col>
          <v-col cols="3" sm="3" md="3">
            <v-text-field
              type="number"
              v-model="speciesPercent2"
              label="Species #2 Percent"
            ></v-text-field>
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Species Group</label>
              <p class="mt-3 readonly__text">AC</p></span
            >
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Species Group Percent</label>
              <p class="mt-3 readonly__text">30.0</p></span
            >
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Site Species</label>
              <p class="mt-3 readonly__text">AC</p></span
            >
          </v-col>
        </v-row>
      </div>
      <div class="species-row mt-n5">
        <v-row>
          <v-col cols="3" sm="3" md="3">
            <v-select
              :items="speciesOptions"
              v-model="species3"
              item-title="label"
              item-value="value"
              label="Species #3"
            ></v-select>
          </v-col>
          <v-col cols="3" sm="3" md="3">
            <v-text-field
              type="number"
              v-model="speciesPercent3"
              label="Species #3 Percent"
            ></v-text-field>
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Species Group</label>
              <p class="mt-3 readonly__text">H</p></span
            >
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Species Group Percent</label>
              <p class="mt-3 readonly__text">30.0</p></span
            >
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Site Species</label>
              <p class="mt-3 readonly__text">H</p></span
            >
          </v-col>
        </v-row>
      </div>
      <div class="species-row mt-n5">
        <v-row>
          <v-col cols="3" sm="3" md="3">
            <v-select
              :items="speciesOptions"
              v-model="species4"
              item-title="label"
              item-value="value"
              label="Species #4"
            ></v-select>
          </v-col>
          <v-col cols="3" sm="3" md="3">
            <v-text-field
              type="number"
              v-model="speciesPercent4"
              label="Species #4 Percent"
            ></v-text-field>
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Species Group</label>
              <p class="mt-3 readonly__text">S</p></span
            >
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Species Group Percent</label>
              <p class="mt-3 readonly__text">10.0</p></span
            >
          </v-col>
          <v-col cols="2" sm="2" md="2">
            <span
              ><label class="readonly__label">Site Species</label>
              <p class="mt-3 readonly__text">S</p></span
            >
          </v-col>
        </v-row>
      </div>
      <div class="species-row mt-n5">
        <v-row>
          <v-col cols="3" sm="3" md="3">
            <v-select
              :items="speciesOptions"
              item-title="label"
              item-value="value"
              label="Species #5"
            ></v-select>
          </v-col>
          <v-col cols="3" sm="3" md="3">
            <v-text-field
              type="number"
              label="Species #5 Percent"
            ></v-text-field>
          </v-col>
        </v-row>
      </div>
      <div class="species-row mt-n5">
        <v-row>
          <v-col cols="3" sm="3" md="3">
            <v-select
              :items="speciesOptions"
              item-title="label"
              item-value="value"
              label="Species #6"
            ></v-select>
          </v-col>
          <v-col cols="3" sm="3" md="3">
            <v-text-field
              type="number"
              label="Species #6 Percent"
            ></v-text-field>
          </v-col>
        </v-row>
      </div>
      <div class="species-row mt-n5 mb-n2">
        <v-row>
          <v-col cols="3" sm="3" md="3"> &nbsp; </v-col>
          <v-col cols="3" sm="3" md="3">
            <span
              ><label class="readonly__label">Total Species Percent</label>
              <p class="mt-3 readonly__text">{{ totalSpeciesPercent }}</p></span
            >
          </v-col>
        </v-row>
      </div>
    </fieldset>
  </v-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSpeciesStore } from '@/stores/speciesStore'
import { storeToRefs } from 'pinia'

const derivedBy = ref('volume')

const speciesStore = useSpeciesStore()
const { speciesList, speciesOptions, speciesGroups, totalSpeciesPercent } =
  storeToRefs(speciesStore)
const { addMoreSpecies } = speciesStore

const species1 = ref('PL')
const species2 = ref('AC')
const species3 = ref('H')
const species4 = ref('S')

const speciesPercent1 = ref(30.0)
const speciesPercent2 = ref(30.0)
const speciesPercent3 = ref(30.0)
const speciesPercent4 = ref(10.0)
</script>

<style scoped>
.full-height {
  height: calc(100vh - 410px);
  display: flex;
  flex-direction: column;
}
</style>
