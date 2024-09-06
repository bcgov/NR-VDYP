import { defineStore } from 'pinia'

export const useSpeciesStore = defineStore('speciesStore', {
  state: () => ({
    speciesList: [
      { name: 'PL - Lodgepole', percent: 30, group: 'S', groupPercent: 30, siteSpecies: 'PL' },
      { name: 'AC - Popular', percent: 30, group: 'AC', groupPercent: 30, siteSpecies: 'AC' },
      { name: 'H - Hemlock', percent: 30, group: 'H', groupPercent: 30, siteSpecies: 'H' },
      { name: 'S - Spruce', percent: 10, group: 'S', groupPercent: 10, siteSpecies: 'S' },
      { name: 'S - Spruce', percent: 0, group: 'S', groupPercent: 10, siteSpecies: 'S' },
      { name: 'S - Spruce', percent: 0, group: 'S', groupPercent: 10, siteSpecies: 'S' },
    ],
    speciesOptions: [
      { value: 'PL - Lodgepole', label: 'PL - Lodgepole' },
      { value: 'AC - Popular', label: 'AC - Popular' },
      { value: 'H - Hemlock', label: 'H - Hemlock' },
      { value: 'S - Spruce', label: 'S - Spruce' },
    ],
    speciesGroups: ['S', 'AC', 'H'],
  }),
  getters: {
    totalSpeciesPercent: (state) => state.speciesList.reduce((sum, species) => sum + species.percent, 0),
  },
  actions: {
    addMoreSpecies() {
      this.speciesList.push({ name: '', percent: 0, group: '', groupPercent: 0, siteSpecies: '' })
    },
  },
})
