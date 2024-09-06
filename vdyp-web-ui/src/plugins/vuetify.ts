import { createVuetify, type ThemeDefinition } from 'vuetify'
import '@mdi/font/css/materialdesignicons.css'
import 'vuetify/styles'

const defaultTheme: ThemeDefinition = {
  dark: false,
  colors: {
    primary: '#003366',
    secondary: '#fcba19',
    accent: '#606060',
    error: '#d8292f',
    warning: '#f9f1c6',
    info: '#d9eaf7',
    success: '#2e8540',
    anchor: '#1A5A96',
    background: '#ffffff',
  },
}

export default createVuetify({
  icons: {
    defaultSet: 'mdi',
  },
  theme: {
    defaultTheme: 'defaultTheme',
    themes: {
      defaultTheme,
    },
  },
  defaults: {
    VTabs: {
      color: 'white darken-1',
      sliderColor: '#fcc219',
      centerActive: true,
      showArrows: true,
      alignWithTitle: true,
      fixedTabs: false,
      bgColor: '#38598a',
    },
  },
})
